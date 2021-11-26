package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Safe wrapper around Java Properties, which allows invalid types and then crashes at runtime
 */
@Suppress("UNCHECKED_CAST")
open class PropertyStore(private val file: File) : MutableMap<String, String> {
    protected val props = OrderedProperties()

    init {
        props.load(file.apply(File::createNewFile).inputStream())
    }

    override val size: Int = props.size
    override fun containsKey(key: String): Boolean = props.containsKey(key)
    override fun containsValue(value: String): Boolean = props.containsValue(value)
    override fun get(key: String): String? = props.getProperty(key)
    override fun isEmpty(): Boolean = props.isEmpty
    override val entries get() = props.entries as SortedSet<MutableMap.MutableEntry<String, String>>
    override val keys: MutableSet<String> get() = props.keys as SortedSet<String>
    override val values: MutableCollection<String> get() = props.values as MutableCollection<String>
    override fun clear() = props.clear()
    override fun put(key: String, value: String): String? = if (value.isEmpty()) remove(key) else props.setProperty(key, value) as String?
    override fun putAll(from: Map<out String, String>) = from.forEach { (k, v) -> put(k, v) }
    override fun remove(key: String): String? = props.remove(key) as String?

    protected fun store(comment: String = "", onFailure: (Exception) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (isEmpty()) file.delete()
                else props.store(file.outputStream(), comment)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    protected open fun save() {
        store() { println("Error saving: $it") }
    }

    protected fun <T> prop(
        getValue: (String?) -> T,
        putValue: (T) -> String = { "$it" }
    ): ReadWriteProperty<Any, T> =
        object : ReadWriteProperty<Any, T> {
            private var cached: T? = null

            override fun getValue(thisRef: Any, property: KProperty<*>): T = cached ?: getValue(props.getProperty(property.name)).also { cached = it }
            override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
                cached = value
                put(property.name, putValue(value))
                save()
            }
        }

    protected fun <K, V> map(
        getValues: Map<String, String>.(propertyName: String) -> Map<K, V>,
        putValues: (propertyName: String, K, V) -> Unit
    ): ReadWriteProperty<Any, Map<K, V>> =
        object : ReadWriteProperty<Any, Map<K, V>> {
            private var cached: Map<K, V>? = null

            override fun getValue(thisRef: Any, property: KProperty<*>): Map<K, V> = cached ?: getValues(property.name).also { cached = it }
            override fun setValue(thisRef: Any, property: KProperty<*>, value: Map<K, V>) {
                cached = value
                value.forEach { (k, v) ->
                    putValues(property.name, k, v)
                }
                save()
            }
        }

    protected fun <T> mutableStateFlowOf(propName: String, getter: (String?) -> T, setter: (T) -> String) =
        MutableStateFlow(getter(get(propName))).also { stateFlow ->
            GlobalScope.launch {
                stateFlow.collectLatest { put(propName, setter(it)) }
            }
        }

    protected fun <T> mutableStateFlowOf(value: T, update: (T) -> Unit) = MutableStateFlow(value).also { stateFlow ->
        GlobalScope.launch {
            stateFlow.collectLatest { update(it) }
        }
    }

    class OrderedProperties : Properties() {
        override val entries get() = super.entries.toSortedSet { o1, o2 -> (o1.key as String).compareTo(o2.key as String) }
        override val keys: MutableSet<Any> get() = super.keys.toSortedSet { o1, o2 -> (o1 as String).compareTo(o2 as String) }
    }
}