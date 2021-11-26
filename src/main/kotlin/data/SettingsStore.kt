package data

import kotlinx.coroutines.flow.MutableStateFlow
import project.Project
import java.io.File

object SettingsStore : PropertyStore(File(Project.projectsFolder, "settings.properties")) {
    val isDarkTheme: MutableStateFlow<Boolean> = mutableStateFlowOf("isDarkTheme", getter = String?::toBoolean)
    val currentProject: MutableStateFlow<String?> = mutableStateFlowOf("currentProject", getter = { it }, setter = { it.orEmpty() })

    override fun save() {
        store("Polyglot settings") { println("Failed to save settings with $it") }
    }
}