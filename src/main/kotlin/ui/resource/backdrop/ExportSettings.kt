package ui.resource.backdrop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    val exportUrls by vm.exportUrls.collectAsState()

    Column {
        Text("Export Resources", style = MaterialTheme.typography.h6)
        Platform.values().forEach { platform ->
            DestinationFileSelectionButton(platform, exportUrls[platform] ?: platform.defaultOutputUrl) {
                vm.exportUrls.value = exportUrls.plus(platform to it)
            }
            Spacer(Modifier.height(8.dp))
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
    Button(onClick = { isFileDialogOpen = true }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)) {
        Text(platform.displayName)
    }
    Text(exportUrl)
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
