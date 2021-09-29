package ui

import R
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import data.PolyglotDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.Platform
import project.ResourceType
import sqldelight.*


@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun App(project: Project, db: PolyglotDatabase, toggleDarkTheme: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBackdropScaffoldState(BackdropValue.Concealed)
    val resourceList: List<Resource> by db.resourceQueries.selectAll(project.name).asFlow().mapToList().collectAsState(listOf())

    BackdropScaffold(
        appBar = {
            TopAppBar(
                title = { Text("Polyglot - ${project.name}") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { if (scaffoldState.isRevealed) scaffoldState.conceal() else scaffoldState.reveal() } }) {
                        Icon(painterResource(R.drawable.language), contentDescription = "Edit locales")
                    }
                },
                actions = {
                    IconButton(onClick = toggleDarkTheme) {
                        Icon(painterResource(R.drawable.darkMode), contentDescription = "Toggle dark theme")
                    }
                    IconButton(onClick = {}) {
                        Icon(painterResource(R.drawable.importExport), contentDescription = "Import or Export")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Build, contentDescription = "Build")
                    }
                },
                elevation = 0.dp,
                backgroundColor = Color.Transparent
            )
        },
        backLayerContent = {
            Row(Modifier.padding(16.dp).background(MaterialTheme.colors.primary)) {
                TextField(
                    value = project.name,
                    onValueChange = {},
                    label = { Text("Project name") },
                    readOnly = true,
                    singleLine = true
                )
                project.locales.forEach {
                    Row {
                        Text(it.value)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    db.transaction {
                                        db.projectQueries.updateLocales(project.locales.minus(it), name = project.name)
                                        db.arrayLocalizationsQueries.deleteLocale(project = project.name, lang = it)
                                        db.pluralLocalizationsQueries.deleteLocale(project = project.name, lang = it)
                                        db.stringLocalizationsQueries.deleteLocale(project = project.name, lang = it)
                                    }
                                }
                            },
                            enabled = it.value != Locale.default
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove ${it.value}")
                        }
                    }
                }
                var isDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { isDropdownExpanded = true }) {
                        Text("Add")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Add Locale")
                    }

                    DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                        listOf("en", "es", "fr", "de").map { LocaleIsoCode(it) }.filter { it !in project.locales }.forEach { locale ->
                            DropdownMenuItem(onClick = {
                                isDropdownExpanded = false
                                scope.launch {
                                    db.transaction {
                                        db.projectQueries.updateLocales(project.locales.plus(locale), name = project.name)
                                        resourceList.forEach {
                                            when (it.type) {
                                                ResourceType.STRING -> db.stringLocalizationsQueries.insert(
                                                    StringLocalizations(
                                                        id = it.id,
                                                        project = it.project,
                                                        lang = locale,
                                                        text = ""
                                                    )
                                                )
                                                ResourceType.PLURAL -> db.pluralLocalizationsQueries.insert(
                                                    PluralLocalizations(
                                                        id = it.id,
                                                        project = it.project,
                                                        lang = locale,
                                                        zero = null,
                                                        one = "",
                                                        two = null,
                                                        few = null,
                                                        many = null,
                                                        other = ""
                                                    )
                                                )
                                                ResourceType.ARRAY -> db.arrayLocalizationsQueries.insert(
                                                    ArrayLocalizations(
                                                        id = it.id,
                                                        project = it.project,
                                                        lang = locale,
                                                        array = listOf()
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text(locale.value)
                            }
                        }
                    }
                }
            }
        },
        frontLayerContent = {
            LazyColumn(Modifier.background(MaterialTheme.colors.surface).padding(horizontal = 16.dp).fillMaxSize()) {
                items(resourceList) { res ->
                    DataRow(db = db, res = res, scaffoldState = scaffoldState)
                    Divider()
                }
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(onClick = {
                    scope.launch {
                        db.transaction {
                            var newId = "new"
                            var n = 0
                            while (resourceList.any { it.id == newId }) {
                                newId = "new$n"
                                n++
                            }
                            val newResource = Resource(id = newId, project = project.name, type = ResourceType.STRING, platforms = Platform.ALL)
                            db.resourceQueries.insert(newResource)
                            project.locales.forEach { locale ->
                                db.stringLocalizationsQueries.insert(
                                    StringLocalizations(
                                        id = newResource.id,
                                        project = newResource.project,
                                        lang = locale,
                                        text = ""
                                    )
                                )
                            }
                        }
                    }
                }, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add new resource")
                }
            }
        },
        scaffoldState = scaffoldState
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DataRow(db: PolyglotDatabase, res: Resource, scaffoldState: BackdropScaffoldState) {
    val scope = rememberCoroutineScope()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        var id by remember { mutableStateOf(res.id) }
        var error by remember { mutableStateOf("") }

        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = id,
                onValueChange = {
                    error = ""
                    id = it
                },
                modifier = Modifier.onFocusChanged {
                    if (!it.hasFocus && res.id != id) {
                        scope.launch {
                            if (db.resourceQueries.exists(res.id, res.project).executeAsOne()) {
                                error = "id already exists"
                            } else {
                                db.resourceQueries.updateId(newId = id, oldId = res.id, project = res.project)
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
                ResourceType.STRING -> StringRows(res, db.stringLocalizationsQueries)
                ResourceType.PLURAL -> PluralRows(res, db.pluralLocalizationsQueries)
                ResourceType.ARRAY -> ArrayRows(res, db.arrayLocalizationsQueries)
            }
        }

        Platform.values().forEach { platform ->
            val isIncluded = platform in res.platforms
            IconButton(
                onClick = {
                    scope.launch {
                        db.resourceQueries.updatePlatforms(
                            platforms = (if (isIncluded) res.platforms.minus(platform) else res.platforms.plus(platform)),
                            id = res.id,
                            project = res.project
                        )
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
                        ResourceType.STRING -> db.stringLocalizationsQueries.selectAll(res.id, res.project).executeAsList()
                        ResourceType.PLURAL -> db.pluralLocalizationsQueries.selectAll(res.id, res.project).executeAsList()
                        ResourceType.ARRAY -> db.arrayLocalizationsQueries.selectAll(res.id, res.project).executeAsList()
                    }
                    db.resourceQueries.delete(res.id, res.project)
                    if (scaffoldState.snackbarHostState.showSnackbar("Removed ${res.id}", actionLabel = "Undo") == SnackbarResult.ActionPerformed) {
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
fun StringRows(res: Resource, queries: StringLocalizationsQueries) {
    ResourceRows(queries.selectAll(id = res.id, project = res.project).asFlow().mapToList()) { scope, loc ->
        var newLocale by remember { mutableStateOf(loc.lang.value) }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        Box {
            OutlinedButton(onClick = { isDropdownExpanded = true }) {
                Text(newLocale)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Locale")
            }

            DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                listOf("en", "es", "fr", "de").forEach {
                    DropdownMenuItem(onClick = {
                        isDropdownExpanded = false
                        newLocale = it
                    }) {
                        Text(it)
                    }
                }
            }
        }

        var newText by remember { mutableStateOf(loc.text) }
        OutlinedTextField(
            value = newText,
            onValueChange = { newText = it },
            modifier = Modifier.onFocusChanged {
                if (!it.hasFocus && loc.text != newText) {
                    scope.launch { queries.updateText(text = newText, id = res.id, project = res.project, lang = loc.lang) }
                }
            },
            label = { Text("Text") })
    }
}

@Composable
fun PluralRows(res: Resource, queries: PluralLocalizationsQueries) {
//    ResourceRows(queries.selectAll(id = res.id, project = res.project).asFlow().mapToList()) { scope, loc ->
//    }
}


@Composable
fun ArrayRows(res: Resource, queries: ArrayLocalizationsQueries) {
//    ResourceRows(queries.selectAll(id = res.id, project = res.project).asFlow().mapToList()) { scope, loc ->
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
