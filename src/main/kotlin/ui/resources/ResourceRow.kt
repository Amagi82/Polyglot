package ui.resources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
    project: Project,
    resId: ResourceId,
    resources: MutableState<Resources>,
    localizedResources: MutableState<LocalizedResources>,
    excludedLocales: Set<LocaleIsoCode>,
    defaultLocale: LocaleIsoCode,
    deleteResource: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        var id by remember { mutableStateOf(resId) }
        var error by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current

        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = id.id,
                onValueChange = {
                    error = ""
                    id = ResourceId(it)
                },
                modifier = Modifier.padding(vertical = 4.dp).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && resId != id) {

                        if (id in resources.value) {
                            error = "id already exists"
                        } else {
                            resources.value = resources.value.toMutableMap().apply {
                                val resource = this[resId]!!
                                remove(resId)
                                put(id, resource)
                                scope.launch {
                                    save(project.name)
                                }
                            }
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

        Column(Modifier.weight(1f)) {
            val resource by remember { derivedStateOf { resources.value[id]!! } }
            localizedResources.value.keys.filter { it !in excludedLocales }.forEach { localeIsoCode ->
                Row(
                    Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val localization = localizedResources.value[localeIsoCode]?.get(id)
                    when (resource.type) {
                        Resource.Type.STRING -> StringRows(defaultLocale, localeIsoCode, localization as? Str ?: Str("")) {
                            localizedResources.value =
                                localizedResources.value.plus(localeIsoCode to localizedResources.value[localeIsoCode]!!.plus(id to it)).apply {
                                    scope.launch {
                                        save(project.name)
                                    }
                                }
                        }
                        Resource.Type.PLURAL -> PluralRows(defaultLocale, localeIsoCode, localization as? Plural ?: Plural(one = null, other = "")) {

                        }
                        Resource.Type.ARRAY -> ArrayRows(defaultLocale, localeIsoCode, localization as? StringArray ?: StringArray(listOf())) {

                        }
                    }
                }
            }
        }

        val resource = resources.value[id]!!

        Platform.values().forEach { platform ->
            val isIncluded = platform in resource.platforms
            IconButton(onClick = {
                val newResource = resource.copy(platforms = if (isIncluded) resource.platforms.minus(platform) else resource.platforms.plus(platform))
                resources.value = resources.value.plus(id to newResource).apply {
                    scope.launch {
                        save(project.name)
                    }
                }
            }) {
                if (isIncluded) {
                    Icon(painterResource(platform.iconId), contentDescription = platform.name)
                }
            }
        }
        IconButton(onClick = deleteResource, modifier = Modifier.padding(start = 16.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove")
        }
    }
    Divider()
}

@Composable
fun StringRows(defaultLocale: LocaleIsoCode, localeIsoCode: LocaleIsoCode, string: Str, update: (Str) -> Unit) {
    val focusManager = LocalFocusManager.current
    var oldText = remember { string.text }
    var newText by remember { mutableStateOf(oldText) }
    OutlinedTextField(
        value = newText,
        onValueChange = { newText = it },
        modifier = Modifier.onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
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
