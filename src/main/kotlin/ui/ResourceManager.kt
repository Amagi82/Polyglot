package ui

import R
import androidx.compose.animation.ExperimentalAnimationApi
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
import generators.ResourceGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import locales.LocaleIsoCode
import project.Platform
import project.ResourceType
import sqldelight.*
import ui.utils.onPressEnter
import java.io.File
import java.util.*
import javax.swing.JFileChooser

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(project: Project, db: PolyglotDatabase, toggleDarkTheme: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBackdropScaffoldState(BackdropValue.Concealed)
    val resourceList: List<Resource> by db.resourceQueries.selectAll(project.name).asFlow().mapToList().collectAsState(listOf())

    BackdropScaffold(
        appBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { if (scaffoldState.isRevealed) scaffoldState.conceal() else scaffoldState.reveal() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Project settings")
                    }
                },
                actions = {
                    IconButton(onClick = toggleDarkTheme) {
                        Icon(painterResource(R.drawable.darkMode), contentDescription = "Toggle dark theme")
                    }
                    IconButton(onClick = {}) {
                        Icon(painterResource(R.drawable.importExport), contentDescription = "Import or Export")
                    }
                    IconButton(onClick = { ResourceGenerator.generateFiles(project, db) }) {
                        Icon(Icons.Default.Build, contentDescription = "Build")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                elevation = 0.dp,
                backgroundColor = Color.Transparent
            )
        },
        backLayerContent = {
            Row(Modifier.padding(16.dp).background(MaterialTheme.colors.primary)) {
                Column {
                    OutputFileSelectionButton(Platform.ANDROID.displayName, project.androidOutputUrl) {
                        db.projectQueries.updateAndroidOutputFolder(it, project.name)
                    }
                    OutputFileSelectionButton(Platform.IOS.displayName, project.iosOutputUrl) {
                        db.projectQueries.updateIOSOutputFolder(it, project.name)
                    }
                }

                Column {
                    var locales by remember { mutableStateOf(project.locales) }
                    locales.forEach { isoCode ->
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(color = MaterialTheme.colors.secondary, shape = MaterialTheme.shapes.large)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(Locale.forLanguageTag(isoCode.value).displayName)
                            IconButton(
                                onClick = {
                                    scope.launch { deleteLocale(db, project.name, locales, isoCode) }
                                    locales = locales.minus(isoCode)
                                },
                                enabled = isoCode != project.defaultLocale
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Remove ${isoCode.value}")
                            }
                        }
                    }
                }

                Column {
                    Text("Add/Remove Locales", modifier = Modifier.padding(vertical = 8.dp))

                    val locales by remember { derivedStateOf { Locale.getAvailableLocales().sortedBy { it.toString() } } }

                    var newLanguage by remember { mutableStateOf("") }
                    Box {
                        val languages by remember { derivedStateOf { Locale.getISOLanguages().sorted() } }
                        var isDropdownExpanded by remember { mutableStateOf(false) }
                        OutlinedTextField(value = newLanguage, onValueChange = { newLanguage = it }, label = { Text("Language") })

                        DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                            languages.forEach { language ->
                                DropdownMenuItem(onClick = {
                                    isDropdownExpanded = false
                                    newLanguage = language
                                }) {
                                    Text(language)
                                }
                            }
                        }
                    }
                    val regionsForLanguage = locales.filter { it.language == newLanguage }.map { it.country }
                    if (regionsForLanguage.isNotEmpty()) {
                        Box {
                            var newRegion by remember { mutableStateOf("") }
                            var isDropdownExpanded by remember { mutableStateOf(false) }
                            OutlinedTextField(value = newRegion, onValueChange = { newRegion = it }, label = { Text("New Language") })

                            DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                                listOf("en", "es", "fr", "de").map { LocaleIsoCode(it) }.filter { it !in project.locales }.forEach { locale ->
                                    DropdownMenuItem(onClick = {
                                        isDropdownExpanded = false
                                        newRegion = locale.value
                                    }) {
                                        Text(locale.value)
                                    }
                                }
                            }
                        }
                    }

                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "Add locale")
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
                                        locale = locale,
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
                            if (db.resourceQueries.exists(id, res.project).executeAsOne()) {
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
                ResourceType.STRING -> StringRows(res.id, res.project, db.stringLocalizationsQueries)
                ResourceType.PLURAL -> PluralRows(res.id, res.project, db.pluralLocalizationsQueries)
                ResourceType.ARRAY -> ArrayRows(res.id, res.project, db.arrayLocalizationsQueries)
            }
        }

        Platform.values().forEach { platform ->
            val isIncluded = platform in res.platforms
            IconButton(
                onClick = {
                    scope.launch {
                        db.resourceQueries.updatePlatforms(
                            platforms = (if (isIncluded) res.platforms.minus(platform) else res.platforms.plus(platform)),
                            id = id,
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
                        ResourceType.STRING -> db.stringLocalizationsQueries.selectAllWithId(id, res.project).executeAsList()
                        ResourceType.PLURAL -> db.pluralLocalizationsQueries.selectAllWithId(id, res.project).executeAsList()
                        ResourceType.ARRAY -> db.arrayLocalizationsQueries.selectAllWithId(id, res.project).executeAsList()
                    }
                    db.resourceQueries.delete(id, res.project)
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
fun StringRows(id: String, projectName: String, queries: StringLocalizationsQueries) {
    ResourceRows(queries.selectAllWithId(id = id, project = projectName).asFlow().mapToList()) { scope, loc ->
        var newText by remember { mutableStateOf(loc.text) }
        OutlinedTextField(
            value = newText,
            onValueChange = { newText = it },
            modifier = Modifier.onFocusChanged {
                if (!it.hasFocus && loc.text != newText) {
                    scope.launch { queries.updateText(text = newText, id = id, project = projectName, locale = loc.locale) }
                }
            },
            label = { Text(Locale.forLanguageTag(loc.locale.value).displayName) })
    }
}

@Composable
fun PluralRows(id: String, projectName: String, queries: PluralLocalizationsQueries) {
//    ResourceRows(queries.selectAll(id = id, project = projectName).asFlow().mapToList()) { scope, loc ->
//    }
}


@Composable
fun ArrayRows(id: String, projectName: String, queries: ArrayLocalizationsQueries) {
//    ResourceRows(queries.selectAll(id = id, project = projectName).asFlow().mapToList()) { scope, loc ->
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

private fun addLocale(db: PolyglotDatabase, projectName: String, locales: List<LocaleIsoCode>, locale: LocaleIsoCode) {
    db.transaction {
        val resourceList = db.resourceQueries.selectAll(projectName).executeAsList()
        db.projectQueries.updateLocales(locales.plus(locale).distinct(), name = projectName)
        resourceList.forEach {
            when (it.type) {
                ResourceType.STRING -> db.stringLocalizationsQueries.insert(
                    StringLocalizations(id = it.id, project = projectName, locale = locale, text = "")
                )
                ResourceType.PLURAL -> db.pluralLocalizationsQueries.insert(
                    PluralLocalizations(
                        id = it.id,
                        project = projectName,
                        locale = locale,
                        zero = null,
                        one = "",
                        two = null,
                        few = null,
                        many = null,
                        other = ""
                    )
                )
                ResourceType.ARRAY -> db.arrayLocalizationsQueries.insert(
                    ArrayLocalizations(id = it.id, project = projectName, locale = locale, array = listOf())
                )
            }
        }
    }
}

private fun deleteLocale(db: PolyglotDatabase, projectName: String, locales: List<LocaleIsoCode>, locale: LocaleIsoCode) {
    db.transaction {
        db.projectQueries.updateLocales(locales.minus(locale), name = projectName)
        db.arrayLocalizationsQueries.deleteLocale(project = projectName, locale = locale)
        db.pluralLocalizationsQueries.deleteLocale(project = projectName, locale = locale)
        db.stringLocalizationsQueries.deleteLocale(project = projectName, locale = locale)
    }
}

@Composable
private fun OutputFileSelectionButton(platformName: String, outputUrl: String, onOutputFolderChanged: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var isFileDialogOpen: Boolean by remember { mutableStateOf(false) }
    var outputFolder: String by remember { mutableStateOf(outputUrl) }

    if (isFileDialogOpen) {
        FileDialog(File(outputFolder), onCloseRequest = {
            if (it != null) scope.launch {
                outputFolder = it
                onOutputFolderChanged(it)
            }
            isFileDialogOpen = false
        })
    }

    OutlinedTextField(
        value = outputFolder,
        onValueChange = { outputFolder = it },
        modifier = Modifier.padding(16.dp).clickable { isFileDialogOpen = true },
        enabled = false,
        label = { Text("$platformName output") },
        singleLine = true
    )
}

@Composable
private fun FileDialog(folder: File, onCloseRequest: (result: String?) -> Unit) {
    val scope = rememberCoroutineScope()
    scope.launch(Dispatchers.Swing) {
        JFileChooser(folder.apply(File::mkdirs).absoluteFile).apply {
            dialogTitle = "Choose output folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            showOpenDialog(null)

            onCloseRequest(selectedFile?.path)
        }
    }
}
