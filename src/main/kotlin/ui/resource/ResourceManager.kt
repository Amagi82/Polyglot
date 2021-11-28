package ui.resource

import R
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update
import project.ResourceGroup
import project.Platform
import project.ResourceType
import ui.core.IconButton
import ui.core.onPressEnter
import ui.core.onPressEsc
import ui.resource.backdrop.ImportSettings
import ui.resource.backdrop.LocaleSettings
import ui.resource.backdrop.ExportSettings
import ui.resource.backdrop.GroupSettings
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ResourceManager(vm: ResourceViewModel, toggleDarkTheme: () -> Unit, closeProject: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBackdropScaffoldState(BackdropValue.Concealed)
    val selectedTab by vm.selectedTab.collectAsState()
    val isMultiSelectEnabled by vm.isMultiSelectEnabled.collectAsState()
    var isLabelDialogShown by remember { mutableStateOf(false) }

    BackdropScaffold(
        appBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(vm.projectName, modifier = Modifier.weight(1f))
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
                    if (isMultiSelectEnabled) {
                        IconButton(R.drawable.label, contentDescription = "Add to Group") { isLabelDialogShown = true }
                        IconButton(R.drawable.labelOff, contentDescription = "Remove from Group") {
                            vm.resourceViewModel(selectedTab).putSelectedInGroup(ResourceGroup())
                        }
                    } else {
                        Spacer(Modifier.width(96.dp))
                    }
                    IconButton(
                        if (isMultiSelectEnabled) R.drawable.rule else R.drawable.checklist,
                        contentDescription = "Toggle multi-select",
                        onClick = { vm.isMultiSelectEnabled.update { !it } })
                    IconButton(R.drawable.darkMode, contentDescription = "Toggle dark theme", onClick = toggleDarkTheme)
                    IconButton(Icons.Default.Send) {
                        val firstInvalid = vm.findFirstInvalidResource()
                        if (firstInvalid == null) {
                            scope.launch { exportFiles(vm, scaffoldState.snackbarHostState) }
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
                GroupSettings(vm)
                LocaleSettings(vm)
                ImportSettings(vm)
                ExportSettings(vm)
            }
        },
        frontLayerContent = {
            val excludedGroups by vm.excludedGroups.collectAsState()
            val displayedLocales by vm.displayedLocales.collectAsState(null)
            displayedLocales?.let {
                ResourceTable(
                    vm.resourceViewModel(selectedTab),
                    excludedGroups = excludedGroups,
                    displayedLocales = it,
                    isMultiSelectEnabled = isMultiSelectEnabled
                )
            }

            if (isLabelDialogShown) {
                GroupSelectionDialog(
                    dismiss = { isLabelDialogShown = false },
                    putSelectedInGroup = {
                        vm.resourceViewModel(selectedTab).putSelectedInGroup(it)
                        isLabelDialogShown = false
                    }
                )
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                ExtendedFloatingActionButton(text = { Text("Add") },
                    onClick = vm.resourceViewModel(selectedTab)::createResource,
                    modifier = Modifier.padding(16.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add new resource") })
            }
        },
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(scaffoldState.snackbarHostState, Modifier.width(344.dp)) }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun GroupSelectionDialog(dismiss: () -> Unit, putSelectedInGroup: (ResourceGroup) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        var group by remember { mutableStateOf(ResourceGroup()) }
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(onClick = { putSelectedInGroup(group) }, enabled = group.name.isNotEmpty()) {
                    Text("Add")
                }
            },
            modifier = Modifier.onPressEnter { putSelectedInGroup(group) }.onPressEsc(dismiss),
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text("Cancel")
                }
            },
            text = {
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Add selected rows to group", style = MaterialTheme.typography.subtitle1)
                    TextField(
                        value = group.value,
                        onValueChange = { group = GroupId(it.filter(Char::isLetterOrDigit)) },
                        singleLine = true
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
private val BackdropScaffoldState.showConcealed
    get() = when {
        direction > 0 -> progress.fraction < 0.5
        direction < 0 -> progress.fraction > 0.5
        else -> isConcealed
    }

private suspend fun exportFiles(vm: ResourceViewModel, snackbarHostState: SnackbarHostState) = withContext(Dispatchers.Main) {
    val exportFiles = Platform.values().map { async(Dispatchers.IO) { it.exportResources(vm) } }
    val result = snackbarHostState.showSnackbar(message = "Generating outputs", actionLabel = "Show")
    exportFiles.awaitAll()
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
