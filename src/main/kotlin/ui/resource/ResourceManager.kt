package ui.resource

import R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import project.Platform
import project.ResourceType
import ui.core.IconButton
import ui.resource.backdrop.ImportSettings
import ui.resource.backdrop.LocaleSettings
import ui.resource.backdrop.ExportSettings
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(vm: ResourceViewModel, toggleDarkTheme: () -> Unit, closeProject: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBackdropScaffoldState(BackdropValue.Concealed)
    val selectedTab by vm.selectedTab.collectAsState()

    BackdropScaffold(
        appBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(vm.project.name, modifier = Modifier.weight(1f))
                        TabRow(selectedTab.ordinal, modifier = Modifier.weight(1f), backgroundColor = MaterialTheme.colors.primary) {
                            ResourceType.values().forEach { type ->
                                Tab(
                                    selected = selectedTab == type,
                                    onClick = { vm.selectedTab.value = type },
                                    text = { Text(type.name) })
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { scope.launch { if (scaffoldState.isRevealed) scaffoldState.conceal() else scaffoldState.reveal() } },
                        modifier = Modifier.size(40.dp)
                    ) {
                        val transition = updateTransition(scaffoldState.progress)
                        val iconSize by transition.animateDp { 24.dp * it.fraction }
                        val showConcealed = scaffoldState.showConcealed
                        Icon(
                            if (showConcealed) Icons.Default.Menu else Icons.Default.Close,
                            contentDescription = if (showConcealed) "Reveal options" else "Hide options",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                },
                actions = {
                    IconButton(R.drawable.darkMode, contentDescription = "Toggle dark theme", onClick = toggleDarkTheme)
                    IconButton(Icons.Default.Send) {
                        val firstInvalid = vm.findFirstInvalidResource()
                        if (firstInvalid == null) {
                            scope.launch { generateFiles(vm, scaffoldState.snackbarHostState) }
                        } else {
                            val (tab, resId) = firstInvalid
                            if (tab != selectedTab) {
                                vm.selectedTab.value = tab
                            }
                            vm.resourceViewModel(tab).scrollToItem.value = resId
                        }
                    }
                    var isMenuExpanded by remember { mutableStateOf(false) }
                    IconButton(Icons.Default.MoreVert, contentDescription = "Options") { isMenuExpanded = true }
                    Box {
                        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                            DropdownMenuItem(onClick = closeProject) { Text("Close project") }
                        }
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = 0.dp
            )
        },
        gesturesEnabled = false,
        backLayerContent = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ImportSettings(vm)
                ExportSettings(vm)
                LocaleSettings(vm)
            }
        },
        frontLayerContent = {
            val displayedLocales by vm.displayedLocales.collectAsState(null)
            displayedLocales?.let {
                ResourceTable(vm.resourceViewModel(selectedTab), it)
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                ExtendedFloatingActionButton(text = { Text("Add") },
                    onClick = vm.resourceViewModel(selectedTab)::createResource,
                    modifier = Modifier.padding(16.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add new resource") })
            }
        },
        scaffoldState = scaffoldState
    )
}

@OptIn(ExperimentalMaterialApi::class)
private val BackdropScaffoldState.showConcealed
    get() = when {
        direction > 0 -> progress.fraction < 0.5
        direction < 0 -> progress.fraction > 0.5
        else -> isConcealed
    }

private suspend fun generateFiles(vm: ResourceViewModel, snackbarHostState: SnackbarHostState) = withContext(Dispatchers.Main) {
    val generateFiles = Platform.values().map { async(Dispatchers.IO) { it.exportResources(vm) } }
    val result = snackbarHostState.showSnackbar(message = "Generating outputs", actionLabel = "Show")
    generateFiles.awaitAll()
    if (result == SnackbarResult.ActionPerformed) {
        vm.exportUrls.value.forEach { openUrl(it.value, snackbarHostState) }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun openUrl(url: String, snackbarHostState: SnackbarHostState) {
    try {
        val folder = File(url)
        Desktop.getDesktop().open(folder)
    } catch (e: Exception) {
        snackbarHostState.showSnackbar("unable to open folder: $e")
    }
}
