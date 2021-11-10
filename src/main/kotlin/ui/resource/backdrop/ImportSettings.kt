package ui.resource.backdrop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import importers.importAndroidResources
import importers.importIosResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import project.Platform
import project.ResourceType
import ui.resource.ResourceViewModel
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter

@Composable
fun ImportSettings(vm: ResourceViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val project by vm.project.collectAsState()
        val scope = rememberCoroutineScope()
        var fileDialog: Platform? by remember { mutableStateOf(null) }
        var importingState: Map<Platform, ImportingState> by remember { mutableStateOf(Platform.values().associateWith { ImportingState.Idle }) }
        var overwrite by remember { mutableStateOf(true) }

        Text("Import Resources", style = MaterialTheme.typography.h6)

        importingState.forEach { (platform, state) ->
            Button(onClick = { fileDialog = platform }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)) {
                Text(platform.displayName)
            }
            when (state) {
                ImportingState.Idle -> Unit
                ImportingState.Importing -> CircularProgressIndicator()
                is ImportingState.Success -> {
                    state.files.forEach {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${it.parentFile.name}${File.separator}${it.name}")
                            Icon(Icons.Default.Check, contentDescription = "Successfully imported")
                        }
                    }
                }
                is ImportingState.Failure -> Text("Import failed\n${state.error.localizedMessage}")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Overwrite")
            Checkbox(
                checked = overwrite,
                onCheckedChange = { overwrite = !overwrite },
                colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colors.onSecondary)
            )
        }

        val platform = fileDialog
        if (platform != null) {
            scope.launch((Dispatchers.Swing)) {
                JFileChooser(platform.outputFolder(project).apply(File::mkdirs).absoluteFile).apply {
                    dialogTitle = "Select file/directory to import"
                    fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
                    fileFilter = object : FileFilter() {
                        val resourceFileExtensions = ResourceType.values().map { platform.fileName(it).substringAfterLast('.') }.distinct()
                        override fun accept(f: File?): Boolean = f?.isDirectory == true || f?.extension in resourceFileExtensions
                        override fun getDescription(): String = resourceFileExtensions.joinToString { "*.$it" }
                    }
                    showOpenDialog(null)
                    selectedFile?.let { file ->
                        importingState = importingState.plus(platform to ImportingState.Importing)
                        scope.launch {
                            importingState = try {
                                val importedFiles = platform.importResources(vm, file, overwrite = overwrite)
                                importingState.plus(platform to ImportingState.Success(importedFiles))
                            } catch (e: Exception) {
                                importingState.plus(platform to ImportingState.Failure(e))
                            }
                        }
                    }
                    fileDialog = null
                }
            }
        }
    }
}

sealed interface ImportingState {
    object Idle : ImportingState
    object Importing : ImportingState
    data class Success(val files: List<File>) : ImportingState
    data class Failure(val error: Exception) : ImportingState
}
