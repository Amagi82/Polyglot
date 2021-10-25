package utils

import project.Project
import utils.extensions.prop
import java.io.File
import java.util.*

object Settings {
    private val file = File(Project.projectsFolder, "settings.properties").apply(File::createNewFile)
    private val props = Properties().apply { load(file.inputStream()) }

    private fun save() {
        runCatching { props.store(file.outputStream(), "Polyglot settings") }.onFailure {
            println("Failed to save settings with $it")
        }
    }

    var isDarkTheme: Boolean by props.prop(::save) { toBoolean() }
    var currentProject: String? by props.prop(::save) { this }
}
