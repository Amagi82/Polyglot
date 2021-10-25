package utils.extensions

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
