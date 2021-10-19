package ui.resource

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
import androidx.compose.ui.unit.dp
import generators.ResourceGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import project.*
import ui.resource.menu.FiltersMenu
import ui.core.IconButton
import ui.resource.menu.MenuState.*
import ui.resource.menu.SettingsMenu
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(vm: ResourceViewModel, toggleDarkTheme: () -> Unit, updateProject: (Project?) -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val project by vm.project.collectAsState()
    val menuState by vm.menuState.collectAsState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = { IconButton(Icons.Default.ArrowBack, contentDescription = "Close Project") { updateProject(null) } },
                actions = {
                    IconButton(R.drawable.darkMode, contentDescription = "Toggle dark theme", onClick = toggleDarkTheme)
                    IconButton(R.drawable.importExport, contentDescription = "Import or Export") {}
                    IconButton(Icons.Default.Build) { scope.launch { generateFiles(vm, scaffoldState) } }
                    IconButton(R.drawable.filterList, contentDescription = "Filter") { vm.menuState.value = if (menuState != FILTERS) FILTERS else CLOSED }
                    IconButton(Icons.Default.Settings) { vm.menuState.value = if (menuState != SETTINGS) SETTINGS else CLOSED }
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
            val resources by vm.resourceMetadata.collectAsState()
            val excludedTypes by vm.excludedResourceInfoTypes.collectAsState()
            var excludedResourceIds by remember { mutableStateOf(setOf<ResourceId>()) }
            val includedResources by remember(resources, excludedResourceIds, excludedTypes) {
                derivedStateOf { resources.filter { (k, v) -> k !in excludedResourceIds && v.type !in excludedTypes }.keys.sorted() }
            }

            LazyColumn(Modifier.padding(start = 16.dp, end = 8.dp).weight(1f), state = state) {
                items(includedResources) { resId ->
                    ResourceRow(vm = vm,
                        resId = resId,
                        deleteResource = {
                            excludedResourceIds = excludedResourceIds.plus(resId)
                            scope.launch {
                                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar("Removed ${resId.id}", actionLabel = "Undo")
                                if (snackbarResult != SnackbarResult.ActionPerformed) {
                                    vm.resourceMetadata.value = vm.resourceMetadata.value.minus(resId)
                                    vm.localizedResources.value = vm.localizedResources.value.map { it.key to it.value.minus(resId) }.toMap()
                                }
                                excludedResourceIds = excludedResourceIds.minus(resId)
                            }
                        })
                    Divider()
                }
            }

            VerticalScrollbar(adapter = ScrollbarAdapter(state))

            if (menuState != CLOSED) {
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
            }

            AnimatedVisibility(visible = menuState == SETTINGS) {
                SettingsMenu(vm)
            }
            AnimatedVisibility(visible = menuState == FILTERS) {
                FiltersMenu(vm)
            }
        }
    }
}

private suspend fun generateFiles(vm: ResourceViewModel, scaffoldState: ScaffoldState) {
    val result = scaffoldState.snackbarHostState.showSnackbar(
        message = "Generating outputs",
        actionLabel = "Show",
        duration = SnackbarDuration.Long
    )
    val project = vm.project.value
    ResourceGenerator.generateFiles(project, vm.resourceMetadata.value, vm.localizedResources.value)
    if (result == SnackbarResult.ActionPerformed) {
        openUrl(project.androidOutputUrl, scaffoldState)
        openUrl(project.iosOutputUrl, scaffoldState)
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun openUrl(url: String, scaffoldState: ScaffoldState) {
    try {
        val folder = File(url)
        Desktop.getDesktop().open(folder)
    } catch (e: Exception) {
        scaffoldState.snackbarHostState.showSnackbar("unable to open folder: $e")
    }
}
