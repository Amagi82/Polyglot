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

    val excludedLocales = MutableStateFlow(setOf<LocaleIsoCode>())
    val selectedTab = MutableStateFlow(ResourceType.STRINGS)

    val strings = StringResourceViewModel(this@ResourceViewModel.project)
    val plurals = PluralResourceViewModel(this@ResourceViewModel.project)
    val arrays = ArrayResourceViewModel(this@ResourceViewModel.project)

    private val comparator = this.project.map { project ->
        Comparator<LocaleIsoCode> { o1, o2 ->
            when {
                o1 == project.defaultLocale -> -1
                o2 == project.defaultLocale -> 1
                else -> Locale[o1].displayName().compareTo(Locale[o2].displayName())
            }
        }
    }

    val displayedLocales = combine(comparator, this@ResourceViewModel.project, excludedLocales) { comparator, project, excludedLocales ->
        project.locales.minus(excludedLocales).sortedWith(comparator)
    }

    init {
        GlobalScope.launch(Dispatchers.IO) { this@ResourceViewModel.project.collectLatest { it.save() } }
    }

    fun addLocale(isoCode: LocaleIsoCode) {
        val localesToAdd = mutableListOf(isoCode)
        if (!isoCode.isBaseLanguage) {
            localesToAdd.add(Locale[isoCode].copy(region = null).isoCode)
        }
        val current = project.value
        project.value = current.copy(locales = current.locales.plus(localesToAdd).sortedBy { Locale[it].displayName() })
    }

    fun deleteLocale(isoCode: LocaleIsoCode) {
        val localesToRemove = mutableSetOf(isoCode)
        // base language is required, so if that's removed, remove the dialects that depend on it
        val current = project.value
        if (isoCode.isBaseLanguage) {
            current.locales.forEach {
                if (it.value.startsWith(isoCode.value)) {
                    localesToRemove += it
                }
            }
        }
        project.value = current.copy(locales = current.locales.minus(localesToRemove))
        localesToRemove.forEach {
            ResourceType.values().forEach { type ->
                Project.localizedResourcesFile(project.value.name, type, it).delete()
            }
        }
    }
}
