package ui.resource.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import locales.Locale
import locales.LocaleIsoCode
import ui.core.onPressEnter
import ui.core.outlinedTextFieldColorsOnPrimary
import ui.resource.ResourceViewModel

@Composable
fun AddLocaleDropdown(vm: ResourceViewModel) {
    val projectLocales by vm.projectLocales.collectAsState(listOf())
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

    fun addLocale(isoCode: LocaleIsoCode? = locales.find { it.isoCode.value == newLocaleText || it.displayName() == newLocaleText }?.isoCode) {
        if (isoCode != null && isoCode !in projectLocales && locales.any { it.isoCode == isoCode }) {
            vm.addLocale(isoCode)
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
            modifier = Modifier.padding(top = 16.dp).onPressEnter { addLocale(); true },
            label = { Text("Add locale") },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColorsOnPrimary()
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
}
