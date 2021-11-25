package ui.resource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.Platform
import project.Project
import project.ResourceId
import project.ResourceType
import data.PropertyStore
import java.io.File

class ResourceViewModel(val project: Project) {
    val excludedLocales = MutableStateFlow(setOf<LocaleIsoCode>())
    val selectedTab = MutableStateFlow(ResourceType.STRINGS)
    val isMultiSelectEnabled = MutableStateFlow(false)

    private val projectStore = PropertyStore(File(Project.projectFolder(project.name), "project.properties"))

    val defaultLocale = MutableStateFlow(LocaleIsoCode(projectStore[Project.PROP_DEFAULT_LOCALE] ?: "en"))
    val locales =
        MutableStateFlow(projectStore[Project.PROP_LOCALES]?.split(",")?.filter(String::isNotEmpty)?.map(::LocaleIsoCode)?.sortedBy { Locale[it].displayName() }
            ?: listOf(defaultLocale.value))
    val exportUrls =
        MutableStateFlow(Platform.values().associateWith { projectStore["${Project.PROP_EXPORT_URLS}.${it.lowercase}"] ?: "output/${it.lowercase}" })

    val strings = StringResourceViewModel(project, defaultLocale = defaultLocale::value)
    val plurals = PluralResourceViewModel(project, defaultLocale = defaultLocale::value)
    val arrays = ArrayResourceViewModel(project, defaultLocale = defaultLocale::value)

    init {
        GlobalScope.launch(Dispatchers.IO) {
            defaultLocale.collectLatest {
                projectStore[Project.PROP_DEFAULT_LOCALE] = it.value
                saveProject()
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            locales.collectLatest { locales ->
                projectStore[Project.PROP_LOCALES] = locales.sorted().joinToString(",") { it.value }
                saveProject()
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            exportUrls.collectLatest { urls ->
                urls.forEach { (platform, url) -> projectStore["${Project.PROP_EXPORT_URLS}.${platform.lowercase}"] = url }
                saveProject()
            }
        }
    }

    fun resourceViewModel(type: ResourceType) = when (type) {
        ResourceType.STRINGS -> strings
        ResourceType.PLURALS -> plurals
        ResourceType.ARRAYS -> arrays
    }

    fun findFirstInvalidResource(): Pair<ResourceType, ResourceId>? {
        val tab = selectedTab.value
        resourceViewModel(tab).findFirstInvalidResource()?.let { return tab to it }
        ResourceType.values().forEach { resourceType ->
            if (tab != resourceType) resourceViewModel(resourceType).findFirstInvalidResource()?.let { return resourceType to it }
        }
        return null
    }

    private val comparator = defaultLocale.map { defaultLocale ->
        Comparator<LocaleIsoCode> { o1, o2 ->
            when {
                o1 == defaultLocale -> -1
                o2 == defaultLocale -> 1
                else -> Locale[o1].displayName().compareTo(Locale[o2].displayName())
            }
        }
    }

    val displayedLocales = combine(comparator, locales, excludedLocales) { comparator, locales, excludedLocales ->
        locales.minus(excludedLocales).sortedWith(comparator)
    }

    fun addLocale(isoCode: LocaleIsoCode) {
        val localesToAdd = mutableListOf(isoCode)
        if (!isoCode.isBaseLanguage) {
            localesToAdd.add(Locale[isoCode].copy(region = null).isoCode)
        }

        locales.value = locales.value.plus(localesToAdd).sortedBy { Locale[it].displayName() }
        projectStore
    }

    fun deleteLocale(isoCode: LocaleIsoCode) {
        val localesToRemove = mutableSetOf(isoCode)
        // base language is required, so if that's removed, remove the dialects that depend on it
        if (isoCode.isBaseLanguage) {
            locales.value.forEach {
                if (it.value.startsWith(isoCode.value)) {
                    localesToRemove += it
                }
            }
        }
        locales.value = locales.value.minus(localesToRemove)
        localesToRemove.forEach { locale ->
            ResourceType.values().forEach { type -> resourceViewModel(type).deleteLocale(locale) }
        }
    }

    private fun saveProject() {
        runCatching { projectStore.store("Project settings for ${project.name}") }.onFailure {
            println("Failed to save settings with $it")
        }
    }
}
