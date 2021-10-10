package ui.projects

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
import project.Project
import ui.core.onPressEnter
import java.io.File

@Composable
fun ProjectPicker(onProjectSelected: (Project) -> Unit) {
    val projects by remember { derivedStateOf { Project.projectFolders } }
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
                Button(onClick = { onProjectSelected(Project.load(project.nameWithoutExtension)) }) {
                    Text(project.nameWithoutExtension)
                }
            }

            if (showDialog) {
                ProjectPickerCreateDialog(
                    projects = projects,
                    onProjectSelected = onProjectSelected,
                    onDismiss = { showDialog = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun ProjectPickerCreateDialog(projects: List<File>, onProjectSelected: (Project) -> Unit, onDismiss: () -> Unit) {
    var newProjectName by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val onClickCreate: () -> Unit = {
        when {
            newProjectName.isBlank() -> errorMsg = "Name required"
            projects.any { it.name == newProjectName } -> errorMsg = "Project already exists"
            else -> onProjectSelected(Project(name = newProjectName))
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
