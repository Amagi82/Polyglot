package ui.resource.backdrop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import project.Platform
import ui.resource.ResourceViewModel
import java.io.File
import javax.swing.JFileChooser

@Composable
fun ExportSettings(vm: ResourceViewModel) {
    val project by vm.project.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Export Resources", style = MaterialTheme.typography.h6)
        Platform.values().forEach { platform ->
            DestinationFileSelectionButton(platform, platform.exportUrl(project)) {
                vm.project.value = project.copy(exportUrls = project.exportUrls.plus(platform to it))
            }
        }
    }
}

@Composable
private fun DestinationFileSelectionButton(platform: Platform, exportUrl: String, onExportUrlChanged: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var isFileDialogOpen: Boolean by remember { mutableStateOf(false) }
    var exportFolder: String by remember { mutableStateOf(exportUrl) }

    if (isFileDialogOpen) {
        FileDialog(File(exportFolder), onCloseRequest = {
            if (it != null) scope.launch {
                exportFolder = it
                onExportUrlChanged(it)
            }
            isFileDialogOpen = false
        })
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { isFileDialogOpen = true }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)) {
            Text(platform.displayName)
        }
        Text(exportUrl)
    }
}

@Composable
private fun FileDialog(folder: File, onCloseRequest: (result: String?) -> Unit) {
    val scope = rememberCoroutineScope()
    scope.launch((Dispatchers.Swing)) {
        JFileChooser(folder.apply(File::mkdirs).absoluteFile).apply {
            dialogTitle = "Choose destination folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            showOpenDialog(null)
            onCloseRequest(selectedFile?.path)
        }
    }
}
