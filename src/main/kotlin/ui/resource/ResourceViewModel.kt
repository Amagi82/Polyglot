package ui.resource

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
    val localizedResources = MutableStateFlow(Project.loadLocalizedResources(project.name))
    val projectLocales = localizedResources.map { res -> res.keys.sortedBy { Locale[it].displayName() } }

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

    val includedResources = combine(comparator, localizedResources, excludedLocales) { comparator, localizedResources, excludedLocales ->
        localizedResources.filter { it.key !in excludedLocales }.toSortedMap(comparator)
    }

    init {
        GlobalScope.launch(Dispatchers.IO) { this@ResourceViewModel.project.collectLatest { it.save() } }
        GlobalScope.launch(Dispatchers.IO) { resourceMetadata.collectLatest { it.save(project.name) } }
        GlobalScope.launch(Dispatchers.IO) { localizedResources.collectLatest { it.save(project.name) } }
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
        localizedResources.value = localizedResources.value.map { it.key to it.value.minus(resId) }.toMap()
    }

    fun deleteLocale(isoCode: LocaleIsoCode) {
        val removeList = mutableSetOf(isoCode)
        // base language is required, so if that's removed, remove the dialects that depend on it
        if (isoCode.isBaseLanguage) {
            localizedResources.value.keys.forEach {
                if (it.value.startsWith(isoCode.value)) {
                    removeList += it
                }
            }
        }
        localizedResources.value = localizedResources.value.minus(removeList)
        removeList.forEach {
            Project.localizedResourcesFile(project.value.name, it).delete()
        }
    }
}