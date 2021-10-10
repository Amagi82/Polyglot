package ui.resources

import R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import generators.ResourceGenerator
import kotlinx.coroutines.launch
import project.*
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(vm: ResourceViewModel, toggleDarkTheme: () -> Unit, updateProject: (Project?) -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val project by vm.project.collectAsState()
    val resources by vm.resources.collectAsState()
    val localizedResources by vm.localizedResources.collectAsState()
    val excludedResourceIds by vm.excludedResourceIds.collectAsState()
    val excludedResourceTypes by vm.excludedResourceTypes.collectAsState()
    val showProjectSettings by vm.showProjectSettings.collectAsState()

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
                            ResourceGenerator.generateFiles(project, resources, localizedResources)
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
                        vm.showProjectSettings.value = false
                        showFilters = !showFilters
                    }) {
                        Icon(painterResource(R.drawable.filterList), contentDescription = "Filter")
                    }

                    IconButton(onClick = {
                        showFilters = false
                        vm.showProjectSettings.value = !showProjectSettings
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::createResource) {
                Icon(Icons.Default.Add, contentDescription = "Add new resource")
            }
        }) { paddingValues ->
        Row(Modifier.padding(paddingValues)) {
            val state = rememberLazyListState()
            LazyColumn(Modifier.padding(start = 16.dp, end = 8.dp).weight(1f), state = state) {
                items(resources.filter { (k, v) -> k !in excludedResourceIds && v.type !in excludedResourceTypes }.keys.toList()) { resId ->
                    ResourceRow(vm = vm,
                        resId = resId,
                        deleteResource = {
                            scope.launch {
                                vm.excludedResourceIds.value = excludedResourceIds.plus(resId)
                                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar("Removed ${resId.id}", actionLabel = "Undo")
                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                    vm.excludedResourceIds.value = excludedResourceIds.minus(resId)
                                } else {
                                    vm.localizedResources.value = localizedResources.toMutableMap().apply {
                                        for ((localeIsoCode, localizations) in this) {
                                            put(localeIsoCode, localizations.minus(resId))
                                        }
                                    }.toSortedMap()
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
                SettingsMenu(vm)
            }
            AnimatedVisibility(visible = showFilters) {
                FiltersMenu(vm)
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
