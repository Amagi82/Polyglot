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
    val darkTheme by SettingsStore.isDarkTheme.collectAsState()
    PolyglotTheme(darkTheme = darkTheme) {
        val savedProject by SettingsStore.currentProject.collectAsState()
        savedProject.let { currentProject ->
            if (currentProject == null) {
                ProjectPicker { SettingsStore.currentProject.value = it }
            } else {
                val vm by remember { mutableStateOf(ResourceViewModel(ProjectStore(currentProject))) }
                ResourceManager(
                    vm,
                    toggleDarkTheme = { SettingsStore.isDarkTheme.value = !darkTheme },
                    closeProject = { SettingsStore.currentProject.value = null }
                )
            }
        }
    }
}
