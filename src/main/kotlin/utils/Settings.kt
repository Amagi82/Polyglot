package utils

import project.Project
import java.io.File

object Settings {
    private val file = File(Project.projectsFolder, "settings.properties").apply(File::createNewFile)
    private val props = PropertyStore(file)

    private fun save() {
        runCatching { props.store("Polyglot settings") }.onFailure {
            println("Failed to save settings with $it")
        }
    }

    var isDarkTheme: Boolean by props.prop(::save) { toBoolean() }
    var currentProject: String? by props.prop(::save) { this }
}
