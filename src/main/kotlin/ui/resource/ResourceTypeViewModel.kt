package ui.resource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import locales.LocaleIsoCode
import project.*

sealed class ResourceTypeViewModel<M : Metadata, R : Resource<M>>(val project: MutableStateFlow<Project>, private val type: ResourceType) {
    val resourceMetadata = MutableStateFlow(Project.loadResourceMetadata<M>(project.value.name, type))
    val resourcesByLocale = MutableStateFlow(Project.loadLocalizedResources<M, R>(project.value.name, type))
    val displayedResources = resourceMetadata.map { metadata -> metadata.toList().sortedBy { it.first.value } }

    init {
        GlobalScope.launch(Dispatchers.IO) { resourceMetadata.collectLatest { it.save(project.value.name, type) } }
        GlobalScope.launch(Dispatchers.IO) { resourcesByLocale.collectLatest { it.save(project.value.name, type) } }
    }

    fun resource(resId: ResourceId, localeIsoCode: LocaleIsoCode) = resourcesByLocale.map { it[localeIsoCode]?.get(resId) }

    fun createResource() {
        var newId = ResourceId("new")
        var n = 0
        val current = resourceMetadata.value
        while (current[newId] != null) {
            newId = ResourceId("new$n")
            n++
        }
        resourceMetadata.value = resourceMetadata.value.plus(newId to createMetadata())
    }

    protected abstract fun createMetadata(): M

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

    fun togglePlatform(resId: ResourceId, info: M, platform: Platform) {
        val platforms = if (platform in info.platforms) info.platforms.minus(platform) else info.platforms.plus(platform)
        resourceMetadata.value = resourceMetadata.value.plus(resId to info.copyPlatforms(platforms))
    }

    protected abstract fun M.copyPlatforms(platforms: List<Platform>): M
}

class StringResourceViewModel(project: MutableStateFlow<Project>) : ResourceTypeViewModel<Str.Metadata, Str>(project, ResourceType.STRINGS) {
    override fun createMetadata() = Str.Metadata()
    override fun Str.Metadata.copyPlatforms(platforms: List<Platform>) = copy(platforms = platforms)
}

class PluralResourceViewModel(project: MutableStateFlow<Project>) : ResourceTypeViewModel<Plural.Metadata, Plural>(project, ResourceType.PLURALS) {
    override fun createMetadata() = Plural.Metadata()
    override fun Plural.Metadata.copyPlatforms(platforms: List<Platform>) = copy(platforms = platforms)
}

class ArrayResourceViewModel(project: MutableStateFlow<Project>) : ResourceTypeViewModel<StringArray.Metadata, StringArray>(project, ResourceType.ARRAYS) {
    override fun createMetadata() = StringArray.Metadata()
    override fun StringArray.Metadata.copyPlatforms(platforms: List<Platform>) = copy(platforms = platforms)
}
