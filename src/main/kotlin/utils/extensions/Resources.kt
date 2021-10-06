package utils.extensions

import R
import project.Quantity
import sqldelight.PluralLocalizations
import java.net.URL
import java.util.*

fun loadResource(resource: String): URL? = R::class.java.classLoader.getResource(resource)

inline fun <reified K, V> loadResource(resource: String, transformer: (String, String) -> Pair<K, V>): Map<K, V> =
    loadResource(resource)!!.openStream().use {
        val props = Properties().apply { load(it) }
        buildMap(props.size) {
            props.forEach { (k, v) ->
                val (key, value) = transformer(k as String, v as String)
                put(key, value)
            }
        }
    }

fun PluralLocalizations.quantity(quantity: Quantity) = when (quantity) {
    Quantity.ZERO -> zero
    Quantity.ONE -> one
    Quantity.TWO -> two
    Quantity.FEW -> few
    Quantity.MANY -> many
    Quantity.OTHER -> other
}
