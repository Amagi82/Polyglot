package ui.resources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import locales.Locale
import locales.LocaleIsoCode
import project.LocalizedResources
import project.Platform
import project.Project
import project.save
import ui.core.Chip
import ui.core.onPressEnter
import java.io.File
import javax.swing.JFileChooser

@Composable
fun ProjectSettings(project: MutableState<Project>, localizedResources: MutableState<LocalizedResources>, onClose: () -> Unit) {
    Column(Modifier.padding(16.dp).width(400.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Settings", style = MaterialTheme.typography.h6)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close settings menu")
            }
        }

        OutputFileSelectionButton(Platform.ANDROID.displayName, project.value.androidOutputUrl) {
            project.value = project.value.copy(androidOutputUrl = it)
        }
        OutputFileSelectionButton(Platform.IOS.displayName, project.value.iosOutputUrl) {
            project.value = project.value.copy(iosOutputUrl = it)
        }

        val locales = remember { Locale.all.values }
        var newLocaleText by remember { mutableStateOf("") }
        var isDropdownExpanded by remember { mutableStateOf(false) }
        val filteredLocales by remember(newLocaleText) {
            derivedStateOf {
                locales.filter { locale ->
                    locale.isoCode.value.startsWith(newLocaleText, ignoreCase = true) ||
                            locale.displayName().startsWith(newLocaleText, ignoreCase = true)
                }
            }
        }
        var projectLocales by remember { mutableStateOf(localizedResources.value.keys) }

        fun addLocale(isoCode: LocaleIsoCode? = locales.find { it.isoCode.value == newLocaleText || it.displayName() == newLocaleText }?.isoCode) {
            if (isoCode != null && isoCode !in projectLocales && locales.any { it.isoCode == isoCode }) {
                val newLocales = projectLocales.toMutableSet()
                if (!isoCode.isBaseLanguage) {
                    newLocales.add(Locale[isoCode].copy(region = null).isoCode)
                }
                newLocales.add(isoCode)

                projectLocales = newLocales.sortedBy { Locale[it].displayName() }.toSet()
                localizedResources.value = localizedResources.value.plus(newLocales.map { it to mapOf() }).apply { save(project.value.name) }
                newLocaleText = ""
                isDropdownExpanded = false
            }
        }

        Box {
            OutlinedTextField(
                value = newLocaleText,
                onValueChange = {
                    newLocaleText = it
                    isDropdownExpanded = it.isNotBlank()
                },
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().onPressEnter { addLocale(); true },
                label = { Text("Add locale") },
                singleLine = true
            )
            DropdownMenu(
                expanded = isDropdownExpanded && filteredLocales.isNotEmpty(),
                onDismissRequest = { isDropdownExpanded = false },
                focusable = false
            ) {
                filteredLocales.take(15).forEach { locale ->
                    DropdownMenuItem(onClick = { addLocale(locale.isoCode) }) {
                        Text(locale.displayName())
                    }
                }
            }
        }

        projectLocales.forEach { isoCode ->
            val isDefault = isoCode == project.value.defaultLocale
            Chip(
                text = {
                    Text(
                        Locale[isoCode].displayName(project.value.defaultLocale == isoCode),
                        color = if (isDefault) MaterialTheme.colors.onSecondary else Color.Unspecified,
                        style = MaterialTheme.typography.body2
                    )
                },
                modifier = Modifier.clickable(enabled = !isDefault && isoCode.isBaseLanguage) { project.value = project.value.copy(defaultLocale = isoCode) },
                color = if (isDefault) MaterialTheme.colors.secondary else Color.Unspecified,
                trailingIcon = {
                    if (!isDefault) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove ${Locale[isoCode].displayName()}",
                            modifier = Modifier.padding(end = 8.dp).size(18.dp).clickable {
                                projectLocales = projectLocales.minus(isoCode)
                                localizedResources.value = localizedResources.value.minus(isoCode).apply { save(project.value.name) }
                            })
                    }
                })
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
        modifier = Modifier.fillMaxWidth().clickable { isFileDialogOpen = true },
        enabled = false,
        label = { Text("$platformName output") },
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColors()
    )
}

@Composable
private fun FileDialog(folder: File, onCloseRequest: (result: String?) -> Unit) {
    val scope = rememberCoroutineScope()
    scope.launch(Dispatchers.Swing) {
        JFileChooser(folder.apply(File::mkdirs).absoluteFile).apply {
            dialogTitle = "Choose output folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            showOpenDialog(null)

            onCloseRequest(selectedFile?.path)
        }
    }
}
