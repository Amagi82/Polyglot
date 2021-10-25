package utils.extensions

import R

inline fun <reified K, V> loadResource(resource: String, transformer: (String, String) -> Pair<K, V>): Map<K, V> =
    R::class.java.classLoader.getResource(resource)!!.openStream().use {
        Properties(it).entries.associate { (k, v) -> transformer(k as String, v as String) }
    }
