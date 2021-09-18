package utils

import utils.extensions.loadResource
import java.util.*

object Localization {
    private val props = loadResource("localization/${System.getProperty("user.language")}}.properties")?.let { Properties().apply { load(it.openStream()) } }
    private val defaultProps = loadResource("localization/en.properties")!!.let { Properties().apply { load(it.openStream()) } }

    init {
        println("System properties")
        System.getProperties().forEach { k, v ->
            println("$k: $v")
        }
    }
}

fun String.localized() = Localization
