package utils.extensions

import R
import utils.PropertyStore

inline fun <reified K, V> loadResource(resource: String, transformer: (Map.Entry<String, String>) -> Pair<K, V>): Map<K, V> =
    R::class.java.classLoader.getResource(resource)!!.openStream().use {
        PropertyStore(it).entries.associate(transformer)
    }
