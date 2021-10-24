package ui.resource

import R
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import project.Project
import project.ResourceInfo
import ui.core.IconButton
import ui.resource.backdrop.LocaleSettings
import ui.resource.backdrop.OutputSettings
import java.awt.Desktop
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(vm: ResourceViewModel, toggleDarkTheme: () -> Unit, updateProject: (Project?) -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBackdropScaffoldState(BackdropValue.Concealed)
    val project by vm.project.collectAsState()
    val selectedTab by vm.selectedTab.collectAsState()

    BackdropScaffold(
        appBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(project.name, modifier = Modifier.weight(1f))
                        TabRow(selectedTab.index, modifier = Modifier.weight(1f), backgroundColor = MaterialTheme.colors.primary) {
                            ResourceInfo.Type.values().forEach { type ->
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
                    val transition = updateTransition(scaffoldState.progress)
                    val iconSize by transition.animateDp { 24.dp * abs(2 * (0.5f - it.fraction)) }
                    IconButton(onClick = { scope.launch { if (scaffoldState.isRevealed) scaffoldState.conceal() else scaffoldState.reveal() } }) {
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
                    IconButton(Icons.Default.Send) { scope.launch { generateFiles(vm, scaffoldState.snackbarHostState) } }
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
                OutputSettings(vm)
                LocaleSettings(vm)
            }
        },
        frontLayerContent = {
            Row {
                val state = rememberLazyListState()
                val resources by vm.displayedResources.collectAsState(listOf())

                LazyColumn(Modifier.padding(start = 16.dp, end = 8.dp).weight(1f), state = state) {
                    items(resources, key = { it.id }) { resId ->
                        ResourceRow(vm = vm, resId = resId)
                        Divider()
                    }
                }

                VerticalScrollbar(adapter = ScrollbarAdapter(state))
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                ExtendedFloatingActionButton(text = { Text("Add") },
                    onClick = vm::createResource,
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

private suspend fun generateFiles(vm: ResourceViewModel, snackbarHostState: SnackbarHostState) {
    val result = snackbarHostState.showSnackbar(
        message = "Generating outputs",
        actionLabel = "Show",
        duration = SnackbarDuration.Long
    )
    vm.generateFiles()
    if (result == SnackbarResult.ActionPerformed) {
        openUrl(vm.project.value.androidOutputUrl, snackbarHostState)
        openUrl(vm.project.value.iosOutputUrl, snackbarHostState)
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
