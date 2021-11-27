package data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import locales.LocaleIsoCode
import project.*
import java.io.File

/**
 * resId.platforms
 * resId.group
 * resId.localeIsoCode (strings)
 * resId.localeIsoCode.quantity (plurals)
 * resId.localeIsoCode.index (arrays)
 * resId.size (arrays)
 */
abstract class ResourceStore<R : Resource, M : Metadata<M>> protected constructor(projectName: String, val type: ResourceType) :
    FilePropertyStore(File(Project.projectFolder(projectName), "${type.title}.properties")) {

    val metadataById by lazy {
        MutableStateFlow<Map<ResourceId, M>>(
            entries.groupBy(
                keySelector = { ResourceId(it.key.substringBefore('.')) },
                valueTransform = { it.key.substringAfter('.') to it.value })
                .mapValues { (_, v) ->
                    val map = v.toMap()
                    createMetadata(
                        group = GroupId(map[Metadata.PROP_GROUP].orEmpty()),
                        platforms = map[Metadata.PROP_PLATFORMS]?.split(',')?.filter(String::isNotEmpty)?.map(Platform::valueOf) ?: Platform.ALL,
                        arraySize = map[ArrayMetadata.PROP_SIZE]?.toInt()
                    )
                }.toSortedMap()
        )
    }

    val localizedResourcesById by lazy {
        MutableStateFlow(
            entries.groupBy(
                keySelector = { ResourceId(it.key.substringBefore('.')) },
                valueTransform = { it.key.substringAfter('.') to it.value })
                .mapValues { (resId, v) ->
                    v.filterNot { (k, _) -> k.startsWith(Metadata.PROP_GROUP) || k.startsWith(Metadata.PROP_PLATFORMS) || k.startsWith(ArrayMetadata.PROP_SIZE) }
                        .groupBy(
                            keySelector = { (k, _) -> LocaleIsoCode(k.substringBefore('.')) },
                            valueTransform = { (resKey, resValue) -> resKey.substringAfter('.') to resValue })
                        .mapValues { createResource(it.value.toMap(), arraySize = get(ArrayMetadata.sizeKey(resId))?.toInt()) }
                }
        )
    }

    fun createResource(newId: ResourceId) {
        metadataById.update { it.plus(newId to createMetadata()) }
        put(Metadata.platformKey(newId), Platform.values().sorted().joinToString(separator = ",") { it.name })
        save()
    }

    fun updateResourceId(oldId: ResourceId, newId: ResourceId) {
        metadataById.update { it.minus(oldId).plus(newId to it[oldId]!!).toSortedMap() }
        localizedResourcesById.update { it.minus(oldId).plus(newId to it[oldId].orEmpty()) }

        for ((k, v) in this) {
            if (k.substringBefore('.') != oldId.value) continue
            remove(k)
            put("${newId.value}.${k.substringAfter('.')}", v)
        }
        save()
    }

    fun updateResource(resId: ResourceId, locale: LocaleIsoCode, resource: R) {
        localizedResourcesById.update { it.plus(resId to it[resId].orEmpty().plus(locale to resource)) }
        putResource("${resId.value}.${locale.value}", resource)
        save()
    }

    fun removeResource(resId: ResourceId) {
        metadataById.update { it.minus(resId).toSortedMap() }
        localizedResourcesById.update { it.minus(resId) }
        for (k in keys) {
            if (k.substringBefore('.') != resId.value) continue
            remove(k)
        }
        save()
    }

    fun deleteLocale(locale: LocaleIsoCode) {
        localizedResourcesById.update { it.mapValues { (_, resByLocale) -> resByLocale.minus(locale) } }
        for (k in keys) {
            if (!k.contains(".${locale.value}")) continue
            remove(k)
        }
        save()
    }

    fun putSelectedInGroup(group: GroupId, selected: List<ResourceId>) {
        if (selected.isEmpty()) return
        metadataById.update { it.mapValues { (resId, metadata) -> if (resId in selected) metadata.copyImpl(group = group) else metadata }.toSortedMap() }
        selected.forEach { resId ->
            put(Metadata.groupKey(resId), group.value)

        }
        save()
    }

    fun togglePlatform(resId: ResourceId, platform: Platform) {
        val metadata = metadataById.value[resId] ?: Metadata(type)
        val platforms = metadata.platforms.run { if (contains(platform)) minus(platform) else plus(platform) }
        metadataById.update { it.plus(resId to metadata.copyImpl(platforms = platforms)).toSortedMap() }
        put(Metadata.platformKey(resId), platforms.sorted().joinToString(separator = ",") { it.name })
        save()
    }

    protected abstract fun createMetadata(group: GroupId = GroupId(), platforms: List<Platform> = Platform.ALL, arraySize: Int? = null): M
    protected abstract fun createResource(values: Map<String, String>, arraySize: Int?): R
    protected abstract fun putResource(baseKey: String, res: R)

    override fun save() {
        store("Localized ${type.title} resources") { println("Failed to save localized ${type.title} resources with $it") }
    }
}
