package data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import locales.LocaleIsoCode
import project.*
import java.io.File

/**
 * _group_.name=resId,resId...
 * _platform_.platform=resId,resId...
 * resId.localeIsoCode (strings)
 * resId.localeIsoCode.quantity (plurals)
 * resId.localeIsoCode.index (arrays)
 */
abstract class ResourceStore<R : Resource> protected constructor(projectName: String, val type: ResourceType) :
    FilePropertyStore(File(Project.projectFolder(projectName), "${type.title}.properties")) {

    val resourceGroups: MutableStateFlow<Map<ResourceGroup, Set<ResourceId>>> by lazy {
        MutableStateFlow(entries.filter { it.key.startsWith(GROUP_KEY) }
            .associate { (k, v) -> ResourceGroup(name = k.substringAfter('.', missingDelimiterValue = "")) to v.toResourceIdSet() })
    }

    val excludedResourcesByPlatform: MutableStateFlow<Map<Platform, Set<ResourceId>>> by lazy {
        MutableStateFlow(entries.filter { it.key.startsWith(PLATFORM_KEY) }
            .associate { (k, v) -> k.substringAfter('.').uppercase().let(Platform::valueOf) to v.toResourceIdSet() })
    }

    val localizedResourcesById: MutableStateFlow<Map<ResourceId, Map<LocaleIsoCode, R>>> by lazy {
        MutableStateFlow(
            entries.filterNot { it.key.startsWith(PLATFORM_KEY) || it.key.startsWith(GROUP_KEY) }
                .groupBy(
                    keySelector = { ResourceId(it.key.substringBefore('.')) },
                    valueTransform = { it.key.substringAfter('.') to it.value })
                .mapValues { (resId, v) ->
                    if (resourceGroups.value.none { resId in it.value }) {
                        resourceGroups.update { it.plus(ResourceGroup() to it[ResourceGroup()].orEmpty().plus(resId)) }
                    }
                    v.filterNot { (k, _) -> k.startsWith(PLATFORM_KEY) }
                        .groupBy(
                            keySelector = { (k, _) -> LocaleIsoCode(k.substringBefore('.')) },
                            valueTransform = { (resKey, resValue) -> resKey.substringAfter('.') to resValue })
                        .mapValues { resource(it.value.toMap()) }
                }
        )
    }

    fun createResource(newId: ResourceId) {
        resourceGroups.update {
            val newIds = it[ResourceGroup()].orEmpty().plus(newId)
            putGroupIds(ResourceGroup(), newIds)
            it.plus(ResourceGroup() to newIds)
        }
        localizedResourcesById.update { it.plus(newId to mapOf()) }
        save()
    }

    fun updateResourceId(oldId: ResourceId, newId: ResourceId) {
        resourceGroups.update {
            it.mapValues { (group, ids) ->
                if (ids.contains(oldId)) ids.minus(oldId).plus(newId).also { newIds -> putGroupIds(group, newIds) } else ids
            }
        }
        localizedResourcesById.update { it.minus(oldId).plus(newId to it[oldId].orEmpty()) }
        excludedResourcesByPlatform.update { it.mapValues { (_, keys) -> if (keys.contains(oldId)) keys.minus(oldId).plus(newId) else keys } }

        for ((k, v) in this) {
            if (k.substringBefore('.') != oldId.value) continue
            remove(k)
            put("${newId.value}.${k.substringAfter('.')}", v)
        }
        save()
    }

    private fun putGroupIds(group: ResourceGroup, ids: Set<ResourceId>) {
        put("$GROUP_KEY.${group.name}".removeSuffix("."), ids.joinToString(",", transform = ResourceId::value))
    }

    fun updateResource(resId: ResourceId, locale: LocaleIsoCode, resource: R) {
        localizedResourcesById.update { it.plus(resId to it[resId].orEmpty().plus(locale to resource)) }
        putResource("${resId.value}.${locale.value}", resource)
        save()
    }

    fun removeResource(resId: ResourceId) {
        resourceGroups.update {
            it.mapValues { (group, ids) ->
                if (ids.contains(resId)) ids.minus(resId).also { newIds -> putGroupIds(group, newIds) } else ids
            }
        }
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

    fun putSelectedInGroup(group: ResourceGroup, selected: Set<ResourceId>) {
        if (selected.isEmpty()) return
        resourceGroups.update {
            it.plus(group to it[group].orEmpty().plus(selected)).mapValues { (grp, ids) -> if (grp == group) ids else ids.minus(selected) }.also { newGroups ->
                newGroups.forEach(::putGroupIds)
            }
        }
        save()
    }

    fun togglePlatform(resId: ResourceId, platform: Platform) {
        excludedResourcesByPlatform.update {
            val current = it[platform].orEmpty()
            it.plus(platform to if (current.contains(resId)) current.minus(resId) else current.plus(resId)).also { updated ->
                put("$PLATFORM_KEY.${platform.lowercase}", updated[platform].orEmpty().joinToString(",", transform = ResourceId::value))
            }
        }
        save()
    }

    private fun String.toResourceIdSet() = split(',').map(::ResourceId).toSet()
    protected abstract fun resource(values: Map<String, String>): R
    protected abstract fun putResource(baseKey: String, res: R)

    override fun save() {
        store("Localized ${type.title} resources") { println("Failed to save localized ${type.title} resources with $it") }
    }

    companion object {
        private const val PLATFORM_KEY = "_platform_"
        private const val GROUP_KEY = "_group_"
    }
}
