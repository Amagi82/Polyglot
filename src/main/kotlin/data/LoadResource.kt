package data

import R
import java.io.File

inline fun <reified K, V> loadResource(resource: String, transformer: (Map.Entry<String, String>) -> Pair<K, V>): Map<K, V> =
    File(R::class.java.classLoader.getResource(resource)!!.toURI()).let(::PropertyStore).entries.associate(transformer)
