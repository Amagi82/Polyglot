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

sealed class ResourceTypeViewModel<R : Resource>(val project: Project, val type: ResourceType, private val defaultLocale: () -> LocaleIsoCode) {
    /**
     * resId.platforms
     * resId.group
     * resId.localeIsoCode (strings)
     * resId.localeIsoCode.quantity (plurals)
     * resId.localeIsoCode.index (arrays)
     * resId.size (arrays)
     */
    protected val propertyStore = PropertyStore(File(Project.projectFolder(project.name), "${type.title}.properties"))

    val metadataById: MutableStateFlow<SortedMap<ResourceId, Metadata>>
    val localizedResourcesById: MutableStateFlow<Map<ResourceId, Map<LocaleIsoCode, R>>>

    init {
        val initialMetadata = sortedMapOf<ResourceId, Metadata>()
        val initialResources = mutableMapOf<ResourceId, MutableMap<LocaleIsoCode, R>>()
        propertyStore.entries.forEach { (k, v) ->
            val resId = ResourceId(k.substringBefore('.'))
            when (val key = k.substringAfter('.')) {
                Metadata.PROP_GROUP -> initialMetadata[resId] = initialMetadata[resId]?.copy(group = GroupId(v)) ?: Metadata(type = type, group = GroupId(v))
                Metadata.PROP_PLATFORMS -> {
                    val platforms = v.split(',').filter(String::isNotEmpty).map(Platform::valueOf)
                    initialMetadata[resId] = initialMetadata[resId]?.copy(platforms = platforms) ?: Metadata(type = type, platforms = platforms)
                }
                else -> initialResources.getOrPut(resId) { mutableMapOf() }.addResource(resId, key, v)
            }
        }
        metadataById = MutableStateFlow(initialMetadata)
        localizedResourcesById = MutableStateFlow(initialResources)
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
        propertyStore.put(resId, locale, resource)
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
        metadataById.update { it.plus(resId to metadata.copy(platforms = platforms)).toSortedMap() }
        propertyStore[Metadata.platformKey(resId)] = platforms.sorted().joinToString(separator = ",") { it.name }
        saveToDisk()
    }

    fun findFirstInvalidResource(): ResourceId? =
        localizedResourcesById.value.entries.find { (_, localeMap) -> localeMap[defaultLocale()]?.isValid != true }?.key

    protected abstract fun MutableMap<LocaleIsoCode, R>.addResource(resId: ResourceId, key: String, value: String)

    protected abstract fun PropertyStore.put(resId: ResourceId, locale: LocaleIsoCode, res: R)

    protected fun saveToDisk() {
        GlobalScope.launch(Dispatchers.IO) {
            runCatching { propertyStore.store("Localized ${type.title} resources") }.onFailure {
                println("Failed to save localized ${type.title} resources with $it")
            }
        }
    }
}

class StringResourceViewModel(project: Project, defaultLocale: () -> LocaleIsoCode) : ResourceTypeViewModel<Str>(project, ResourceType.STRINGS, defaultLocale) {

    override fun MutableMap<LocaleIsoCode, Str>.addResource(resId: ResourceId, key: String, value: String) {
        put(LocaleIsoCode(key), Str(value))
    }

    override fun PropertyStore.put(resId: ResourceId, locale: LocaleIsoCode, res: Str) {
        put("${resId.value}.${locale.value}", res.text)
    }
}

class PluralResourceViewModel(project: Project, defaultLocale: () -> LocaleIsoCode) :
    ResourceTypeViewModel<Plural>(project, ResourceType.PLURALS, defaultLocale) {

    override fun MutableMap<LocaleIsoCode, Plural>.addResource(resId: ResourceId, key: String, value: String) {
        val locale = LocaleIsoCode(key.substringBefore('.'))
        val quantity = key.substringAfter('.').uppercase().let(Quantity::valueOf)
        put(locale, Plural(get(locale)?.items?.plus(quantity to value) ?: mapOf(quantity to value)))
    }

    override fun PropertyStore.put(resId: ResourceId, locale: LocaleIsoCode, res: Plural) {
        Quantity.values().forEach { quantity ->
            val text = res.items[quantity]
            val key = "${resId.value}.${locale.value}.${quantity.label}"
            if (text == null) {
                remove(key)
            } else {
                put(key, text)
            }
        }
    }
}

class ArrayResourceViewModel(project: Project, defaultLocale: () -> LocaleIsoCode) :
    ResourceTypeViewModel<StringArray>(project, ResourceType.ARRAYS, defaultLocale) {
    val arraySizes = MutableStateFlow<Map<ResourceId, Int>>(mapOf())

    init {
        arraySizes.value = metadataById.value.keys.associateWith { propertyStore["${it.value}.size"]?.toInt() ?: 0 }
    }

    override fun MutableMap<LocaleIsoCode, StringArray>.addResource(resId: ResourceId, key: String, value: String) {
        if (key == "size") return
        val locale = LocaleIsoCode(key.substringBefore('.'))
        val index = key.substringAfter('.').toInt()
        val size = propertyStore["${resId.value}.size"]?.toInt() ?: (index + 1)
        val items = get(locale)?.items
        put(locale, StringArray(List(size) { i -> if (i == index) value else items?.getOrNull(i) ?: "" }))
    }

    override fun PropertyStore.put(resId: ResourceId, locale: LocaleIsoCode, res: StringArray) {
        res.items.forEachIndexed { i, text ->
            put("${resId.value}.${locale.value}.$i", text)
        }
    }

    fun updateArraySize(resId: ResourceId, size: Int) {
        val oldSize = arraySizes.value[resId] ?: size
        arraySizes.value = arraySizes.value.plus(resId to size)
        localizedResourcesById.update {
            it.mapValues { (_, resByLocale) -> resByLocale.mapValues { (_, array) -> StringArray(List(size) { i -> array.items.getOrElse(i) { "" } }) } }
        }

        propertyStore["${resId.value}.size"] = "$size"
        if (oldSize > size) {
            propertyStore.filter {
                it.key.substringBefore('.') == resId.value
                        && it.key.last().isDigit()
                        && it.key.substringAfterLast('.').toInt() > size
            }.forEach {
                propertyStore.remove(it.key)
            }
        }

        saveToDisk()
    }
}
