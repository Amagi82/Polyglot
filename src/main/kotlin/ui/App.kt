package ui

import androidx.compose.runtime.*
import data.PolyglotDatabase
import sqldelight.Project
import ui.theme.PolyglotTheme
import utils.Settings

@Composable
fun App(db: PolyglotDatabase, project: Project?, onProjectChanged: (Project?) -> Unit) {
    var darkTheme by remember { mutableStateOf(Settings.isDarkTheme) }
    PolyglotTheme(darkTheme = darkTheme) {
        if (project == null) ProjectPicker(projectQueries = db.projectQueries, onProjectSelected = {
            onProjectChanged(it)
            Settings.currentProject = it.name
        })
        else ResourceManager(project, db = db,
            toggleDarkTheme = {
                Settings.isDarkTheme = !darkTheme
                darkTheme = !darkTheme
            })
    }
}
