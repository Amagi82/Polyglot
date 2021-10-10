package project

import locales.LocaleIsoCode
import java.util.*


typealias Localizations = Map<ResourceId, Localization>
typealias LocalizedResources = SortedMap<LocaleIsoCode, Localizations>

@JvmName("saveLocalizedResources")
fun LocalizedResources.save(projectName: String) {
    Project.localizedResourceFiles(projectName).filter { LocaleIsoCode(it.nameWithoutExtension) !in keys }.forEach { it.delete() }
    forEach { (locale, resources) ->
        val file = Project.localizedResourcesFile(projectName, locale)
        if (resources.isEmpty()) {
            file.delete()
            return@forEach
        }
        val props = Properties()
        resources.forEach { (k, v) ->
            when (v) {
                is Str -> props.setProperty(k.id, v.text)
                is Plural -> v.items.forEach { (quantity, text) -> props.setProperty("${k.id}.${quantity.name.lowercase()}", text) }
                is StringArray -> v.items.forEachIndexed { i, text -> props.setProperty("${k.id}.$i", text) }
            }
        }
        runCatching { props.store(file.outputStream(), "") }.onFailure {
            println("Failed to save localized resources with $it")
        }
    }
}
