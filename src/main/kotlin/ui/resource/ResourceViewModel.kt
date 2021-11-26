package ui.resource

import data.ProjectStore
import generators.ProjectData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import locales.Locale
import locales.LocaleIsoCode
import project.Platform
import project.ResourceId
import project.ResourceType

class ResourceViewModel(projectStore: ProjectStore) {
    val excludedLocales = MutableStateFlow(setOf<LocaleIsoCode>())
    val selectedTab = MutableStateFlow(ResourceType.STRINGS)
    val isMultiSelectEnabled = MutableStateFlow(false)

    val projectName = projectStore.name
    val defaultLocale = projectStore.defaultLocale
    val locales = projectStore.locales
    val exportUrls = projectStore.exportUrls

    val strings = StringResourceViewModel(projectStore)
    val plurals = PluralResourceViewModel(projectStore)
    val arrays = ArrayResourceViewModel(projectStore)

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
        if (isoCode.isBaseLanguage) {
            locales.value += isoCode
        } else {
            locales.value += listOf(Locale[isoCode].copy(region = null).isoCode, isoCode)
        }
    }

    fun deleteLocale(isoCode: LocaleIsoCode) {
        // base language is required, so if that's removed, remove the dialects that depend on it
        val localesToRemove = if (isoCode.isBaseLanguage) listOf(isoCode) else locales.value.filter { it.value.startsWith(isoCode.value) }
        locales.value -= localesToRemove
        localesToRemove.forEach { locale ->
            ResourceType.values().forEach { type -> resourceViewModel(type).deleteLocale(locale) }
        }
    }

    fun projectData(platform: Platform): ProjectData = ProjectData(
        defaultLocale = defaultLocale.value,
        locales = locales.value,
        exportUrl = exportUrls.value[platform] ?: platform.defaultOutputUrl,
        strings = strings.resourceData(),
        plurals = plurals.resourceData(),
        arrays = arrays.resourceData()
    )
}
