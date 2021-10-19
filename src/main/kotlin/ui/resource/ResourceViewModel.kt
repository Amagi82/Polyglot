package ui.resource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.*
import ui.resource.menu.MenuState
import java.util.*

class ResourceViewModel(project: Project) {
    val project = MutableStateFlow(project)
    val resourceMetadata = MutableStateFlow(Project.loadResourceMetadata(project.name))
    val localizedResources = MutableStateFlow(Project.loadLocalizedResources(project.name))

    val menuState = MutableStateFlow(MenuState.CLOSED)

    val excludedLocales = MutableStateFlow(setOf<LocaleIsoCode>())
    val excludedResourceInfoTypes = MutableStateFlow(setOf<ResourceInfo.Type>())

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
        GlobalScope.launch(Dispatchers.IO) { resourceMetadata.collect { it.save(project.name) } }
        GlobalScope.launch(Dispatchers.IO) { localizedResources.collect { it.save(project.name) } }
    }

    fun createResource() {
        var newId = ResourceId("new")
        var n = 0
        while (resourceMetadata.value[newId] != null) {
            newId = ResourceId("new$n")
            n++
        }
        resourceMetadata.value = resourceMetadata.value.plus(newId to ResourceInfo())
    }

    fun deleteLocale(isoCode: LocaleIsoCode) {
        Project.localizedResourcesFile(project.value.name, isoCode).delete()
        localizedResources.value = localizedResources.value.minus(isoCode)
    }
}
