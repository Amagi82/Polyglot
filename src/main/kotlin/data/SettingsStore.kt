package data

import project.Project
import java.io.File

object SettingsStore : PropertyStore(File(Project.projectsFolder, "settings.properties")) {
    var isDarkTheme: Boolean by prop(getValue = String?::toBoolean)
    var currentProject: String? by prop(getValue = { it })

    override fun save() {
        store("Polyglot settings") { println("Failed to save settings with $it") }
    }
}
