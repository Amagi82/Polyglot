package ui

import R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import data.PolyglotDatabase
import data.polyglotDatabase
import generators.ResourceGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import locales.LocaleIsoCode
import project.Platform
import project.Project
import project.ResourceType
import sqldelight.*
import ui.utils.onPressEnter
import java.awt.Desktop
import java.io.File
import java.util.*
import javax.swing.JFileChooser

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(project: Project, toggleDarkTheme: () -> Unit, updateProject: (Project?) -> Unit) {
    val db = remember { polyglotDatabase(project.name) }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val resourceList: List<Resource> by db.resourceQueries.selectAll().asFlow().mapToList().collectAsState(listOf())
    var showProjectSettings by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = {
                    IconButton(onClick = { updateProject(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close Project")
                    }
                },
                actions = {
                    IconButton(onClick = toggleDarkTheme) {
                        Icon(painterResource(R.drawable.darkMode), contentDescription = "Toggle dark theme")
                    }
                    IconButton(onClick = {}) {
                        Icon(painterResource(R.drawable.importExport), contentDescription = "Import or Export")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            ResourceGenerator.generateFiles(project, db)
                            val result = scaffoldState.snackbarHostState.showSnackbar(
                                message = "Generating outputs",
                                actionLabel = "Show",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                project.androidOutputUrl.let(::File).let { openFolder(it, scaffoldState) }
                                project.iosOutputUrl.let(::File).let { openFolder(it, scaffoldState) }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Build, contentDescription = "Build")
                    }

                    IconButton(onClick = {
                        showProjectSettings = false
                        showFilters = !showFilters
                    }) {
                        Icon(painterResource(R.drawable.filterList), contentDescription = "Filter")
                    }

                    IconButton(onClick = {
                        showFilters = false
                        showProjectSettings = !showProjectSettings
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    db.transaction {
                        var newId = "new"
                        var n = 0
                        while (resourceList.any { it.id == newId }) {
                            newId = "new$n"
                            n++
                        }
                        val newResource = Resource(id = newId, type = ResourceType.STRING, platforms = Platform.ALL)
                        db.resourceQueries.insert(newResource)
                        project.locales.forEach { locale ->
                            db.stringLocalizationsQueries.insert(
                                StringLocalizations(resId = newResource.id, locale = locale, text = "")
                            )
                        }
                    }
                }
            }) { Icon(Icons.Default.Add, contentDescription = "Add new resource") }
        }) {
        Row(Modifier.padding(it)) {
            LazyColumn(Modifier.padding(horizontal = 16.dp).weight(1f)) {
                items(resourceList) { res ->
                    DataRow(db = db, res = res, scaffoldState = scaffoldState)
                    Divider()
                }
            }

            if (showProjectSettings || showFilters) {
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
            }

            AnimatedVisibility(visible = showProjectSettings) {
                ProjectSettings(db = db, project = project, updateProject = updateProject, onClose = { showProjectSettings = false })
            }
            AnimatedVisibility(visible = showFilters) {
                Column(Modifier.width(240.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Filters", style = MaterialTheme.typography.h6)
                        IconButton(onClick = { showFilters = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close filters menu")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DataRow(db: PolyglotDatabase, res: Resource, scaffoldState: ScaffoldState) {
    val scope = rememberCoroutineScope()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        var id by remember { mutableStateOf(res.id) }
        var error by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current

        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = id,
                onValueChange = {
                    error = ""
                    id = it
                },
                modifier = Modifier.onPressEnter { focusManager.moveFocus(FocusDirection.Down); false }.onFocusChanged {
                    if (!it.hasFocus && res.id != id) {
                        scope.launch {
                            if (db.resourceQueries.exists(id).executeAsOne()) {
                                error = "id already exists"
                            } else {
                                db.resourceQueries.updateId(newId = id, oldId = res.id)
                            }
                        }
                    }
                },
                label = { Text("id") },
                isError = error.isNotEmpty()
            )
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colors.error)
            }
        }

        Column(Modifier.weight(1f)) {
            when (res.type) {
                ResourceType.STRING -> StringRows(res.id, db.stringLocalizationsQueries)
                ResourceType.PLURAL -> PluralRows(res.id, db.pluralLocalizationsQueries)
                ResourceType.ARRAY -> ArrayRows(res.id, db.arrayLocalizationsQueries)
            }
        }

        Platform.values().forEach { platform ->
            val isIncluded = platform in res.platforms
            IconButton(
                onClick = {
                    scope.launch {
                        db.resourceQueries.updatePlatforms((if (isIncluded) res.platforms.minus(platform) else res.platforms.plus(platform)), id = id)
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                if (isIncluded) {
                    Icon(painterResource(platform.iconId), contentDescription = platform.name)
                }
            }
        }
        IconButton(
            onClick = {
                scope.launch {
                    val resources = when (res.type) {
                        ResourceType.STRING -> db.stringLocalizationsQueries.selectAllWithId(id).executeAsList()
                        ResourceType.PLURAL -> db.pluralLocalizationsQueries.selectAllWithId(id).executeAsList()
                        ResourceType.ARRAY -> db.arrayLocalizationsQueries.selectAllWithId(id).executeAsList()
                    }
                    db.resourceQueries.delete(id)
                    if (scaffoldState.snackbarHostState.showSnackbar("Removed $id", actionLabel = "Undo") == SnackbarResult.ActionPerformed) {
                        db.transaction {
                            db.resourceQueries.insert(res)
                            when (res.type) {
                                ResourceType.STRING -> resources.forEach { db.stringLocalizationsQueries.insert(it as StringLocalizations) }
                                ResourceType.PLURAL -> resources.forEach { db.pluralLocalizationsQueries.insert(it as PluralLocalizations) }
                                ResourceType.ARRAY -> resources.forEach { db.arrayLocalizationsQueries.insert(it as ArrayLocalizations) }
                            }
                        }
                    }
                }
            }, modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Remove")
        }
    }
    Divider()
}

@Composable
fun StringRows(id: String, queries: StringLocalizationsQueries) {
    ResourceRows(queries.selectAllWithId(resId = id).asFlow().mapToList()) { scope, loc ->
        var newText by remember { mutableStateOf(loc.text) }
        OutlinedTextField(
            value = newText,
            onValueChange = { newText = it },
            modifier = Modifier.onFocusChanged {
                if (!it.hasFocus && loc.text != newText) {
                    scope.launch { queries.updateText(text = newText, resId = id, locale = loc.locale) }
                }
            },
            label = { Text("${Locale.forLanguageTag(loc.locale.value.take(2)).displayName} (${loc.locale.value})") })
    }
}

@Composable
fun PluralRows(id: String, queries: PluralLocalizationsQueries) {
//    ResourceRows(queries.selectAll(resId = id).asFlow().mapToList()) { scope, loc ->
//    }
}


@Composable
fun ArrayRows(id: String, queries: ArrayLocalizationsQueries) {
//    ResourceRows(queries.selectAll(resId = id).asFlow().mapToList()) { scope, loc ->
//    }
}

@Composable
fun <T> ResourceRows(localizationsFlow: Flow<List<T>>, rowContent: @Composable (CoroutineScope, T) -> Unit) {
    val scope = rememberCoroutineScope()
    val localizations by localizationsFlow.collectAsState(listOf())
    localizations.forEach {
        Row(
            Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowContent(scope, it)
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun openFolder(folder: File, scaffoldState: ScaffoldState) {
    try {
        Desktop.getDesktop().open(folder)
    } catch (e: Exception) {
        scaffoldState.snackbarHostState.showSnackbar("unable to open folder: $e")
    }
}
