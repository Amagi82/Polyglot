package ui.resource

import data.*
import data.exporters.ExportResourceData
import kotlinx.coroutines.flow.*
import locales.LocaleIsoCode
import project.*

sealed class ResourceTypeViewModel<R : Resource, M : Metadata<M>>(projectStore: ProjectStore, protected val propertyStore: ResourceStore<R, M>) {
    val type: ResourceType = propertyStore.type
    private val defaultLocale = projectStore.defaultLocale::value

    val metadataById: MutableStateFlow<Map<ResourceId, M>> = propertyStore.metadataById
    val localizedResourcesById: MutableStateFlow<Map<ResourceId, Map<LocaleIsoCode, R>>> = propertyStore.localizedResourcesById
    val selectedRows = MutableStateFlow(listOf<ResourceId>())

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
        propertyStore.createResource(newId)
        scrollToItem.value = newId
    }

    fun updateResourceId(oldId: ResourceId, newId: ResourceId): Boolean {
        if (newId in metadataById.value) return false
        propertyStore.updateResourceId(oldId, newId)
        return true
    }

    fun updateResource(resId: ResourceId, locale: LocaleIsoCode, resource: R) = propertyStore.updateResource(resId, locale, resource)

    fun removeResource(resId: ResourceId) = propertyStore.removeResource(resId)

    fun deleteLocale(locale: LocaleIsoCode) = propertyStore.deleteLocale(locale)

    fun putSelectedInGroup(group: GroupId) {
        val selected = selectedRows.value
        if (selected.isEmpty()) return
        propertyStore.putSelectedInGroup(group, selected)
        selectedRows.value = listOf()
    }

    fun togglePlatform(resId: ResourceId, platform: Platform) = propertyStore.togglePlatform(resId, platform)

    fun findFirstInvalidResource(): ResourceId? = metadataById.value.keys.firstOrNull { resId ->
        localizedResourcesById.value[resId]?.get(defaultLocale())?.isValid != true
    }

    fun exportResourceData(): ExportResourceData<R, M> = ExportResourceData(
        type = type,
        metadataByIdByGroup = metadataById.value.entries
            .groupBy { it.value.group }
            .mapValues { (_, v) -> v.associate { it.key to it.value }.toSortedMap() }
            .toSortedMap(),
        localizedResourcesById = localizedResourcesById.value
    )
}

class StringResourceViewModel(projectStore: ProjectStore) : ResourceTypeViewModel<Str, StringMetadata>(projectStore, StringStore(projectStore.name))

class PluralResourceViewModel(projectStore: ProjectStore) : ResourceTypeViewModel<Plural, PluralMetadata>(projectStore, PluralStore(projectStore.name))

class ArrayResourceViewModel(projectStore: ProjectStore) : ResourceTypeViewModel<StringArray, ArrayMetadata>(projectStore, ArrayStore(projectStore.name)) {
    fun updateArraySize(resId: ResourceId, size: Int) = (propertyStore as ArrayStore).updateArraySize(resId, size)
}
