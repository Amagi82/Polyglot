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
fun <R : Resource> ResourceRow(vm: ResourceViewModel, resourceVM: ResourceTypeViewModel<R>, resId: ResourceId) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                id.value.isEmpty() -> resourceVM.removeResource(resId)
                                !resourceVM.updateResourceId(resId, id) -> error = "id already exists"
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

        if (resourceVM is ArrayResourceViewModel) {
            val oldSize by resourceVM.arraySize(resId).collectAsState(1)
            var newSize by remember(oldSize) { mutableStateOf(oldSize) }
            DenseTextField(
                value = newSize.toString(),
                onValueChange = { newSize = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                modifier = Modifier.padding(start = 8.dp).width(64.dp).focusNextOnEnter().onFocusChanged {
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

@Composable
fun RowScope.PluralFields(vm: PluralResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {
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


@Composable
fun RowScope.ArrayFields(vm: ArrayResourceViewModel, resId: ResourceId, localeIsoCode: LocaleIsoCode) {
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
