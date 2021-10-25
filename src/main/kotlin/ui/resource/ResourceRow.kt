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

        if (metadata is StringArray.Metadata && resourceVM is ArrayResourceViewModel) {
            var size by remember(metadata) { mutableStateOf(metadata.size) }
            OutlinedTextField(
                value = size.toString(),
                onValueChange = { size = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                modifier = Modifier.padding(start = 8.dp).width(72.dp).onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && metadata.size != size) {
                        resourceVM.updateArraySize(resId, metadata.copy(size = size))
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
                is PluralResourceViewModel -> PluralFields(resourceVM, resId, locale, metadata as Plural.Metadata)
                is ArrayResourceViewModel -> ArrayFields(resourceVM, resId, locale, metadata as StringArray.Metadata)
            }
        }
        Spacer(Modifier.width(8.dp))

        Platform.values().forEach { platform ->
            IconButton(platform.iconId, Modifier.alpha(if (platform in metadata.platforms) 1f else 0.1f), contentDescription = platform.name) {
                resourceVM.togglePlatform(resId, metadata, platform)
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
fun RowScope.PluralFields(vm: PluralResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode, metadata: Plural.Metadata) {
    Column(modifier = Modifier.weight(1f)) {
        val isDefaultLocale by vm.project.map { it.defaultLocale == localeIsoCode }.collectAsState(false)
        val resource by vm.resource(resId, localeIsoCode).map { it ?: Plural() }.collectAsState(Plural())
        val focusManager = LocalFocusManager.current

        Text(Locale[localeIsoCode].displayName(isDefaultLocale))
        metadata.quantities.forEach { quantity ->
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
fun RowScope.ArrayFields(vm: ArrayResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode, metadata: StringArray.Metadata) {
    val isDefaultLocale by vm.project.map { it.defaultLocale == localeIsoCode }.collectAsState(false)
    val focusManager = LocalFocusManager.current
    val resource by vm.resource(resId, localeIsoCode).map { it ?: StringArray(List(metadata.size) { "" }) }
        .collectAsState(StringArray(List(metadata.size) { "" }))

    Column(modifier = Modifier.weight(1f)) {
        for (index in 0 until metadata.size) {
            var oldText = remember(resource) { resource.items.getOrElse(index) { "" } }
            var newText by remember(oldText) { mutableStateOf(oldText) }
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                modifier = Modifier.fillMaxWidth().onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }.onFocusChanged {
                    if (!it.hasFocus && oldText != newText) {
                        oldText = newText
                        vm.updateResource(localeIsoCode, resId, StringArray(resource.items.toMutableList().apply { set(index, newText) }.toList()))
                    }
                },
                label = { Text(Locale[localeIsoCode].displayName(isDefaultLocale)) },
                singleLine = true
            )
        }
    }
}
