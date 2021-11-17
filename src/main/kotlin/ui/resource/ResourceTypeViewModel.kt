package ui.resource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import locales.LocaleIsoCode
import project.*
import utils.PropertyStore
import java.io.File
import java.util.*

sealed class ResourceTypeViewModel<R : Resource, M : Metadata<M>>(
    val project: Project,
    val type: ResourceType,
    private val defaultLocale: () -> LocaleIsoCode
) {
    /**
     * resId.platforms
     * resId.group
     * resId.localeIsoCode (strings)
     * resId.localeIsoCode.quantity (plurals)
     * resId.localeIsoCode.index (arrays)
     * resId.size (arrays)
     */
    protected val propertyStore = PropertyStore(File(Project.projectFolder(project.name), "${type.title}.properties"))

    val metadataById: MutableStateFlow<SortedMap<ResourceId, M>>
    val localizedResourcesById: MutableStateFlow<Map<ResourceId, Map<LocaleIsoCode, R>>>

    init {
        val metadata = sortedMapOf<ResourceId, M>()
        val resources = mutableMapOf<ResourceId, MutableMap<LocaleIsoCode, R>>()
        propertyStore.entries.forEach { (k, v) ->
            val resId = ResourceId(k.substringBefore('.'))
            when (val key = k.substringAfter('.')) {
                Metadata.PROP_GROUP -> metadata[resId] = metadata[resId]?.copyImpl(group = GroupId(v)) ?: Metadata(type = type, group = GroupId(v))
                Metadata.PROP_PLATFORMS -> {
                    val platforms = v.split(',').filter(String::isNotEmpty).map(Platform::valueOf)
                    metadata[resId] = metadata[resId]?.copyImpl(platforms = platforms) ?: Metadata(type = type, platforms = platforms)
                }
                ArrayMetadata.PROP_SIZE -> metadata[resId] = metadata[resId].copyOrCreate(v.toInt())
                else -> resources.getOrPut(resId) { mutableMapOf() }.addResource(resId, key, v)
            }
        }
        metadataById = MutableStateFlow(metadata)
        localizedResourcesById = MutableStateFlow(resources)
    }

    val scrollToItem = MutableStateFlow<ResourceId?>(null)

    fun platforms(resId: ResourceId) = metadataById.map { it[resId]?.platforms ?: Platform.ALL }

    fun createResource() {
        var newId = ResourceId("new")
        var n = 0
        val currentMetadata = metadataById.value
        while (currentMetadata[newId] != null) {
            newId = ResourceId("new$n")
            n++
        }
        metadataById.update { it.plus(newId to Metadata(type)).toSortedMap() }
        scrollToItem.value = newId

        propertyStore[Metadata.platformKey(newId)] = Platform.values().sorted().joinToString(separator = ",") { it.name }
        saveToDisk()
    }

    fun updateResourceId(oldId: ResourceId, newId: ResourceId): Boolean {
        if (newId in metadataById.value) return false

        val currentResources = localizedResourcesById.value
        val resourceMap = currentResources[oldId].orEmpty()
        metadataById.update { it.minus(oldId).plus(newId to it[oldId]!!).toSortedMap() }
        localizedResourcesById.update { it.minus(oldId).plus(newId to resourceMap) }

        propertyStore.filter { it.key.substringBefore('.') == oldId.value }.forEach { (k, v) ->
            propertyStore.remove(k)
            propertyStore["${newId.value}.${k.substringAfter('.')}"] = v
        }
        saveToDisk()
        return true
    }

    fun updateResource(resId: ResourceId, locale: LocaleIsoCode, resource: R) {
        localizedResourcesById.update { it.plus(resId to it[resId].orEmpty().plus(locale to resource)) }
        propertyStore.putResource("${resId.value}.${locale.value}", resource)
        saveToDisk()
    }

    fun removeResource(resId: ResourceId) {
        metadataById.update { it.minus(resId).toSortedMap() }
        localizedResourcesById.update { it.minus(resId) }
        propertyStore.filter { it.key.substringBefore('.') == resId.value }.forEach { propertyStore.remove(it.key) }
        saveToDisk()
    }

    fun deleteLocale(locale: LocaleIsoCode) {
        localizedResourcesById.update { it.mapValues { (_, resByLocale) -> resByLocale.minus(locale) } }
        propertyStore.filter { it.key.contains(".${locale.value}") }.forEach { propertyStore.remove(it.key) }
        saveToDisk()
    }

    fun togglePlatform(resId: ResourceId, platform: Platform) {
        val metadata = metadataById.value[resId] ?: Metadata(type)
        val platforms = metadata.platforms.run { if (contains(platform)) minus(platform) else plus(platform) }
        metadataById.update { it.plus(resId to metadata.copyImpl(platforms = platforms)).toSortedMap() }
        propertyStore[Metadata.platformKey(resId)] = platforms.sorted().joinToString(separator = ",") { it.name }
        saveToDisk()
    }

    fun findFirstInvalidResource(): ResourceId? =
        localizedResourcesById.value.entries.find { (_, localeMap) -> localeMap[defaultLocale()]?.isValid != true }?.key

    protected open fun M?.copyOrCreate(arraySize: Int): M? = null
    protected abstract fun MutableMap<LocaleIsoCode, R>.addResource(resId: ResourceId, key: String, value: String)
    protected abstract fun PropertyStore.putResource(baseKey: String, res: R)

    protected fun saveToDisk() {
        GlobalScope.launch(Dispatchers.IO) {
            runCatching { propertyStore.store("Localized ${type.title} resources") }.onFailure {
                println("Failed to save localized ${type.title} resources with $it")
            }
        }
    }
}

