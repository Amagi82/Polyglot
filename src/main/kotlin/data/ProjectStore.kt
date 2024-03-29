package data

import kotlinx.coroutines.flow.MutableStateFlow
import locales.Locale
import locales.LocaleIsoCode
import project.Platform
import project.Project
import java.io.File

/**
 * @property defaultLocale: The current locale set as default, which will be used as backup if no suitable translation is found.
 * Android and iOS handle this differently:
 * With Android, the base values folder gets the default strings, and values-en, values-de, etc get the localized translations
 * With iOS, there is no base folder, all localizations are placed in their respective folders, e.g. en.proj, es.proj, de.proj
 * @property locales: Set of locales this project supports
 * @property exportUrls: Set this to your project folders and Polyglot will generate files directly to that folder
 * */
class ProjectStore(val name: String) : FilePropertyStore(File(Project.projectFolder(name), "project.properties")) {
    val defaultLocale: MutableStateFlow<LocaleIsoCode> = mutableStateFlowOf("defaultLocale", getter = { LocaleIsoCode(it ?: "en") }) { it.value }

    val locales: MutableStateFlow<Set<LocaleIsoCode>> = mutableStateFlowOf("locales",
        getter = { value -> value?.split(",")?.map(::LocaleIsoCode)?.toSet() ?: setOf(defaultLocale.value) },
        setter = { value -> value.sortedBy { Locale[it].displayName() }.joinToString(",", transform = LocaleIsoCode::value) })

    val exportUrls: MutableStateFlow<Map<Platform, String>> =
        mutableStateFlowOf(Platform.values().associateWith { this["$PROP_EXPORT_URLS.${it.lowercase}"] ?: it.defaultOutputUrl }) {
            it.forEach { (platform, url) ->
                put("$PROP_EXPORT_URLS.${platform.lowercase}", url)
            }
        }

    override fun save() {
        store("Project settings for $name") { println("Failed to save project settings with $it") }
    }

    companion object {
        private const val PROP_EXPORT_URLS = "exportUrls"
    }
}
