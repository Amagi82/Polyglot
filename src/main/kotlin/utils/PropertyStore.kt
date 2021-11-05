package utils

import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Safe wrapper around Java Properties, which allows invalid types and then crashes at runtime
 */
@Suppress("UNCHECKED_CAST")
class PropertyStore() : MutableMap<String, String> {
    private val props = Properties()

    constructor(vararg properties: Pair<String, String>) : this() {
        putAll(properties)
    }

    constructor(properties: Map<String, String>) : this() {
        putAll(properties)
    }

    constructor(inputStream: InputStream) : this() {
        props.load(inputStream)
    }

    constructor(file: File) : this(file.inputStream())

    override val size: Int = props.size
    override fun containsKey(key: String): Boolean = props.containsKey(key)
    override fun containsValue(value: String): Boolean = props.containsValue(value)
    override fun get(key: String): String? = props.getProperty(key)
    override fun isEmpty(): Boolean = props.isEmpty
    override val entries get() = props.entries as MutableSet<MutableMap.MutableEntry<String, String>>
    override val keys: MutableSet<String> get() = props.keys as MutableSet<String>
    override val values: MutableCollection<String> get() = props.values as MutableCollection<String>
    override fun clear() = props.clear()
    override fun put(key: String, value: String): String? = props.setProperty(key, value) as String?
    override fun putAll(from: Map<out String, String>) = from.forEach { (k, v) -> props.setProperty(k, v) }
    override fun remove(key: String): String? = props.remove(key) as String?

    fun store(file: File, comment: String = "") {
        if (isEmpty()) file.delete()
        else props.store(file.outputStream(), comment)
    }

    fun <T> prop(save: () -> Unit, convert: String?.() -> T): ReadWriteProperty<Any, T> =
        object : ReadWriteProperty<Any, T> {
            override fun getValue(thisRef: Any, property: KProperty<*>): T = props.getProperty(property.name).convert()
            override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
                if (value == null) props.remove(property.name)
                else props.setProperty(property.name, "$value")
                save()
            }
        }
}
