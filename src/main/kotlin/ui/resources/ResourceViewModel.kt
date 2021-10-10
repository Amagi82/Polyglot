package ui.resources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import locales.LocaleIsoCode
import project.Project
import project.Resource
import project.ResourceId
import project.save

class ResourceViewModel(project: Project) {
    val project = MutableStateFlow(project)
    val resources = MutableStateFlow(Project.loadResources(project.name))
    val localizedResources = MutableStateFlow(Project.loadLocalizedResources(project.name))

    val excludedLocales = MutableStateFlow(setOf<LocaleIsoCode>())
    val excludedResourceIds = MutableStateFlow(setOf<ResourceId>())
    val excludedResourceTypes = MutableStateFlow(setOf<Resource.Type>())

    val showFilters = MutableStateFlow(false)
    val showProjectSettings = MutableStateFlow(false)

    init {
        GlobalScope.launch(Dispatchers.IO) { resources.collect { it.save(project.name) } }
        GlobalScope.launch(Dispatchers.IO) { localizedResources.collect { it.save(project.name) } }
    }
}
