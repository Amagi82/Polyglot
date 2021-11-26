package ui

import androidx.compose.runtime.*
import data.ProjectStore
import data.SettingsStore
import ui.project.ProjectPicker
import ui.resource.ResourceManager
import ui.resource.ResourceViewModel
import ui.core.theme.PolyglotTheme

@Composable
fun App() {
    var darkTheme by remember { mutableStateOf(SettingsStore.isDarkTheme) }
    PolyglotTheme(darkTheme = darkTheme) {
        var savedProject by remember { mutableStateOf(SettingsStore.currentProject) }
        val currentProject = savedProject
        SettingsStore.currentProject = currentProject
        if (currentProject == null) {
            ProjectPicker { savedProject = it }
        } else {
            val vm by remember { mutableStateOf(ResourceViewModel(ProjectStore(currentProject))) }
            ResourceManager(
                vm,
                toggleDarkTheme = {
                    SettingsStore.isDarkTheme = !darkTheme
                    darkTheme = !darkTheme
                },
                closeProject = { savedProject = null }
            )
        }
    }
}
