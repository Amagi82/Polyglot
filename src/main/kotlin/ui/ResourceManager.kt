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
import generators.ResourceGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.*
import ui.utils.onPressEnter
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(project: MutableState<Project>, toggleDarkTheme: () -> Unit, updateProject: (Project?) -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    var resources = remember { mutableStateOf(Project.loadResources(project.value.name)) }
    var localizedResources = remember { mutableStateOf(Project.loadLocalizedResources(project.value.name)) }
    var showProjectSettings by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    var excludedLocales by remember { mutableStateOf(setOf<LocaleIsoCode>()) }
    var excludedResourceIds by remember { mutableStateOf(setOf<ResourceId>()) }
    var excludedResourceTypes by remember { mutableStateOf(setOf<Resource.Type>()) }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(project.value.name) },
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
                            ResourceGenerator.generateFiles(project.value, resources.value, localizedResources.value)
                            val result = scaffoldState.snackbarHostState.showSnackbar(
                                message = "Generating outputs",
                                actionLabel = "Show",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                project.value.androidOutputUrl.let(::File).let { openFolder(it, scaffoldState) }
                                project.value.iosOutputUrl.let(::File).let { openFolder(it, scaffoldState) }
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
                    var newId = ResourceId("new")
                    var n = 0
                    while (resources.value[newId] != null) {
                        newId = ResourceId("new$n")
                        n++
                    }
                    resources.value = resources.value.plus(newId to Resource()).apply { save(project.value.name) }
                }
            }) { Icon(Icons.Default.Add, contentDescription = "Add new resource") }
        }) { paddingValues ->
        Row(Modifier.padding(paddingValues)) {
            val state = rememberLazyListState()
            LazyColumn(Modifier.padding(start = 16.dp, end = 8.dp).weight(1f), state = state) {
                items(resources.value.filter { (k, v) -> k !in excludedResourceIds && v.type !in excludedResourceTypes }.keys.toList()) { resId ->
                    DataRow(project = project.value,
                        resId = resId,
                        resources = resources,
                        localizedResources = localizedResources,
                        excludedLocales = excludedLocales,
                        defaultLocale = project.value.defaultLocale,
                        deleteResource = {
                            scope.launch {
                                excludedResourceIds = excludedResourceIds.plus(resId)
                                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar("Removed ${resId.id}", actionLabel = "Undo")
                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                    excludedResourceIds = excludedResourceIds.minus(resId)
                                } else {
                                    localizedResources.value = localizedResources.value.toMutableMap().apply {
                                        for ((localeIsoCode, localizations) in this) {
                                            put(localeIsoCode, localizations.minus(resId))
                                        }
                                        save(project.value.name)
                                    }
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
                ProjectSettings(project = project, localizedResources = localizedResources, onClose = { showProjectSettings = false })
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
                    Resource.Type.values().forEach { resType ->
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
                    localizedResources.value.keys.forEach { localeIsoCode ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Switch(checked = localeIsoCode !in excludedLocales,
                                onCheckedChange = { isChecked ->
                                    excludedLocales = if (isChecked) excludedLocales.minus(localeIsoCode) else excludedLocales.plus(localeIsoCode)
                                })
                            Text(Locale[localeIsoCode].displayName(localeIsoCode == project.value.defaultLocale))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DataRow(
    project: Project,
    resId: ResourceId,
    resources: MutableState<Resources>,
    localizedResources: MutableState<LocalizedResources>,
    excludedLocales: Set<LocaleIsoCode>,
    defaultLocale: LocaleIsoCode,
    deleteResource: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        var id by remember { mutableStateOf(resId) }
        var error by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current

        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = id.id,
                onValueChange = {
                    error = ""
                    id = ResourceId(it)
                },
                modifier = Modifier.padding(vertical = 4.dp).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && resId != id) {
                        scope.launch {
                            if (id in resources.value) {
                                error = "id already exists"
                            } else {
                                resources.value = resources.value.toMutableMap().apply {
                                    val resource = this[resId]!!
                                    remove(resId)
                                    put(id, resource)
                                    save(project.name)
                                }
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
            val resource by remember { derivedStateOf { resources.value[id]!! } }
            localizedResources.value.keys.filter { it !in excludedLocales }.forEach { localeIsoCode ->
                Row(
                    Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val localization = localizedResources.value[localeIsoCode]?.get(id)
                    when (resource.type) {
                        Resource.Type.STRING -> StringRows(defaultLocale, localeIsoCode, localization as? Str ?: Str("")) {
                            scope.launch {
                                localizedResources.value =
                                    localizedResources.value.plus(localeIsoCode to localizedResources.value[localeIsoCode]!!.plus(id to it))
                                localizedResources.value.save(project.name)
                            }

                        }
                        Resource.Type.PLURAL -> PluralRows(defaultLocale, localeIsoCode, localization as? Plural ?: Plural(one = null, other = "")) {

                        }
                        Resource.Type.ARRAY -> ArrayRows(defaultLocale, localeIsoCode, localization as? StringArray ?: StringArray(listOf())) {

                        }
                    }
                }
            }
        }

        val resource = resources.value[id]!!

        Platform.values().forEach { platform ->
            val isIncluded = platform in resource.platforms
            IconButton(onClick = {
                scope.launch {
                    val newResource = resource.copy(platforms = if (isIncluded) resource.platforms.minus(platform) else resource.platforms.plus(platform))
                    resources.value = resources.value.plus(id to newResource).apply { save(project.name) }
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
fun StringRows(defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, string: Str, update: (Str) -> Unit) {
    val focusManager = LocalFocusManager.current
    var oldText = remember { string.text }
    var newText by remember { mutableStateOf(oldText) }
    OutlinedTextField(
        value = newText,
        onValueChange = { newText = it },
        modifier = Modifier.onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
            if (!it.hasFocus && oldText != newText) {
                oldText = newText
                update(Str(newText))
            }
        },
        label = { Text(Locale[localeIsoCode].displayName(localeIsoCode == defaultLocale)) },
        singleLine = true
    )
}

@Composable
fun PluralRows(defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, plural: Plural, update: (Plural) -> Unit) {

}


@Composable
fun ArrayRows(defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, array: StringArray, update: (StringArray) -> Unit) {

}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun openFolder(folder: File, scaffoldState: ScaffoldState) {
    try {
        Desktop.getDesktop().open(folder)
    } catch (e: Exception) {
        scaffoldState.snackbarHostState.showSnackbar("unable to open folder: $e")
    }
}
