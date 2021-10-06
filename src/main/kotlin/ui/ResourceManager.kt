package ui

import R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import data.PolyglotDatabase
import data.polyglotDatabase
import generators.ResourceGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.Platform
import project.Project
import project.ResourceType
import sqldelight.*
import ui.utils.onPressEnter
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(project: Project, toggleDarkTheme: () -> Unit, updateProject: (Project?) -> Unit) {
    val db = remember { polyglotDatabase(project.name) }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val resourceList: List<Resource> by db.resourceQueries.selectAll().asFlow().mapToList().collectAsState(listOf())
    var showProjectSettings by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    var excludedLocales by remember { mutableStateOf(setOf<LocaleIsoCode>()) }
    var excludedResourceIds by remember { mutableStateOf(setOf<String>()) }
    var excludedResourceTypes by remember { mutableStateOf(setOf<ResourceType>()) }

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
                            db.stringLocalizationsQueries.insert(StringLocalizations(resId = newResource.id, locale = locale, text = ""))
                        }
                    }
                }
            }) { Icon(Icons.Default.Add, contentDescription = "Add new resource") }
        }) { paddingValues ->
        Row(Modifier.padding(paddingValues)) {
            val state = rememberLazyListState()
            LazyColumn(Modifier.padding(start = 16.dp, end = 8.dp).weight(1f), state = state) {
                items(resourceList.filter { res -> res.id !in excludedResourceIds && res.type !in excludedResourceTypes }) { res ->
                    DataRow(db = db, res = res, displayedLocales = project.locales.filter { locale -> locale !in excludedLocales }, deleteResource = {
                        scope.launch {
                            excludedResourceIds = excludedResourceIds.plus(res.id)
                            val snackbarResult = scaffoldState.snackbarHostState.showSnackbar("Removed ${res.id}", actionLabel = "Undo")
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                excludedResourceIds = excludedResourceIds.minus(res.id)
                            } else {
                                db.resourceQueries.delete(res.id)
                            }
                        }
                    })
                    Divider()
                }
            }

            VerticalScrollbar(adapter = ScrollbarAdapter(state))

            if (showProjectSettings || showFilters) {
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
            }

            AnimatedVisibility(visible = showProjectSettings) {
                ProjectSettings(db = db, project = project, updateProject = updateProject, onClose = { showProjectSettings = false })
            }
            AnimatedVisibility(visible = showFilters) {
                Column(Modifier.width(300.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Filters", style = MaterialTheme.typography.h6)
                        IconButton(onClick = { showFilters = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close filters menu")
                        }
                    }

                    Text("Resource types", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.subtitle1)
                    ResourceType.values().forEach { resType ->
                        var isChecked by remember { mutableStateOf(true) }
                        Row(
                            modifier = Modifier.height(32.dp)
                                .background(
                                    color = if (isChecked) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    isChecked = !isChecked
                                    excludedResourceTypes = if (isChecked) excludedResourceTypes.minus(resType) else excludedResourceTypes.plus(resType)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isChecked) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 8.dp).size(18.dp),
                                    tint = MaterialTheme.colors.onSecondary
                                )
                            }
                            Text(
                                resType.name,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colors.onSecondary,
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }

                    Text("Locales", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.subtitle1)
                    project.locales.forEach { localeIsoCode ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Switch(checked = localeIsoCode !in excludedLocales,
                                onCheckedChange = { isChecked ->
                                    excludedLocales = if (isChecked) excludedLocales.minus(localeIsoCode) else excludedLocales.plus(localeIsoCode)
                                })
                            Text(Locale[localeIsoCode].displayName)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DataRow(db: PolyglotDatabase, res: Resource, displayedLocales: List<LocaleIsoCode>, deleteResource: () -> Unit) {
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
                modifier = Modifier.padding(vertical = 4.dp).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
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
                isError = error.isNotEmpty(),
                singleLine = true
            )
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colors.error)
            }
        }

        Column(Modifier.weight(1f)) {
            when (res.type) {
                ResourceType.STRING -> StringRows(res.id, displayedLocales, db.stringLocalizationsQueries)
                ResourceType.PLURAL -> PluralRows(res.id, displayedLocales, db.pluralLocalizationsQueries)
                ResourceType.ARRAY -> ArrayRows(res.id, displayedLocales, db.arrayLocalizationsQueries)
            }
        }

        Platform.values().forEach { platform ->
            val isIncluded = platform in res.platforms
            IconButton(onClick = {
                scope.launch {
                    db.resourceQueries.updatePlatforms((if (isIncluded) res.platforms.minus(platform) else res.platforms.plus(platform)), id = id)
                }
            }) {
                if (isIncluded) {
                    Icon(painterResource(platform.iconId), contentDescription = platform.name)
                }
            }
        }
        IconButton(onClick = deleteResource, modifier = Modifier.padding(start = 16.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove")
        }
    }
    Divider()
}

@Composable
fun StringRows(id: String, displayedLocales: List<LocaleIsoCode>, queries: StringLocalizationsQueries) {
    val focusManager = LocalFocusManager.current
    ResourceRows(displayedLocales) { scope, localeIsoCode ->
        var oldText = remember { queries.select(id, localeIsoCode).executeAsOneOrNull() }
        var newText by remember { mutableStateOf(oldText ?: "") }
        OutlinedTextField(
            value = newText,
            onValueChange = { newText = it },
            modifier = Modifier.onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                if (!it.hasFocus && oldText != newText) {
                    oldText = newText
                    scope.launch { queries.updateText(text = newText, resId = id, locale = localeIsoCode) }
                }
            },
            label = { Text(Locale[localeIsoCode].displayName) },
            singleLine = true
        )
    }
}

@Composable
fun PluralRows(id: String, displayedLocales: List<LocaleIsoCode>, queries: PluralLocalizationsQueries) {
//    ResourceRows(queries.selectAll(resId = id).asFlow().mapToList()) { scope, loc ->
//    }
}


@Composable
fun ArrayRows(id: String, displayedLocales: List<LocaleIsoCode>, queries: ArrayLocalizationsQueries) {
//    ResourceRows(queries.selectAll(resId = id).asFlow().mapToList()) { scope, loc ->
//    }
}

@Composable
fun ResourceRows(locales: List<LocaleIsoCode>, rowContent: @Composable (CoroutineScope, LocaleIsoCode) -> Unit) {
    val scope = rememberCoroutineScope()
    locales.forEach {
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
