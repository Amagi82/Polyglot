import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.json.Json
import ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Polyglot",
        icon = painterResource("language_black_24dp.svg")
    ) {
        App()
    }
}

val json = Json { prettyPrint = true }
