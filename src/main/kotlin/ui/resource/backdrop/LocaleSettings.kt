package ui.resource.backdrop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import data.SettingsStore
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import translation.CloudTranslateApi
import ui.core.Chip
import ui.core.onPressEnter
import ui.core.onPressEsc
import ui.core.outlinedTextFieldColorsOnPrimary
import ui.resource.ResourceViewModel

@Composable
fun LocaleSettings(vm: ResourceViewModel, showSnackbar: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val locales by vm.locales.collectAsState()
        val defaultLocale by vm.defaultLocale.collectAsState()
        val excludedLocales by vm.excludedLocales.collectAsState()
        val apiKey by SettingsStore.apiKey.collectAsState()
        var translatableLanguages by remember { mutableStateOf(listOf<LocaleIsoCode>()) }

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

                                AutoTranslateDropDown(
                                    vm = vm,
                                    apiKey = apiKey,
                                    defaultLocale = defaultLocale,
                                    isoCode = isoCode,
                                    translatableLanguages = translatableLanguages
                                )

                                DropdownMenuItem(onClick = { vm.deleteLocale(isoCode) }) { Text("Delete") }
                            }
                        }
                    }
                })
        }
        AddLocaleDropdown(vm)

        val scope = rememberCoroutineScope()
        apiKey?.let { key ->
            LaunchedEffect(key) {
                scope.launch {
                    CloudTranslateApi.supportedLanguages(key)
                        .onSuccess { response -> translatableLanguages = response.data.languages.map { LocaleIsoCode(it.language) } }
                        .onFailure { showSnackbar(it.localizedMessage) }
                }
            }
        }

        var key by remember(apiKey) { mutableStateOf(apiKey.orEmpty()) }
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            modifier = Modifier.padding(top = 8.dp)
                .onPressEnter { SettingsStore.apiKey.value = key; focusManager.clearFocus() }
                .onPressEsc { key = apiKey.orEmpty(); focusManager.clearFocus() },
            label = { Text("Google Translate API key") },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColorsOnPrimary()
        )
    }
}
