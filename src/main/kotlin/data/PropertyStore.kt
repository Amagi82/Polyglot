package data

import java.io.InputStream
import java.util.*

/**
 * Safe wrapper around Java Properties, which allows invalid types and then crashes at runtime
 */
@Suppress("UNCHECKED_CAST")
open class PropertyStore(inputStream: InputStream) : MutableMap<String, String> {
    protected val props = OrderedProperties()

    init {
        props.load(inputStream)
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

    class OrderedProperties : Properties() {
        override val entries get() = super.entries.toSortedSet { o1, o2 -> (o1.key as String).compareTo(o2.key as String) }
        override val keys: MutableSet<Any> get() = super.keys.toSortedSet { o1, o2 -> (o1 as String).compareTo(o2 as String) }
    }
}
