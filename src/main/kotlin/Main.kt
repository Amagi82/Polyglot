import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.App

fun main() = application {
    // Look at https://github.com/adrielcafe/lyricist for localization
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(size = DpSize(2560.dp, 1440.dp)),
        title = "Polyglot",
        icon = painterResource(R.drawable.language)
    ) {
        App()
    }
}
