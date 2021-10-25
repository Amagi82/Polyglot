package ui.resource

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import locales.Locale
import locales.LocaleIsoCode
import project.*
import ui.core.IconButton
import ui.core.onPressEnter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <M : Metadata, R : Resource<M>> ResourceRow(vm: ResourceViewModel, resourceVM: ResourceTypeViewModel<M, R>, resId: ResourceId, metadata: M) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            val focusManager = LocalFocusManager.current
            var id by remember { mutableStateOf(resId) }
            var error by remember { mutableStateOf("") }

            OutlinedTextField(
                value = id.value,
                onValueChange = {
                    error = ""
                    id = ResourceId(it.dropWhile(Char::isDigit).filter(Char::isLetterOrDigit))
                },
                modifier = Modifier.padding(vertical = 4.dp).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && resId != id) {
                        if (id.value.isEmpty()) {
                            resourceVM.removeResource(resId)
                        } else if (!resourceVM.updateResourceId(resId, id)) {
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
            when (resourceVM) {
                is StringResourceViewModel -> StringField(resourceVM, resId, locale)
                is PluralResourceViewModel -> PluralFields(resourceVM, resId, locale)
                is ArrayResourceViewModel -> ArrayFields(resourceVM, resId, locale)
            }
        }
        Spacer(Modifier.width(8.dp))

        Platform.values().forEach { platform ->
            IconButton(platform.iconId, Modifier.alpha(if (platform in metadata.platforms) 1f else 0.1f), contentDescription = platform.name) {
                resourceVM.togglePlatform(resId, metadata, platform)
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
fun RowScope.StringField(vm: StringResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {
    val isDefaultLocale by vm.project.map { it.defaultLocale == localeIsoCode }.collectAsState(false)
    val resource by vm.resource(resId, localeIsoCode).map { it ?: Str() }.collectAsState(Str())
    val focusManager = LocalFocusManager.current
    var oldText = remember(resource) { resource.text }
    var newText by remember(resource) { mutableStateOf(oldText) }
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
fun PluralFields(vm: PluralResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {

}


@Composable
fun ArrayFields(vm: ArrayResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {

}
