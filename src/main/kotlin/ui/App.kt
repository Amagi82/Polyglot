package ui

import androidx.compose.runtime.*
import data.PolyglotDatabase
import data.polyglotDatabase
import project.Project
import ui.theme.PolyglotTheme
import utils.Settings

@Composable
fun App(project: Project?, updateProject: (Project?) -> Unit) {
    var darkTheme by remember { mutableStateOf(Settings.isDarkTheme) }
    PolyglotTheme(darkTheme = darkTheme) {
        if (project == null) ProjectPicker {
            updateProject(it)
            Settings.currentProject = it.name
        }
        else ResourceManager(
            project,
            toggleDarkTheme = {
                Settings.isDarkTheme = !darkTheme
                darkTheme = !darkTheme
            },
            updateProject = updateProject
        )
    }
}
