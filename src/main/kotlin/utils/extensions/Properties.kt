package utils.extensions

import java.io.File
import java.io.InputStream
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

fun Properties(file: File) = Properties(file.inputStream())

fun Properties(stream: InputStream) = Properties().apply { load(stream) }

fun Properties.store(file: File, comment: String = "") = store(file.outputStream(), comment)
