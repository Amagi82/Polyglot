package ui.resource

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import locales.Locale
import locales.LocaleIsoCode
import project.*
import ui.core.onPressEnter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ResourceRow(
    vm: ResourceViewModel,
    resId: ResourceId
) {
    val project by vm.project.collectAsState()
    val resourceMetadata by vm.resourceMetadata.collectAsState()
    val localizedResources by vm.includedResourcesByLocale.collectAsState(sortedMapOf())
    val info by remember { derivedStateOf { resourceMetadata[resId] } }
    val resourceInfo = info ?: return

    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        if (!vm.updateResourceId(resId, id)) {
                            error = "id already exists"
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

        localizedResources.forEach { (localeIsoCode, resources) ->
            Spacer(Modifier.width(8.dp))
            val resource = resources[id] ?: when (resourceInfo.type) {
                ResourceInfo.Type.STRING -> Str("")
                ResourceInfo.Type.PLURAL -> Plural(one = null, other = "")
                ResourceInfo.Type.ARRAY -> StringArray(listOf())
            }
            when (resource) {
                is Str -> StringRows(resId, project.defaultLocale, localeIsoCode, resource, vm::updateResource)
                is Plural -> PluralRows(resId, project.defaultLocale, localeIsoCode, resource, vm::updateResource)
                is StringArray -> ArrayRows(resId, project.defaultLocale, localeIsoCode, resource, vm::updateResource)
            }
        }

        Platform.values().forEach { platform ->
            val isIncluded = platform in resourceInfo.platforms
            IconButton(onClick = {
                val newResource =
                    resourceInfo.copy(platforms = if (isIncluded) resourceInfo.platforms.minus(platform) else resourceInfo.platforms.plus(platform))
                vm.resourceMetadata.value = resourceMetadata.plus(id to newResource)
            }) {
                Icon(
                    painterResource(platform.iconId),
                    contentDescription = platform.name,
                    tint = if (isIncluded) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                )
            }
        }
//        val menuState by vm.menuState.collectAsState()
//        if (menuState == MenuState.SETTINGS) {
//            IconButton(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.padding(start = 16.dp), onClick = deleteResource)
//        }
    }
    Divider()
}

@Composable
fun RowScope.StringRows(resId: ResourceId, defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, string: Str, update: (LocaleIsoCode, ResourceId, Str) -> Unit) {
    val focusManager = LocalFocusManager.current
    var oldText = remember { string.text }
    var newText by remember { mutableStateOf(oldText) }
    OutlinedTextField(
        value = newText,
        onValueChange = { newText = it },
        modifier = Modifier.weight(1f).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
            if (!it.hasFocus && oldText != newText) {
                oldText = newText
                update(localeIsoCode, resId, Str(newText))
            }
        },
        label = { Text(Locale[localeIsoCode].displayName(localeIsoCode == defaultLocale)) },
        singleLine = true
    )
}

@Composable
fun PluralRows(resId: ResourceId, defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, plural: Plural, update: (LocaleIsoCode, ResourceId, Plural) -> Unit) {

}


@Composable
fun ArrayRows(resId: ResourceId, defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, array: StringArray, update: (LocaleIsoCode, ResourceId, StringArray) -> Unit) {

}