class StringResourceViewModel(project: Project, defaultLocale: () -> LocaleIsoCode) :
    ResourceTypeViewModel<Str, StringMetadata>(project, ResourceType.STRINGS, defaultLocale) {

    override fun MutableMap<LocaleIsoCode, Str>.addResource(resId: ResourceId, key: String, value: String) {
        put(LocaleIsoCode(key), Str(value))
    }

    override fun PropertyStore.putResource(baseKey: String, res: Str) {
        put(baseKey, res.text)
    }
}

class PluralResourceViewModel(project: Project, defaultLocale: () -> LocaleIsoCode) :
    ResourceTypeViewModel<Plural, PluralMetadata>(project, ResourceType.PLURALS, defaultLocale) {

    override fun MutableMap<LocaleIsoCode, Plural>.addResource(resId: ResourceId, key: String, value: String) {
        val locale = LocaleIsoCode(key.substringBefore('.'))
        val quantity = key.substringAfter('.').uppercase().let(Quantity::valueOf)
        put(locale, Plural(get(locale)?.items?.plus(quantity to value) ?: mapOf(quantity to value)))
    }

    override fun PropertyStore.putResource(baseKey: String, res: Plural) {
        Quantity.values().forEach { quantity ->
            val text = res.items[quantity]
            val key = "$baseKey.${quantity.label}"
            if (text == null) {
                remove(key)
            } else {
                put(key, text)
            }
        }
    }
}

class ArrayResourceViewModel(project: Project, defaultLocale: () -> LocaleIsoCode) :
    ResourceTypeViewModel<StringArray, ArrayMetadata>(project, ResourceType.ARRAYS, defaultLocale) {

    override fun MutableMap<LocaleIsoCode, StringArray>.addResource(resId: ResourceId, key: String, value: String) {
        if (key == ArrayMetadata.PROP_SIZE) return
        val locale = LocaleIsoCode(key.substringBefore('.'))
        val index = key.substringAfter('.').toInt()
        val size = propertyStore[ArrayMetadata.sizeKey(resId)]?.toInt() ?: (index + 1)
        val items = get(locale)?.items
        put(locale, StringArray(List(size) { i -> if (i == index) value else items?.getOrNull(i) ?: "" }))
    }

    override fun PropertyStore.putResource(baseKey: String, res: StringArray) {
        res.items.forEachIndexed { i, text -> put("$baseKey.$i", text) }
    }

    fun updateArraySize(resId: ResourceId, size: Int) {
        val oldSize = metadataById.value[resId]?.size ?: 0
        metadataById.update { it.plus(resId to it[resId].copyOrCreate(size)).toSortedMap() }
        localizedResourcesById.update {
            it.plus(resId to it[resId]?.mapValues { (_, array) -> StringArray(List(size) { i -> array.items.getOrElse(i) { "" } }) }.orEmpty())
        }

        propertyStore[ArrayMetadata.sizeKey(resId)] = "$size"
        if (oldSize > size) {
            propertyStore.forEach { (k, _) ->
                if (k.substringBefore('.') == resId.value && k.last().isDigit() && k.substringAfterLast('.').toInt() > size) {
                    propertyStore.remove(k)
                }
            }
        }

        saveToDisk()
    }

    override fun ArrayMetadata?.copyOrCreate(arraySize: Int): ArrayMetadata = this?.copy(size = arraySize) ?: ArrayMetadata(size = arraySize)
}
