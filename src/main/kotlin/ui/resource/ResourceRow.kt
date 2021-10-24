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
import kotlinx.coroutines.flow.map
import locales.Locale
import locales.LocaleIsoCode
import project.*
import project.ResourceInfo.Type.*
import ui.core.onPressEnter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ResourceRow(vm: ResourceViewModel, resId: ResourceId) {
    val resourceMetadata by vm.resourceMetadata.collectAsState()
    val info by remember { derivedStateOf { resourceMetadata[resId] } }
    val resourceInfo = info ?: return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            val focusManager = LocalFocusManager.current
            var id by remember { mutableStateOf(resId) }
            var error by remember { mutableStateOf("") }

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

        val displayedLocales by vm.displayedLocales.collectAsState(listOf())
        displayedLocales.forEach { locale ->
            Spacer(Modifier.width(8.dp))
            when (resourceInfo.type) {
                STRING -> StringRows(vm, resId, locale)
                PLURAL -> PluralRows(vm, resId, locale)
                ARRAY -> ArrayRows(vm, resId, locale)
            }
        }
        Spacer(Modifier.width(8.dp))

        Platform.values().forEach { platform ->
            val isIncluded = platform in resourceInfo.platforms
            IconButton(onClick = {
                val newResource =
                    resourceInfo.copy(platforms = if (isIncluded) resourceInfo.platforms.minus(platform) else resourceInfo.platforms.plus(platform))
                vm.resourceMetadata.value = resourceMetadata.plus(resId to newResource)
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
fun RowScope.StringRows(vm: ResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {
    val isDefaultLocale by vm.project.map { it.defaultLocale == localeIsoCode }.collectAsState(false)
    val resource by vm.resource(resId, localeIsoCode).map { it as? Str ?: Str() }.collectAsState(Str())
    val focusManager = LocalFocusManager.current
    var oldText = remember { resource.text }
    var newText by remember { mutableStateOf(oldText) }
    OutlinedTextField(
        value = newText,
        onValueChange = { newText = it },
        modifier = Modifier.weight(1f).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
            if (!it.hasFocus && oldText != newText) {
                oldText = newText
                vm.updateResource(localeIsoCode, resId, Str(newText))
            }
        },
        label = { Text(Locale[localeIsoCode].displayName(isDefaultLocale)) },
        singleLine = true
    )
}

@Composable
fun PluralRows(vm: ResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {

}


@Composable
fun ArrayRows(vm: ResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {

}
