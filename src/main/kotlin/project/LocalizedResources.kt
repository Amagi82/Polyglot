package project

import locales.LocaleIsoCode
import java.util.*


typealias Resources = Map<ResourceId, Resource>
typealias LocalizedResources = Map<LocaleIsoCode, Resources>

@JvmName("saveLocalizedResources")
fun LocalizedResources.save(projectName: String) {
    forEach { (locale, resources) ->
        val file = Project.localizedResourcesFile(projectName, locale)
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
