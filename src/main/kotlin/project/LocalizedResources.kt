package project

import locales.LocaleIsoCode
import java.util.*

fun <M : Metadata, T : Resource<M>> Map<LocaleIsoCode, Map<ResourceId, T>>.save(projectName: String, type: ResourceType) {
    forEach { (locale, resources) ->
        val file = Project.localizedResourcesFile(projectName, type, locale)
        val props = Properties()
        resources.forEach { (k, v) ->
            when (v) {
                is Str -> props.setProperty(k.value, v.text)
                is Plural -> v.items.forEach { (quantity, text) -> props.setProperty("${k.value}.${quantity.name.lowercase()}", text) }
                is StringArray -> v.items.forEachIndexed { i, text -> props.setProperty("${k.value}.$i", text) }
            }
        }
        runCatching { props.store(file.outputStream(), "") }.onFailure {
            println("Failed to save localized ${type.name.lowercase()} resources with $it")
        }
    }
}
