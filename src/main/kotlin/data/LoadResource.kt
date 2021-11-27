package data

import R

inline fun <reified K, V> loadResource(resource: String, transformer: (Map.Entry<String, String>) -> Pair<K, V>): Map<K, V> =
    R::class.java.classLoader.getResourceAsStream(resource)!!.let(::PropertyStore).entries.associate(transformer)
