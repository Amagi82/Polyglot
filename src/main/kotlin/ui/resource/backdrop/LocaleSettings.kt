package ui.resource.backdrop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import locales.Locale
import ui.core.Chip
import ui.resource.ResourceViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LocaleSettings(vm: ResourceViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val locales by vm.locales.collectAsState()
        val defaultLocale by vm.defaultLocale.collectAsState()
        val excludedLocales by vm.excludedLocales.collectAsState()

        Text("Locales", style = MaterialTheme.typography.h6)

        locales.forEach { isoCode ->
            val isDefault = isoCode == defaultLocale
            var isMenuExpanded by remember { mutableStateOf(false) }
            val isExcluded = isoCode in excludedLocales
            Chip(
                text = { Text(Locale[isoCode].displayName(defaultLocale == isoCode)) },
                onClick = { vm.excludedLocales.value = if (isExcluded) excludedLocales.minus(isoCode) else excludedLocales.plus(isoCode) },
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
                                        vm.defaultLocale.value = isoCode
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
