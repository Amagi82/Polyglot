package ui.resource

import data.*
import data.exporters.ExportResourceData
import kotlinx.coroutines.flow.*
import locales.LocaleIsoCode
import project.*

sealed class ResourceTypeViewModel<R : Resource>(projectStore: ProjectStore, val propertyStore: ResourceStore<R>) {
    val type: ResourceType = propertyStore.type
    protected val defaultLocale = projectStore.defaultLocale::value

    val resourceGroups: MutableStateFlow<Map<ResourceGroup, Set<ResourceId>>> = propertyStore.resourceGroups
    val excludedResourcesByPlatform: MutableStateFlow<Map<Platform, Set<ResourceId>>> = propertyStore.excludedResourcesByPlatform
    val localizedResourcesById: MutableStateFlow<Map<ResourceId, Map<LocaleIsoCode, R>>> = propertyStore.localizedResourcesById
    val selectedRows = MutableStateFlow(setOf<ResourceId>())

    val scrollToItem = MutableStateFlow<ResourceId?>(null)

    fun platforms(resId: ResourceId) = Platform.values().filterNot { excludedResourcesByPlatform.value[it]?.contains(resId) == true }

    fun createResource() {
        var newId = ResourceId("new")
        var n = 0

        val ids = localizedResourcesById.value.keys
        while (newId in ids) {
            newId = ResourceId("new$n")
            n++
        }
        propertyStore.createResource(newId)
        scrollToItem.value = newId
    }

    fun updateResourceId(oldId: ResourceId, newId: ResourceId): Boolean {
        if (newId in localizedResourcesById.value.keys) return false
        propertyStore.updateResourceId(oldId, newId)
        return true
    }

    fun updateResource(resId: ResourceId, locale: LocaleIsoCode, resource: R) = propertyStore.updateResource(resId, locale, resource)

    fun removeResource(resId: ResourceId) = propertyStore.removeResource(resId)

    fun deleteLocale(locale: LocaleIsoCode) = propertyStore.deleteLocale(locale)

    fun putSelectedInGroup(group: ResourceGroup) {
        val selected = selectedRows.value
        if (selected.isEmpty()) return
        propertyStore.putSelectedInGroup(group, selected)
        selectedRows.value = setOf()
    }

    fun togglePlatform(resId: ResourceId, platform: Platform) = propertyStore.togglePlatform(resId, platform)

    fun findFirstInvalidResource(): ResourceId? = localizedResourcesById.value.keys.firstOrNull { resId ->
        localizedResourcesById.value[resId]?.get(defaultLocale())?.isValid != true
    }

    fun exportResourceData(): ExportResourceData<R> = ExportResourceData(
        type = type,
        resourceGroups = resourceGroups.value,
        excludedResourcesByPlatform = excludedResourcesByPlatform.value,
        localizedResourcesById = localizedResourcesById.value
    )
}

class StringResourceViewModel(projectStore: ProjectStore) : ResourceTypeViewModel<Str>(projectStore, StringStore(projectStore.name))

class PluralResourceViewModel(projectStore: ProjectStore) : ResourceTypeViewModel<Plural>(projectStore, PluralStore(projectStore.name))

class ArrayResourceViewModel(projectStore: ProjectStore) : ResourceTypeViewModel<StringArray>(projectStore, ArrayStore(projectStore.name)) {
    fun arraySize(resId: ResourceId) = localizedResourcesById.value[resId]?.get(defaultLocale())?.items?.size ?: 1
    fun updateArraySize(resId: ResourceId, size: Int) = (propertyStore as ArrayStore).updateArraySize(resId, defaultLocale(), size)
}
