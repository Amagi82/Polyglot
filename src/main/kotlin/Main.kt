import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import project.Project
import ui.App
import utils.Settings

fun main() = application {
    // Look at https://github.com/adrielcafe/lyricist for localization

    val project = remember { mutableStateOf(Settings.currentProject?.let(Project::load)) }
    project.value?.save()

    val state = rememberWindowState()
    state.size = if (project.value == null) DpSize(400.dp, 600.dp) else DpSize(2560.dp, 1440.dp)
    state.position = WindowPosition(Alignment.Center)

    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = "Polyglot",
        icon = painterResource(R.drawable.language),
        undecorated = false
    ) {
        App(project)
    }
}
