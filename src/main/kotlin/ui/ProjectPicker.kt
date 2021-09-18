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
import androidx.compose.ui.unit.dp
import json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import project.Project
import utils.Settings

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun ProjectPicker(onProjectSelected: (Project) -> Unit) {
    val projects by remember { mutableStateOf(Project.all) }
    var showDialog by remember { mutableStateOf(false) }

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
                ProjectPickerCreateDialog(projects = projects, onProjectSelected = onProjectSelected, onDismiss = { showDialog = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ProjectPickerCreateDialog(projects: List<Project>, onProjectSelected: (Project) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var newProjectName by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                when {
                    newProjectName.isBlank() -> errorMsg = "Name required"
                    projects.any { it.name == newProjectName } -> errorMsg = "Project already exists"
                    else -> scope.launch {
                        val newProject = Project(newProjectName, listOf())
                        newProject.save()
                        onProjectSelected(newProject)
                    }
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Create new project") },
        text = {
            Column {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = {
                        newProjectName = it
                        if (errorMsg.isNotEmpty() && projects.none { proj -> proj.name == newProjectName }) {
                            errorMsg = ""
                        }
                    },
                    label = { Text("Name") },
                    isError = errorMsg.isNotEmpty()
                )
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = MaterialTheme.colors.error)
                }
            }
        })
}
