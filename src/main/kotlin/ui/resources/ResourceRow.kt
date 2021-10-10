package ui.resources

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.*
import ui.core.onPressEnter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ResourceRow(
    vm: ResourceViewModel,
    resId: ResourceId,
    deleteResource: () -> Unit
) {
    val project by vm.project.collectAsState()
    val resources by vm.resources.collectAsState()
    val localizedResources by vm.localizedResources.collectAsState()
    val resource by remember { derivedStateOf { resources[resId]!! } }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        var error by remember { mutableStateOf("") }
        var id by remember { mutableStateOf(resId) }
        val focusManager = LocalFocusManager.current

        Column(modifier = Modifier.weight(1f)) {

            OutlinedTextField(
                value = id.id,
                onValueChange = {
                    error = ""
                    id = ResourceId(it.dropWhile(Char::isDigit).filter(Char::isLetterOrDigit))
                },
                modifier = Modifier.padding(vertical = 4.dp).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && resId != id) {
                        if (id in resources) {
                            error = "id already exists"
                        } else {
                            vm.resources.value = resources.toMutableMap().apply {
                                put(id, remove(resId)!!)
                            }.toSortedMap()
                        }
                    }
                },
                label = { Text("id") },
                isError = error.isNotEmpty(),
                singleLine = true
            )
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colors.error)
            }
        }

        val excludedLocales by vm.excludedLocales.collectAsState()
        localizedResources.keys.filter { it !in excludedLocales }.forEach { localeIsoCode ->
            val localization = localizedResources[localeIsoCode]?.get(id)
            when (resource.type) {
                Resource.Type.STRING -> StringRows(project.defaultLocale, localeIsoCode, localization as? Str ?: Str("")) {
                    vm.localizedResources.value = localizedResources.plus(localeIsoCode to localizedResources[localeIsoCode]!!.plus(id to it)).toSortedMap()
                }
                Resource.Type.PLURAL -> PluralRows(project.defaultLocale, localeIsoCode, localization as? Plural ?: Plural(one = null, other = "")) {

                }
                Resource.Type.ARRAY -> ArrayRows(project.defaultLocale, localeIsoCode, localization as? StringArray ?: StringArray(listOf())) {

                }
            }
        }

        Platform.values().forEach { platform ->
            val isIncluded = platform in resource.platforms
            IconButton(onClick = {
                val newResource = resource.copy(platforms = if (isIncluded) resource.platforms.minus(platform) else resource.platforms.plus(platform))
                vm.resources.value = resources.plus(id to newResource).toSortedMap()
            }) {
                if (isIncluded) {
                    Icon(painterResource(platform.iconId), contentDescription = platform.name)
                }
            }
        }
        val menuState by vm.menuState.collectAsState()
        if (menuState == MenuState.SETTINGS) {
            IconButton(onClick = deleteResource, modifier = Modifier.padding(start = 16.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
    Divider()
}

@Composable
fun RowScope.StringRows(defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, string: Str, update: (Str) -> Unit) {
    val focusManager = LocalFocusManager.current
    var oldText = remember { string.text }
    var newText by remember { mutableStateOf(oldText) }
    OutlinedTextField(
        value = newText,
        onValueChange = { newText = it },
        modifier = Modifier.weight(1f).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
            if (!it.hasFocus && oldText != newText) {
                oldText = newText
                update(Str(newText))
            }
        },
        label = { Text(Locale[localeIsoCode].displayName(localeIsoCode == defaultLocale)) },
        singleLine = true
    )
}

@Composable
fun PluralRows(defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, plural: Plural, update: (Plural) -> Unit) {

}


@Composable
fun ArrayRows(defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, array: StringArray, update: (StringArray) -> Unit) {

}
