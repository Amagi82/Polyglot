package ui.resource

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import locales.LocaleIsoCode
import project.*
import ui.core.DenseTextField
import ui.core.IconButton
import ui.core.focusNextOnEnter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <R : Resource> ResourceRow(vm: ResourceTypeViewModel<R>, displayedLocales: List<LocaleIsoCode>, resId: ResourceId) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EditableIdField(vm, resId)

        when (vm) {
            is StringResourceViewModel -> StringRow(vm, displayedLocales, resId)
            is PluralResourceViewModel -> PluralRow(vm, displayedLocales, resId)
            is ArrayResourceViewModel -> ArrayRow(vm, displayedLocales, resId)
        }
        Spacer(Modifier.width(8.dp))

        PlatformEditor(vm, resId)
    }
    Divider()
}

@Composable
private fun <R : Resource> RowScope.EditableIdField(vm: ResourceTypeViewModel<R>, resId: ResourceId) {
    var editMode by remember { mutableStateOf(false) }
    val idModifier = Modifier.weight(1f).padding(vertical = 4.dp)

    if (editMode) {
        Column(modifier = idModifier) {
            var id by remember { mutableStateOf(resId) }
            var error by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            var hasBeenFocused by remember { mutableStateOf(false) }

            DenseTextField(
                value = id.value,
                onValueChange = {
                    error = ""
                    id = ResourceId(it.dropWhile(Char::isDigit).filter(Char::isLetterOrDigit))
                },
                modifier = Modifier.fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusNextOnEnter()
                    .onFocusChanged {
                        when {
                            it.hasFocus -> Unit
                            !hasBeenFocused -> hasBeenFocused = true
                            resId == id -> editMode = false
                            id.value.isEmpty() -> vm.removeResource(resId)
                            !vm.updateResourceId(resId, id) -> error = "id already exists"
                            else -> Unit
                        }
                    },
                isError = error.isNotEmpty(),
                singleLine = true,
            )

            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colors.error)
            }
        }
    } else {
        Text(resId.value, modifier = idModifier.pointerInput(Unit) { detectTapGestures(onDoubleTap = { editMode = true }) })
    }
}

@Composable
private fun <R : Resource> PlatformEditor(vm: ResourceTypeViewModel<R>, resId: ResourceId) {
    val platforms by vm.platforms(resId).collectAsState(Platform.ALL)
    Platform.values().forEach { platform ->
        IconButton(platform.iconId, Modifier.alpha(if (platform in platforms) 1f else 0.1f), contentDescription = platform.name) {
            vm.togglePlatform(resId, platform)
        }
    }
}

@Composable
private fun RowScope.StringRow(vm: StringResourceViewModel, displayedLocales: List<LocaleIsoCode>, resId: ResourceId) {
    displayedLocales.forEach { localeIsoCode ->
        Spacer(Modifier.width(8.dp))
        val resource by vm.resource(resId, localeIsoCode).map { it ?: Str() }.collectAsState(Str())
        var oldText = remember(resource) { resource.text }
        var newText by remember(resource) { mutableStateOf(oldText) }
        DenseTextField(
            value = newText,
            onValueChange = { newText = it },
            modifier = Modifier.weight(1f).focusNextOnEnter().onFocusChanged {
                if (!it.hasFocus && oldText != newText) {
                    oldText = newText
                    vm.updateResource(localeIsoCode, resId, Str(newText))
                }
            },
            singleLine = true
        )
    }
}

@Composable
private fun RowScope.PluralRow(vm: PluralResourceViewModel, displayedLocales: List<LocaleIsoCode>, resId: ResourceId) {
    displayedLocales.forEach { localeIsoCode ->
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
            val resource by vm.resource(resId, localeIsoCode).map { it ?: Plural() }.collectAsState(Plural())
            Quantity.values().forEach { quantity ->
                var oldText = remember(resource) { resource[quantity] ?: "" }
                var newText by remember(oldText) { mutableStateOf(oldText) }
                DenseTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth().focusNextOnEnter().onFocusChanged {
                        if (!it.hasFocus && oldText != newText) {
                            oldText = newText
                            vm.updateResource(localeIsoCode, resId, Plural(resource.items.plus(quantity to newText)))
                        }
                    },
                    singleLine = true
                )
            }
        }
    }
}


@Composable
private fun RowScope.ArrayRow(vm: ArrayResourceViewModel, displayedLocales: List<LocaleIsoCode>, resId: ResourceId) {
    val oldSize by vm.arraySize(resId).collectAsState(1)
    var newSize by remember(oldSize) { mutableStateOf(oldSize) }
    DenseTextField(
        value = newSize.toString(),
        onValueChange = { newSize = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
        modifier = Modifier.padding(start = 8.dp).width(64.dp).focusNextOnEnter().onFocusChanged {
            if (!it.hasFocus && oldSize != newSize) {
                vm.updateArraySize(resId, newSize)
            }
        },
        label = { Text("Size") },
        singleLine = true
    )
    displayedLocales.forEach { localeIsoCode ->
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
            val size by vm.arraySize(resId).collectAsState(1)
            val items by vm.resource(resId, localeIsoCode).map { it?.items ?: listOf() }.collectAsState(listOf())
            for (index in 0 until size) {
                var oldText = remember(items) { items.getOrElse(index) { "" } }
                var newText by remember(oldText) { mutableStateOf(oldText) }
                DenseTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth().focusNextOnEnter().onFocusChanged {
                        if (!it.hasFocus && oldText != newText) {
                            oldText = newText
                            vm.updateResource(localeIsoCode, resId, StringArray(List(size) { i -> if (i == index) newText else items.getOrElse(i) { "" } }))
                        }
                    },
                    singleLine = true
                )
            }
        }
    }
}
