package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.launch
import locales.LocaleIsoCode
import sqldelight.Project
import sqldelight.ProjectQueries
import ui.utils.onPressEnter

@Composable
fun ProjectPicker(projectQueries: ProjectQueries, onProjectSelected: (Project) -> Unit) {
    val projects: List<Project> by projectQueries.selectAll().asFlow().mapToList().collectAsState(listOf())
    var showDialog by remember { mutableStateOf(projects.isEmpty()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Projects") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "New project")
            }
        }) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            projects.forEach { project ->
                Button(onClick = { onProjectSelected(project) }) {
                    Text(project.name)
                }
            }

            if (showDialog) {
                ProjectPickerCreateDialog(
                    projectQueries = projectQueries,
                    projects = projects,
                    onProjectSelected = onProjectSelected,
                    onDismiss = { showDialog = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun ProjectPickerCreateDialog(projectQueries: ProjectQueries, projects: List<Project>, onProjectSelected: (Project) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var newProjectName by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val onClickCreate: () -> Unit = {
        when {
            newProjectName.isBlank() -> errorMsg = "Name required"
            projects.any { it.name == newProjectName } -> errorMsg = "Project already exists"
            else -> scope.launch {
                val newProject = Project(
                    name = newProjectName,
                    androidOutputUrl = "output/Android",
                    iosOutputUrl = "output/iOS",
                    locales = listOf(LocaleIsoCode("en")), defaultLocale = LocaleIsoCode("en")
                )
                scope.launch { projectQueries.insert(newProject) }
                onProjectSelected(newProject)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onClickCreate, enabled = newProjectName.isNotBlank()) { Text("Create") } },
        dismissButton = { if (projects.isNotEmpty()) TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Create new project") },
        text = {
            Column {
                val requester = FocusRequester()
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = {
                        newProjectName = it
                        if (errorMsg.isNotEmpty() && projects.none { proj -> proj.name == newProjectName }) {
                            errorMsg = ""
                        }
                    },
                    modifier = Modifier.onPressEnter { newProjectName.isNotBlank().also { isValid -> if (isValid) onClickCreate() } }.focusRequester(requester),
                    label = { Text("Name") },
                    isError = errorMsg.isNotEmpty(),
                    singleLine = true
                )
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = MaterialTheme.colors.error)
                }

                // OutlinedTextField doesn't get focus automatically
                LaunchedEffect("reqFocus") {
                    requester.requestFocus()
                }
            }
        })
}
