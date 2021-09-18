package ui

import R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import locales.Locale
import project.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class, ExperimentalSerializationApi::class)
@Composable
fun App(project: Project, updateResources: (List<Resource>) -> Unit, toggleDarkTheme: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBackdropScaffoldState(BackdropValue.Concealed)
    var expandedIds by remember { mutableStateOf(listOf<String>()) }

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
                    IconButton(onClick = {
                        scope.launch {
                            project.save()
                            scaffoldState.snackbarHostState.showSnackbar("Saved ${project.name}")
                        }
                    }) {
                        Icon(painterResource(R.drawable.save), contentDescription = "Save")
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
            Column(Modifier.padding(16.dp).background(MaterialTheme.colors.primary)) {
                TextField(
                    value = project.name, onValueChange = {},
                    label = { Text("project.Project name") },
                    readOnly = true,
                    singleLine = true
                )
            }
        },
        frontLayerContent = {
            LazyColumn(Modifier.background(MaterialTheme.colors.surface).padding(horizontal = 16.dp).fillMaxSize()) {
                stickyHeader {
                    var sortingState by remember { mutableStateOf<SortingState?>(null) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SortingColumn.values().forEach { column ->
                            TableColumnHeader(column, sortingState = sortingState) { state ->
                                sortingState = state
                                if (state != null) updateResources(state.sort(project.resources))
                            }
                        }
                        Text(
                            "description",
                            modifier = Modifier.padding(16.dp).weight(1f).wrapContentWidth(),
                            style = MaterialTheme.typography.button
                        )
                        Text(
                            "tags",
                            modifier = Modifier.padding(16.dp).weight(1f).wrapContentWidth(),
                            style = MaterialTheme.typography.button
                        )
                        Text(
                            "platforms",
                            modifier = Modifier.padding(16.dp).weight(0.4f),
                            style = MaterialTheme.typography.button
                        )
                        var expandAll by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = {
                                expandAll = !expandAll
                                expandedIds = if (expandAll) project.resources.map { it.id } else listOf()
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                painterResource(if (expandAll) R.drawable.expand else R.drawable.compress),
                                contentDescription = "toggle expand"
                            )
                        }
                    }
                    Divider()
                }

                items(project.resources) { res ->
                    val isEditable = expandedIds.contains(res.id)
                    Row(
                        modifier = Modifier.clickable {
                            expandedIds = if (isEditable) expandedIds.minus(res.id) else expandedIds.plus(res.id)
                        },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(value = res.id, onValueChange = {}, modifier = Modifier.weight(1f), readOnly = !isEditable)
                        BasicTextField(value = res.group, onValueChange = {}, modifier = Modifier.weight(1f), readOnly = !isEditable)
                        BasicTextField(value = res.name, onValueChange = {}, modifier = Modifier.weight(1f), readOnly = !isEditable)
                        BasicTextField(value = res.description, onValueChange = {}, modifier = Modifier.weight(1f), readOnly = !isEditable)
                        BasicTextField(value = res.tags.joinToString(), onValueChange = {}, modifier = Modifier.weight(1f), readOnly = !isEditable)
                        Box(modifier = Modifier.weight(0.4f)) {
                            res.platforms.forEach {
                                Icon(painterResource(it.iconFileName), contentDescription = it.name)
                            }
                        }
                        IconButton(onClick = {
                            val index = project.resources.indexOf(res)
                            updateResources(project.resources.minus(res))
                            scope.launch {
                                if (scaffoldState.snackbarHostState.showSnackbar(
                                        "Removed ${res.id}",
                                        actionLabel = "Undo"
                                    ) == SnackbarResult.ActionPerformed
                                ) {
                                    updateResources(project.resources.toMutableList().apply { add(index, res) })
                                }
                            }
                        }, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                    AnimatedVisibility(visible = isEditable) {
                        Column {
                            when (res) {
                                is Str -> res.localizations.forEach {
                                    Row {
                                        Text(it.key, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        Text(it.value, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                is Plural -> {

                                }
                                is StringArray -> {

                                }
                            }
                        }
                    }
                    Divider()
                }
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(onClick = {
                    updateResources(project.resources.plus(Str(id = "new", localizations = mapOf(Locale.default to "Edit me"))))
//                        resources = resources.plus(Plural(id = "new", one = null, other = Localizations("")))
//                        resources = resources.plus(StringArray(id = "new", items = listOf()))
                }, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add new resource")
                }
            }
        },
        scaffoldState = scaffoldState
    )
}

//enum class ResourceTab {
//    STRING, PLURAL, ARRAY
//}

@Composable
fun RowScope.TableColumnHeader(
    column: SortingColumn,
    sortingState: SortingState?,
    updateSortingState: (SortingState?) -> Unit
) {
    TextButton(onClick = {
        when {
            sortingState?.column != column -> updateSortingState(SortingState(column, isAscending = true))
            else -> updateSortingState(sortingState.copy(isAscending = false))
        }
    }, modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
        Text(column.name.lowercase())
        if (sortingState?.column == column) {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Sort by ${column.name}",
                modifier = Modifier.padding(start = 8.dp).rotate(if (sortingState.isAscending) 0f else 180f)
            )
        }
    }
}

@Stable
data class SortingState(val column: SortingColumn, val isAscending: Boolean) {

    private val Resource.sortingField: String
        get() = when (column) {
            SortingColumn.ID -> id
            SortingColumn.GROUP -> group
            SortingColumn.NAME -> name
        }

    fun sort(resources: List<Resource>): List<Resource> = when {
        isAscending -> resources.sortedBy { it.sortingField }
        else -> resources.sortedByDescending { it.sortingField }
    }
}

enum class SortingColumn {
    ID, GROUP, NAME
}

/*
Dropdown menu
   val projects = rememberSaveable { mutableStateListOf<String>() }
        if (projects.isEmpty()) {
            projects += "Brilliant"
            projects += "Other project 1"
            projects += "Other project 2"
        }

Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    Text("Polyglot")

    var isProjectDropdownExpanded by remember { mutableStateOf(false) }
    var isEditingProjectName by remember { mutableStateOf(false) }

    Column {
        var textfieldSize by remember { mutableStateOf(Size.Zero) }

        TextField(
            value = projects[0],
            onValueChange = { projects[0] = it },
            modifier = Modifier.onGloballyPositioned { coordinates ->
                textfieldSize = coordinates.size.toSize()
            },
            readOnly = !isEditingProjectName,
            label = { Text("project.Project", color = MaterialTheme.colors.onPrimary) },
            trailingIcon = {
                if (isEditingProjectName) {
                    IconButton(onClick = { isEditingProjectName = false }) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = "Finish editing project name",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                } else {
                    IconButton(onClick = { isProjectDropdownExpanded = !isProjectDropdownExpanded }) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = if (isProjectDropdownExpanded) "Close project edit menu" else "Open project edit menu",
                            modifier = Modifier.rotate(if (isProjectDropdownExpanded) 0f else 180f),
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                }
            },
            singleLine = true
        )
        DropdownMenu(
            expanded = isProjectDropdownExpanded,
            onDismissRequest = { isProjectDropdownExpanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { textfieldSize.width.toDp() })
        ) {
            DropdownMenuItem(onClick = {
                isEditingProjectName = true
                isProjectDropdownExpanded = false
            }) {
                Icon(Icons.Default.Edit, contentDescription = "Rename project")
                Text("Rename project", modifier = Modifier.padding(start = 8.dp))
            }
            DropdownMenuItem(onClick = {
                projects.add(0, "New project")
                isEditingProjectName = true
                isProjectDropdownExpanded = false
            }) {
                Icon(Icons.Default.Add, contentDescription = "Create new project")
                Text("New project", modifier = Modifier.padding(start = 8.dp))
            }
            Divider()

            projects.drop(1).forEach { project ->
                DropdownMenuItem(onClick = {
                    projects.remove(project)
                    projects.add(0, project)
                    isProjectDropdownExpanded = false
                }) {
                    Text(project)
                }
            }
        }
    }
}*/
