package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.PolyglotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import locales.Locale
import locales.LocaleIsoCode
import project.Platform
import project.Project
import project.ResourceType
import sqldelight.ArrayLocalizations
import sqldelight.PluralLocalizations
import sqldelight.StringLocalizations
import ui.utils.onPressEnter
import java.io.File
import javax.swing.JFileChooser


@Composable
fun ProjectSettings(db: PolyglotDatabase, project: Project, updateProject: (Project) -> Unit, onClose: () -> Unit) {
    Column(Modifier.padding(16.dp).width(400.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val scope = rememberCoroutineScope()

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Settings", style = MaterialTheme.typography.h6)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close settings menu")
            }
        }

        OutputFileSelectionButton(Platform.ANDROID.displayName, project.androidOutputUrl) {
            updateProject(project.copy(androidOutputUrl = it))
        }
        OutputFileSelectionButton(Platform.IOS.displayName, project.iosOutputUrl) {
            updateProject(project.copy(iosOutputUrl = it))
        }

        val locales = remember { Locale.all.keys }
        var newLocale by remember { mutableStateOf(LocaleIsoCode("")) }
        var isDropdownExpanded by remember { mutableStateOf(false) }
        val filteredLocales by remember(newLocale) { derivedStateOf { locales.filter { it.value.startsWith(newLocale.value, ignoreCase = true) } } }
        var projectLocales by remember { mutableStateOf(project.locales) }

        Box {
            OutlinedTextField(
                value = newLocale.value,
                onValueChange = {
                    newLocale = LocaleIsoCode(it)
                    isDropdownExpanded = it.isNotBlank()
                },
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().onPressEnter {
                    if (newLocale !in projectLocales && newLocale in locales) {
                        projectLocales = projectLocales.plus(newLocale)
                        addLocale(db, project = project, locale = newLocale, updateProject = updateProject)
                    }
                    true
                },
                label = { Text("Add locale") },
                trailingIcon = {
                    if (newLocale !in projectLocales && newLocale in locales) {
                        IconButton(onClick = {
                            projectLocales = projectLocales.plus(newLocale)
                            addLocale(db, project = project, locale = newLocale, updateProject = updateProject)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add locale")
                        }
                    }
                },
                singleLine = true
            )
            DropdownMenu(
                expanded = isDropdownExpanded && filteredLocales.isNotEmpty(),
                onDismissRequest = { isDropdownExpanded = false },
                focusable = false
            ) {
                filteredLocales.take(15).forEach { isoCode ->
                    DropdownMenuItem(onClick = {
                        isDropdownExpanded = false
                        newLocale = isoCode
                    }) {
                        Text(isoCode.value)
                    }
                }
            }
        }

        projectLocales.forEach { isoCode ->
            Chip(Locale[isoCode].displayName(project.defaultLocale == isoCode), hasClose = isoCode != project.defaultLocale, close = {
                projectLocales = projectLocales.minus(isoCode)
                scope.launch { deleteLocale(db = db, project = project, locale = isoCode, updateProject = updateProject) }
            })
        }
    }
}


private fun addLocale(db: PolyglotDatabase, project: Project, locale: LocaleIsoCode, updateProject: (Project) -> Unit) {
    db.transaction {
        val resourceList = db.resourceQueries.selectAll().executeAsList()
        resourceList.forEach {
            when (it.type) {
                ResourceType.STRING -> db.stringLocalizationsQueries.insert(StringLocalizations(resId = it.id, locale = locale, text = ""))
                ResourceType.PLURAL -> db.pluralLocalizationsQueries.insert(
                    PluralLocalizations(
                        resId = it.id,
                        locale = locale,
                        zero = null,
                        one = "",
                        two = null,
                        few = null,
                        many = null,
                        other = ""
                    )
                )
                ResourceType.ARRAY -> db.arrayLocalizationsQueries.insert(
                    ArrayLocalizations(resId = it.id, locale = locale, array = listOf())
                )
            }
        }
    }
    updateProject(project.copy(locales = project.locales.plus(locale).distinct().sortedBy { it.value }))
}

private fun deleteLocale(db: PolyglotDatabase, project: Project, locale: LocaleIsoCode, updateProject: (Project) -> Unit) {
    db.transaction {
        db.arrayLocalizationsQueries.deleteLocale(locale)
        db.pluralLocalizationsQueries.deleteLocale(locale)
        db.stringLocalizationsQueries.deleteLocale(locale)
    }
    updateProject(project.copy(locales = project.locales.minus(locale)))
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
