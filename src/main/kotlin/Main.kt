import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import data.polyglotDatabase
import sqldelight.Project
import ui.App
import utils.Settings

fun main() = application {
    // Look at https://github.com/adrielcafe/lyricist for localization
    val db = polyglotDatabase

    var project: Project? by remember { mutableStateOf(Settings.currentProject?.let { db.projectQueries.select(it).executeAsOneOrNull() }) }

    val state = rememberWindowState()
    state.size = if (project == null) WindowSize(400.dp, 600.dp) else WindowSize(2560.dp, 1440.dp)
    state.position = WindowPosition(Alignment.Center)

//    Locale.getAvailableLocales().sortedBy { it.language }.forEach {
//        println("availableLocale: language: ${it.language}, country: ${it.country}, displayLanguage: ${it.displayLanguage}, displayCountry: ${it.displayCountry}, script: ${it.script}, displayScript: ${it.displayScript}, variant: ${it.variant}")
//    }
//    Locale.getISOCountries().forEach {
//        println("isoCountry: $it")
//    }

    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = "Polyglot",
        icon = painterResource(R.drawable.language),
        undecorated = false
    ) {
        App(db, project, onProjectChanged = { project = it })
    }
}
