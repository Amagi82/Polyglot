package ui.resource

import R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.unit.dp
import generators.ResourceGenerator
import kotlinx.coroutines.launch
import project.Project
import project.ResourceId
import project.ResourceInfo
import project.ResourceMetadata
import ui.core.IconButton
import ui.resource.menu.FiltersMenu
import ui.resource.menu.MenuState.*
import ui.resource.menu.SettingsMenu
import java.awt.Desktop
import java.io.File

@Composable
fun ResourceManager(vm: ResourceViewModel, toggleDarkTheme: () -> Unit, updateProject: (Project?) -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val project by vm.project.collectAsState()
    val menuState by vm.menuState.collectAsState()
    val selectedTab by vm.selectedTab.collectAsState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            Column {
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

                Surface(elevation = AppBarDefaults.TopAppBarElevation) {
                    TabRow(selectedTab.index) {
                        ResourceInfo.Type.values().forEach { type ->
                            Tab(
                                selected = selectedTab == type,
                                onClick = { vm.selectedTab.value = type },
                                text = { Text(type.name) })
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::createResource) {
                Icon(Icons.Default.Add, contentDescription = "Add new resource")
            }
        }) { paddingValues ->
        Row(Modifier.padding(paddingValues)) {
            val state = rememberLazyListState()
            val resources by vm.resourceMetadata.collectAsState()
            val includedResources by remember(resources, selectedTab) {
                derivedStateOf { resources.filter { (_, v) -> v.type == selectedTab }.keys.sorted() }
            }

            LazyColumn(Modifier.padding(start = 16.dp, end = 8.dp).weight(1f), state = state) {
                items(includedResources) { resId ->
                    ResourceRow(vm = vm, resId = resId)
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
