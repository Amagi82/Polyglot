import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import locales.Locale
import project.Project
import ui.App
import utils.Settings

fun main() = application {
    // Look at https://github.com/adrielcafe/lyricist for localization

    var project: Project? by remember { mutableStateOf(Settings.currentProject?.let(Project::load)) }
    project?.defaultLocale?.let { Locale.default = it }

    val state = rememberWindowState()
    state.size = if (project == null) WindowSize(400.dp, 600.dp) else WindowSize(2560.dp, 1440.dp)
    state.position = WindowPosition(Alignment.Center)

    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = "Polyglot",
        icon = painterResource(R.drawable.language),
        undecorated = false
    ) {
        App(project, updateProject = {
            project = it
            it?.save()
        })
    }
}
