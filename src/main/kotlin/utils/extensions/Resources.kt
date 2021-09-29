package utils.extensions

import R
import java.net.URL

fun loadResource(resource: String): URL? = R::class.java.classLoader.getResource(resource)

//@OptIn(ExperimentalSerializationApi::class)
//inline fun <reified T> loadResource(resource: String) = loadResource(resource)!!.openStream().use {
//    Json.decodeFromStream<T>(it)
//}
