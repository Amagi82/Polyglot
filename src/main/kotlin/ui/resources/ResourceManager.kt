package ui.resources

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import generators.ResourceGenerator
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.*
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
                    ResourceRow(project = project.value,
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

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun openFolder(folder: File, scaffoldState: ScaffoldState) {
    try {
        Desktop.getDesktop().open(folder)
    } catch (e: Exception) {
        scaffoldState.snackbarHostState.showSnackbar("unable to open folder: $e")
    }
}