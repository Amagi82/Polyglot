package ui

import androidx.compose.runtime.*
import project.Project
import ui.theme.PolyglotTheme
import utils.Settings

@Composable
fun App(projectState: MutableState<Project?>) {
    var darkTheme by remember { mutableStateOf(Settings.isDarkTheme) }
    PolyglotTheme(darkTheme = darkTheme) {
        projectState.value?.let {
            val project = remember(projectState) { mutableStateOf(it) }
            ResourceManager(
                project,
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
