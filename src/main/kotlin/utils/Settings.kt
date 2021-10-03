package utils

import java.io.File
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Settings {
    private val file = File("polyglot", "settings.properties").apply(File::createNewFile)
    private val props = Properties().apply {
        load(file.inputStream())
    }

    var isDarkTheme: Boolean by prop { toBoolean() }
    var currentProject: String? by prop { this }

    @Suppress("UNCHECKED_CAST")
    private fun <T> prop(convert: String?.() -> T): ReadWriteProperty<Settings, T> =
        object : ReadWriteProperty<Settings, T> {
            override fun getValue(thisRef: Settings, property: KProperty<*>): T = props.getProperty(property.name).convert()
            override fun setValue(thisRef: Settings, property: KProperty<*>, value: T) {
                props.setProperty(property.name, "$value")
                runCatching { props.store(file.outputStream(), "Polyglot settings") }.onFailure {
                    println("Failed to save settings with $it")
                }
            }
        }
}
