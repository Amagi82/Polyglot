package ui.resource.backdrop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import project.Platform
import ui.core.outlinedTextFieldColorsOnPrimary
import ui.resource.ResourceViewModel
import java.io.File
import javax.swing.JFileChooser

@Composable
fun OutputSettings(vm: ResourceViewModel) {
    val project by vm.project.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Outputs", style = MaterialTheme.typography.h6)
        OutputFileSelectionButton(Platform.ANDROID.displayName, project.androidOutputUrl) {
            vm.project.value = project.copy(androidOutputUrl = it)
        }
        OutputFileSelectionButton(Platform.IOS.displayName, project.iosOutputUrl) {
            vm.project.value = project.copy(iosOutputUrl = it)
        }
    }
}

@Composable
private fun OutputFileSelectionButton(platformName: String, outputUrl: String, onOutputFolderChanged: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var isFileDialogOpen: Boolean by remember { mutableStateOf(false) }
    var outputFolder: String by remember { mutableStateOf(outputUrl) }

    if (isFileDialogOpen) {
        FileDialog(File(outputFolder), onCloseRequest = {
            if (it != null) scope.launch {
                outputFolder = it
                onOutputFolderChanged(it)
            }
            isFileDialogOpen = false
        })
    }

    OutlinedTextField(
        value = outputFolder,
        onValueChange = { outputFolder = it },
        modifier = Modifier.clickable { isFileDialogOpen = true },
        enabled = false,
        label = { Text("$platformName output") },
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColorsOnPrimary()
    )
}

@Composable
private fun FileDialog(folder: File, onCloseRequest: (result: String?) -> Unit) {
    val scope = rememberCoroutineScope()
    scope.launch((Dispatchers.Swing)) {
        JFileChooser(folder.apply(File::mkdirs).absoluteFile).apply {
            dialogTitle = "Choose output folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            showOpenDialog(null)
            onCloseRequest(selectedFile?.path)
        }
    }
}
