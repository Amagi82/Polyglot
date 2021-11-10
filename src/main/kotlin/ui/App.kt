package ui

import androidx.compose.runtime.*
import ui.project.ProjectPicker
import ui.resource.ResourceManager
import ui.resource.ResourceViewModel
import ui.core.theme.PolyglotTheme
import utils.Settings

@Composable
fun App() {
    var darkTheme by remember { mutableStateOf(Settings.isDarkTheme) }
    PolyglotTheme(darkTheme = darkTheme) {
        var savedProject by remember { mutableStateOf(Settings.currentProject) }
        val currentProject = savedProject
        Settings.currentProject = currentProject
        if (currentProject == null) {
            ProjectPicker { savedProject = it }
        } else {
            val vm by remember { mutableStateOf(ResourceViewModel(currentProject)) }
            ResourceManager(
                vm,
                toggleDarkTheme = {
                    Settings.isDarkTheme = !darkTheme
                    darkTheme = !darkTheme
                },
                closeProject = { savedProject = null }
            )
        }
    }
}
