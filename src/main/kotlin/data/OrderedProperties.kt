package data

import java.util.*

class OrderedProperties : Properties() {
    override val entries get() = super.entries.toSortedSet { o1, o2 -> (o1.key as String).compareTo(o2.key as String) }
    override val keys: MutableSet<Any> get() = super.keys.toSortedSet { o1, o2 -> (o1 as String).compareTo(o2 as String) }
}
