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
fun <R : Resource> ResourceRow(vm: ResourceViewModel, resourceVM: ResourceTypeViewModel<R>, resId: ResourceId) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val focusManager = LocalFocusManager.current
        Column(modifier = Modifier.weight(1f)) {
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

        if (resourceVM is ArrayResourceViewModel) {
            val oldSize by resourceVM.arraySize(resId).collectAsState(1)
            var newSize by remember(oldSize) { mutableStateOf(oldSize) }
            OutlinedTextField(
                value = newSize.toString(),
                onValueChange = { newSize = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                modifier = Modifier.padding(start = 8.dp).width(72.dp).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && oldSize != newSize) {
                        resourceVM.updateArraySize(resId, newSize)
                    }
                },
                label = { Text("Size") },
                singleLine = true
            )
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

        val platforms by resourceVM.platforms(resId).collectAsState(Platform.ALL)
        Platform.values().forEach { platform ->
            IconButton(platform.iconId, Modifier.alpha(if (platform in platforms) 1f else 0.1f), contentDescription = platform.name) {
                resourceVM.togglePlatform(resId, platform)
            }
        }
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
fun RowScope.PluralFields(vm: PluralResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {
    Column(modifier = Modifier.weight(1f)) {
        val isDefaultLocale by vm.project.map { it.defaultLocale == localeIsoCode }.collectAsState(false)
        val resource by vm.resource(resId, localeIsoCode).map { it ?: Plural() }.collectAsState(Plural())
        val focusManager = LocalFocusManager.current

        Text(Locale[localeIsoCode].displayName(isDefaultLocale))
        Quantity.values().forEach { quantity ->
            var oldText = remember(resource) { resource[quantity] ?: "" }
            var newText by remember(oldText) { mutableStateOf(oldText) }
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                modifier = Modifier.fillMaxWidth().onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && oldText != newText) {
                        oldText = newText
                        vm.updateResource(localeIsoCode, resId, Plural(resource.items.plus(quantity to newText)))
                    }
                },
                label = { Text(quantity.label) },
                singleLine = true
            )
        }
    }
}


@Composable
fun RowScope.ArrayFields(vm: ArrayResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {
    val isDefaultLocale by vm.project.map { it.defaultLocale == localeIsoCode }.collectAsState(false)
    val focusManager = LocalFocusManager.current
    val size by vm.arraySize(resId).collectAsState(1)
    val items by vm.resource(resId, localeIsoCode).map { it?.items ?: listOf() }.collectAsState(listOf())

    Column(modifier = Modifier.weight(1f)) {
        for (index in 0 until size) {
            var oldText = remember(items) { items.getOrElse(index) { "" } }
            var newText by remember(oldText) { mutableStateOf(oldText) }
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                modifier = Modifier.fillMaxWidth().onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && oldText != newText) {
                        oldText = newText
                        vm.updateResource(localeIsoCode, resId, StringArray(List(size) { i -> if (i == index) newText else items.getOrElse(i) { "" } }))
                    }
                },
                label = { Text(Locale[localeIsoCode].displayName(isDefaultLocale)) },
                singleLine = true
            )
        }
    }
}
