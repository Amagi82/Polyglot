package ui.resource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import locales.LocaleIsoCode
import project.*

sealed class ResourceTypeViewModel<R : Resource>(val project: MutableStateFlow<Project>, val type: ResourceType) {
    val resourceMetadata = MutableStateFlow(project.value.loadMetadata(type))
    val resourcesByLocale = MutableStateFlow(project.value.loadResources<R>(type))
    val displayedResources = resourceMetadata.map { it.keys.sorted() }
    val scrollToItem = MutableStateFlow<ResourceId?>(null)

    init {
        GlobalScope.launch(Dispatchers.IO) { resourceMetadata.collectLatest { project.value.saveMetadata(it, type) } }
        GlobalScope.launch(Dispatchers.IO) { resourcesByLocale.collectLatest { project.value.saveResources(it, type) } }
    }

    fun platforms(resId: ResourceId) = resourceMetadata.map { it[resId]?.platforms ?: Platform.ALL }

    fun resource(resId: ResourceId, localeIsoCode: LocaleIsoCode) = resourcesByLocale.map { it[localeIsoCode]?.get(resId) }

    fun createResource() {
        var newId = ResourceId("new")
        var n = 0
        val current = resourceMetadata.value
        while (current[newId] != null) {
            newId = ResourceId("new$n")
            n++
        }
        resourceMetadata.value = resourceMetadata.value.plus(newId to Metadata(type))
        scrollToItem.value = newId
    }

    fun updateResourceId(oldId: ResourceId, newId: ResourceId): Boolean {
        val current = resourceMetadata.value
        if (newId in current) return false
        val value = current[oldId]!!
        resourceMetadata.value = current.minus(oldId).plus(newId to value)
        resourcesByLocale.value = resourcesByLocale.value.map { (locale, resources) ->
            locale to resources.map { (resId, resource) -> (if (resId == oldId) newId else resId) to resource }.toMap()
        }.toMap()
        return true
    }

    fun updateResource(locale: LocaleIsoCode, resId: ResourceId, resource: R) {
        val current = resourcesByLocale.value
        resourcesByLocale.value = current.plus(locale to current[locale].orEmpty().plus(resId to resource))
    }

    fun removeResource(resId: ResourceId) {
        resourceMetadata.value = resourceMetadata.value.minus(resId)
        resourcesByLocale.value = resourcesByLocale.value.map { it.key to it.value.minus(resId) }.toMap()
    }

    fun togglePlatform(resId: ResourceId, platform: Platform) {
        val metadata = resourceMetadata.value[resId] ?: Metadata(type)
        val platforms = metadata.platforms.run { if (contains(platform)) minus(platform) else plus(platform) }
        resourceMetadata.value = resourceMetadata.value.plus(resId to metadata.copy(platforms = platforms))
    }

    fun findFirstInvalidResource(): ResourceId? {
        val defaultResources = resourcesByLocale.value[project.value.defaultLocale]!!
        return resourceMetadata.value.keys.find { resId -> defaultResources[resId]?.isValid != true }
    }
}

class StringResourceViewModel(project: MutableStateFlow<Project>) : ResourceTypeViewModel<Str>(project, ResourceType.STRINGS)

class PluralResourceViewModel(project: MutableStateFlow<Project>) : ResourceTypeViewModel<Plural>(project, ResourceType.PLURALS)

class ArrayResourceViewModel(project: MutableStateFlow<Project>) : ResourceTypeViewModel<StringArray>(project, ResourceType.ARRAYS) {
    val arraySizes = MutableStateFlow(project.value.loadArraySizes())
    fun arraySize(resId: ResourceId) = arraySizes.map { it[resId]?.coerceAtLeast(1) ?: 1 }

    init {
        GlobalScope.launch(Dispatchers.IO) { arraySizes.collectLatest { project.value.saveArraySizes(it) } }
    }

    fun updateArraySize(resId: ResourceId, size: Int) {
        arraySizes.value = arraySizes.value.plus(resId to size)
        resourcesByLocale.value = resourcesByLocale.value.map { (locale, resourceMap) ->
            locale to resourceMap.map { (resId, array) -> resId to StringArray(List(size) { i -> array.items.getOrElse(i) { "" } }) }.toMap()
        }.toMap()
    }
}
