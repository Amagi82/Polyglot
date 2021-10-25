package ui

import androidx.compose.runtime.*
import project.Project
import ui.project.ProjectPicker
import ui.resource.ResourceManager
import ui.resource.ResourceViewModel
import ui.core.theme.PolyglotTheme
import utils.Settings

@Composable
fun App(project: Project?, updateProject: (Project?) -> Unit) {
    var darkTheme by remember { mutableStateOf(Settings.isDarkTheme) }
    PolyglotTheme(darkTheme = darkTheme) {
        if (project == null) {
            ProjectPicker {
                updateProject(it)
                Settings.currentProject = it.name
            }
        } else {
            val vm = remember { ResourceViewModel(project) }
            ResourceManager(
                vm,
                toggleDarkTheme = {
                    Settings.isDarkTheme = !darkTheme
                    darkTheme = !darkTheme
                },
                updateProject = updateProject
            )
        }
    }
}
