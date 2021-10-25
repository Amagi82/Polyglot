package ui.resource.backdrop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import locales.Locale
import ui.core.Chip
import ui.resource.ResourceViewModel

@Composable
fun LocaleSettings(vm: ResourceViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val project by vm.project.collectAsState()
        val excludedLocales by vm.excludedLocales.collectAsState()

        Text("Locales", style = MaterialTheme.typography.h6)

        project.locales.forEach { isoCode ->
            val isDefault = isoCode == project.defaultLocale
            var isMenuExpanded by remember { mutableStateOf(false) }
            val isExcluded = isoCode in excludedLocales
            Chip(
                text = { Text(Locale[isoCode].displayName(project.defaultLocale == isoCode)) },
                onClick = {
                    vm.excludedLocales.value = if (isExcluded) vm.excludedLocales.value.minus(isoCode) else vm.excludedLocales.value.plus(isoCode)
                },
                isClickEnabled = !isDefault,
                leadingIcon = {
                    if (isExcluded) Icon(painterResource(R.drawable.visibilityOff), contentDescription = "Hidden")
                    else Icon(painterResource(R.drawable.visibility), contentDescription = "Visible")
                },
                trailingIcon = {
                    if (!isDefault) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.clickable { isMenuExpanded = true })

                        Box {
                            DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                                if (isoCode.isBaseLanguage) {
                                    DropdownMenuItem(onClick = {
                                        vm.project.value = vm.project.value.copy(defaultLocale = isoCode)
                                    }) { Text("Make default") }
                                }
                                DropdownMenuItem(onClick = { vm.deleteLocale(isoCode) }) { Text("Delete") }
                            }
                        }
                    }
                })
        }
        AddLocaleDropdown(vm)
    }
}
