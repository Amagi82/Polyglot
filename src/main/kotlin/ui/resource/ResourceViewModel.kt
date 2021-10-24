package ui.resource

import generators.ResourceGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.*
import java.util.*

class ResourceViewModel(project: Project) {
    val project = MutableStateFlow(project)
    val resourceMetadata = MutableStateFlow(Project.loadResourceMetadata(project.name))
    private val resourcesByLocale = MutableStateFlow(Project.loadLocalizedResources(project.name))
    val projectLocales = resourcesByLocale.map { res -> res.keys.sortedBy { Locale[it].displayName() } }

    val excludedLocales = MutableStateFlow(setOf<LocaleIsoCode>())
    val selectedTab = MutableStateFlow(ResourceInfo.Type.STRING)

    val displayedResources = combine(resourceMetadata, selectedTab) { resourceMetadata, selectedTab ->
        resourceMetadata.filter { it.value.type == selectedTab }.keys.sorted()
    }

    private val comparator = this.project.map { project ->
        Comparator<LocaleIsoCode> { o1, o2 ->
            when {
                o1 == project.defaultLocale -> -1
                o2 == project.defaultLocale -> 1
                else -> Locale[o1].displayName().compareTo(Locale[o2].displayName())
            }
        }
    }

    val includedResourcesByLocale = combine(comparator, resourcesByLocale, excludedLocales) { comparator, localizedResources, excludedLocales ->
        localizedResources.filter { it.key !in excludedLocales }.toSortedMap(comparator)
    }

    init {
        GlobalScope.launch(Dispatchers.IO) { this@ResourceViewModel.project.collectLatest { it.save() } }
        GlobalScope.launch(Dispatchers.IO) { resourceMetadata.collectLatest { it.save(project.name) } }
        GlobalScope.launch(Dispatchers.IO) { resourcesByLocale.collectLatest { it.save(project.name) } }
    }

    fun createResource() {
        var newId = ResourceId("new")
        var n = 0
        while (resourceMetadata.value[newId] != null) {
            newId = ResourceId("new$n")
            n++
        }
        resourceMetadata.value = resourceMetadata.value.plus(newId to ResourceInfo(type = selectedTab.value))
    }

    fun removeResource(resId: ResourceId) {
        resourceMetadata.value = resourceMetadata.value.minus(resId)
        resourcesByLocale.value = resourcesByLocale.value.map { it.key to it.value.minus(resId) }.toMap()
    }

    fun addLocale(isoCode: LocaleIsoCode) {
        val localesToAdd = mutableListOf(isoCode)
        if (!isoCode.isBaseLanguage) {
            localesToAdd.add(Locale[isoCode].copy(region = null).isoCode)
        }
        val current = resourcesByLocale.value
        resourcesByLocale.value = current.plus(localesToAdd.map { it to current[it].orEmpty() })
    }

    fun deleteLocale(isoCode: LocaleIsoCode) {
        val localesToRemove = mutableSetOf(isoCode)
        // base language is required, so if that's removed, remove the dialects that depend on it
        val current = resourcesByLocale.value
        if (isoCode.isBaseLanguage) {
            current.keys.forEach {
                if (it.value.startsWith(isoCode.value)) {
                    localesToRemove += it
                }
            }
        }
        resourcesByLocale.value = current.minus(localesToRemove)
        localesToRemove.forEach {
            Project.localizedResourcesFile(project.value.name, it).delete()
        }
    }

    suspend fun generateFiles() {
        ResourceGenerator.generateFiles(project.value, resourceMetadata.value, resourcesByLocale.value)
    }
}
