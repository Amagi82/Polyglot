package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.PolyglotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import locales.LocaleIsoCode
import project.Platform
import project.Project
import project.ResourceType
import sqldelight.ArrayLocalizations
import sqldelight.PluralLocalizations
import sqldelight.StringLocalizations
import ui.utils.onPressEnter
import java.io.File
import java.util.*
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

        Text("Add/Remove Locales", modifier = Modifier.padding(vertical = 8.dp))

        var projectLocales by remember { mutableStateOf(project.locales) }
        projectLocales.forEach { isoCode ->
            Chip(isoCode.value, hasClose = isoCode != project.defaultLocale, close = {
                scope.launch { deleteLocale(db = db, project = project, locale = isoCode, updateProject = updateProject) }
                projectLocales = projectLocales.minus(isoCode)
            })
        }

        val focusManager = LocalFocusManager.current
        val locales = remember { Locale.getAvailableLocales().sortedBy { it.toString() } }
        var newLanguage by remember { mutableStateOf("") }
        var newRegion by remember { mutableStateOf("") }

        Box {
            val languages = remember { locales.filter { it.country.isEmpty() } }
            val filteredLanguages by remember(newLanguage) {
                derivedStateOf {
                    languages.filter {
                        it.language.startsWith(newLanguage, ignoreCase = true) || it.displayLanguage.startsWith(newLanguage, ignoreCase = true)
                    }
                }
            }

            var isDropdownExpanded by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = newLanguage,
                onValueChange = {
                    newLanguage = it
                    isDropdownExpanded = it.isNotEmpty()
                },
                modifier = Modifier.onPressEnter {
                    languages.find { locale ->
                        locale.language.equals(newLanguage, ignoreCase = true) || locale.displayLanguage.equals(newLanguage, ignoreCase = true)
                    }?.let {
                        focusManager.moveFocus(FocusDirection.Down)
                        isDropdownExpanded = false
                    }
                    false
                },
                label = { Text("Language") },
                singleLine = true
            )

            DropdownMenu(
                expanded = isDropdownExpanded && filteredLanguages.isNotEmpty(),
                onDismissRequest = { isDropdownExpanded = false },
                focusable = false
            ) {
                filteredLanguages.forEach { language ->
                    DropdownMenuItem(onClick = {
                        isDropdownExpanded = false
                        newLanguage = language.displayLanguage
                    }) {
                        Text(language.displayLanguage)
                    }
                }
            }
        }
        val regionsForLanguage by remember(newLanguage) { derivedStateOf { locales.filter { it.displayLanguage == newLanguage } } }
        if (regionsForLanguage.isNotEmpty()) {
            Box {
                var isDropdownExpanded by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = newRegion,
                    onValueChange = {
                        newRegion = it
                        isDropdownExpanded = true
                    },
                    modifier = Modifier.onPressEnter {
                        regionsForLanguage.find { locale ->
                            locale.language.equals(newLanguage, ignoreCase = true) || locale.displayLanguage.equals(newLanguage, ignoreCase = true)
                        }?.let {
                            focusManager.moveFocus(FocusDirection.Down)
                            isDropdownExpanded = false
                        }
                        false
                    },
                    label = { Text("Region") },
                    singleLine = true
                )

                val filteredRegionsForLanguage by remember {
                    derivedStateOf {
                        regionsForLanguage.filter {
                            (it.country.startsWith(newRegion, ignoreCase = true) || it.displayCountry.startsWith(newRegion, ignoreCase = true)) &&
                                    LocaleIsoCode(it.toString()) !in project.locales
                        }
                    }
                }
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    focusable = false
                ) {
                    filteredRegionsForLanguage.forEach { locale ->
                        DropdownMenuItem(onClick = {
                            isDropdownExpanded = false
                            newRegion = locale.displayCountry
                        }) {
                            Text(locale.displayCountry)
                        }
                    }
                }
            }
        }

        IconButton(onClick = {
        }) {
            Icon(Icons.Default.Add, contentDescription = "Add locale")
        }
    }
}


private fun addLocale(db: PolyglotDatabase, project: Project, locale: LocaleIsoCode, updateProject: (Project) -> Unit) {
    db.transaction {
        val resourceList = db.resourceQueries.selectAll().executeAsList()
        resourceList.forEach {
            when (it.type) {
                ResourceType.STRING -> db.stringLocalizationsQueries.insert(
                    StringLocalizations(resId = it.id, locale = locale, text = "")
                )
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
fun Chip(text: String, hasClose: Boolean, close: () -> Unit) {
    Row(
        modifier = Modifier.height(32.dp)
            .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.body2)
        if (hasClose) {
            IconButton(onClick = close) {
                Icon(Icons.Default.Close, contentDescription = "Remove $text", modifier = Modifier.size(18.dp))
            }
        }
    }
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
