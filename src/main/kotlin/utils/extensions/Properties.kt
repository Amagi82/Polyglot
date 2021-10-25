package utils.extensions

import locales.LocaleIsoCode
import project.*
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
fun <T> Properties.prop(save: () -> Unit, convert: String?.() -> T): ReadWriteProperty<Any, T> =
    object : ReadWriteProperty<Any, T> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T = getProperty(property.name).convert()
        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            setProperty(property.name, "$value")
            save()
        }
    }

fun <M : Metadata, T : Resource<M>> Map<LocaleIsoCode, Map<ResourceId, T>>.save(projectName: String, type: ResourceType) {
    forEach { (locale, resources) ->
        val file = Project.resourcesFile(projectName, type, locale)
        val props = Properties()
        resources.forEach { (k, v) ->
            when (v) {
                is Str -> props.setProperty(k.value, v.text)
                is Plural -> v.items.forEach { (quantity, text) -> props.setProperty("${k.value}.${quantity.label}", text) }
                is StringArray -> v.items.forEachIndexed { i, text -> props.setProperty("${k.value}.$i", text) }
            }
        }
        runCatching { props.store(file.outputStream(), "") }.onFailure {
            println("Failed to save localized ${type.title} resources with $it")
        }
    }
}
