import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import project.Project
import ui.App
import ui.ProjectPicker
import ui.theme.PolyglotTheme
import utils.Settings
import java.io.FileReader
import javax.script.ScriptEngineManager

object Config {
    var isDarkTheme = false
    var currentProject: String? = null
}

fun main() = application {
    with(ScriptEngineManager().getEngineByExtension("kts")) {
        eval(FileReader("polyglot/config.kts"))
    }

    // Look at https://github.com/adrielcafe/lyricist for localization

    println("Config.isDarkTheme: ${Config.isDarkTheme}, currentProject: ${Config.currentProject}")

    val scope = rememberCoroutineScope()
    var project by remember { mutableStateOf(Project.current) }
    val state = rememberWindowState()
    var darkTheme by remember { mutableStateOf(Settings.isDarkTheme) }

    if (project == null) {
        state.size = WindowSize(400.dp, 600.dp)
        state.position = WindowPosition(Alignment.Center)
    } else {
        state.size = WindowSize(2560.dp, 1440.dp)
        state.position = WindowPosition(Alignment.Center)
    }

    fun saveAndExit(){
        scope.launch {
            project?.cache()
            Settings.save()
            exitApplication()
        }
    }

    Window(
        onCloseRequest = ::saveAndExit,
        state = state,
        title = "Polyglot",
        icon = painterResource(R.drawable.language)
    ) {
        PolyglotTheme(darkTheme = darkTheme) {
            val currentProject = project
            if (currentProject == null) ProjectPicker(onProjectSelected = {
                project = it
                Settings.currentProject = it.name
            })
            else {
                MenuBar {
                    Menu("File") {
                        if (project != null) {
                            Item("Close project") {
                                scope.launch {
                                    project?.cache()
                                    project = null
                                    Settings.currentProject = null
                                }
                            }
                            Item("Exit", onClick = ::saveAndExit)
                        }
                    }
                }
                App(currentProject,
                    updateResources = {
                        project = currentProject.copy(resources = it)
                        scope.launch { project?.cache() }
                    },
                    toggleDarkTheme = {
                        Settings.isDarkTheme = !darkTheme
                        darkTheme = !darkTheme
                    })
            }
        }
    }
}

val json = Json { prettyPrint = true }
