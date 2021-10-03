package utils.extensions

import R
import project.Platform
import project.Quantity
import sqldelight.PluralLocalizations
import sqldelight.Project
import java.net.URL

fun loadResource(resource: String): URL? = R::class.java.classLoader.getResource(resource)

//@OptIn(ExperimentalSerializationApi::class)
//inline fun <reified T> loadResource(resource: String) = loadResource(resource)!!.openStream().use {
//    Json.decodeFromStream<T>(it)
//}

fun PluralLocalizations.quantity(quantity: Quantity) = when (quantity) {
    Quantity.ZERO -> zero
    Quantity.ONE -> one
    Quantity.TWO -> two
    Quantity.FEW -> few
    Quantity.MANY -> many
    Quantity.OTHER -> other
}
