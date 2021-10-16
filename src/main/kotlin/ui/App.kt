package ui

import androidx.compose.runtime.*
import project.Project
import ui.project.ProjectPicker
import ui.resource.ResourceManager
import ui.resource.ResourceViewModel
import ui.core.theme.PolyglotTheme
import utils.Settings

@Composable
fun App(projectState: MutableState<Project?>) {
    var darkTheme by remember { mutableStateOf(Settings.isDarkTheme) }
    PolyglotTheme(darkTheme = darkTheme) {
        projectState.value?.let {
            val project = remember(projectState) { mutableStateOf(it) }
            val vm = remember { ResourceViewModel(project.value) }
            ResourceManager(
                vm,
                toggleDarkTheme = {
                    Settings.isDarkTheme = !darkTheme
                    darkTheme = !darkTheme
                },
                updateProject = { projectState.value = it }
            )
        } ?: ProjectPicker {
            projectState.value = it
            Settings.currentProject = it.name
        }
    }
}
