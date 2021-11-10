package ui.resource

import R
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults.BackgroundOpacity
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import locales.LocaleIsoCode
import project.*
import ui.core.DenseTextField
import ui.core.IconButton
import ui.core.onPressEnter

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
private fun DoubleTapToEditDenseTextField(
    text: @Composable (modifier: Modifier) -> Unit,
    textField: @Composable (modifier: Modifier) -> Unit,
    shouldDropFocus: () -> Boolean
) {
    var editMode by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    if (editMode) {
        val focusRequester = remember { FocusRequester() }
        var hasBeenFocused by remember { mutableStateOf(false) }

        textField(Modifier.focusRequester(focusRequester)
            .onPressEnter { if (shouldDropFocus()) focusManager.clearFocus() }
            .onFocusChanged {
                when {
                    it.hasFocus -> Unit
                    !hasBeenFocused -> hasBeenFocused = true
                    shouldDropFocus() -> editMode = false
                    else -> Unit
                }
            })
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    } else {
        text(Modifier.pointerInput(Unit) { detectTapGestures(onDoubleTap = { editMode = true }) })
    }
}

@Composable
private fun <R : Resource> RowScope.EditableIdField(vm: ResourceTypeViewModel<R>, resId: ResourceId) {
    val idModifier = Modifier.weight(1f).padding(vertical = 4.dp)
    var id by remember { mutableStateOf(resId) }
    var error by remember { mutableStateOf("") }
    DoubleTapToEditDenseTextField(
        text = { Text(resId.value, modifier = idModifier.then(it)) },
        textField = { modifier ->
            Column(modifier = idModifier) {
                DenseTextField(
                    value = id.value,
                    onValueChange = {
                        error = ""
                        id = ResourceId(it.dropWhile(Char::isDigit).filter(Char::isLetterOrDigit))
                    },
                    modifier = Modifier.fillMaxWidth().then(modifier),
                    isError = error.isNotEmpty(),
                    singleLine = true,
                )

                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colors.error)
                }
            }
        },
        shouldDropFocus = {
            when {
                resId == id -> true
                id.value.isEmpty() -> {
                    vm.removeResource(resId)
                    false
                }
                !vm.updateResourceId(resId, id) -> {
                    error = "id already exists"
                    false
                }
                else -> false
            }
        })
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
        var text by remember(resource) { mutableStateOf(resource.text) }
        val isError = localeIsoCode == displayedLocales.first() && !resource.isValid
        DoubleTapToEditDenseTextField(
            text = {
                Text(
                    resource.text.ifEmpty { "(empty)" },
                    modifier = Modifier.weight(1f)
                        .alpha(if (resource.text.isEmpty()) ContentAlpha.disabled else ContentAlpha.high)
                        .background(color = if (isError) MaterialTheme.colors.error else Color.Unspecified)
                        .padding(8.dp).then(it),
                    color = if (isError) MaterialTheme.colors.onError else Color.Unspecified
                )
            },
            textField = { modifier ->
                DenseTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f).then(modifier),
                    singleLine = true
                )
            },
            shouldDropFocus = {
                if (resource.text != text) vm.updateResource(localeIsoCode, resId, Str(text))
                true
            }
        )
    }
}

@Composable
private fun RowScope.PluralRow(vm: PluralResourceViewModel, displayedLocales: List<LocaleIsoCode>, resId: ResourceId) {
    var isExpanded by remember { mutableStateOf(false) }
    IconButton(
        resourcePath = if (isExpanded) R.drawable.compress else R.drawable.expand,
        contentDescription = if (isExpanded) "Collapse quantity options" else "Show all quantity options"
    ) {
        isExpanded = !isExpanded
    }
    displayedLocales.forEach { localeIsoCode ->
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
            val resource by vm.resource(resId, localeIsoCode).map { it ?: Plural() }.collectAsState(Plural())
            val quantityModifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()
            Quantity.values().forEach { quantity ->
                var text by remember(resource) { mutableStateOf(resource[quantity].orEmpty()) }
                if (isExpanded || quantity.isRequired || text.isNotEmpty()) {
                    val isError = localeIsoCode == displayedLocales.first() && quantity.isRequired && text.isEmpty()
                    DoubleTapToEditDenseTextField(
                        text = {
                            Text(
                                text = "${quantity.label}: ${resource[quantity].orEmpty().ifEmpty { "(empty)" }}",
                                modifier = quantityModifier
                                    .alpha(if (text.isEmpty() && !quantity.isRequired) ContentAlpha.disabled else ContentAlpha.high)
                                    .background(color = if (isError) MaterialTheme.colors.error else Color.Unspecified)
                                    .padding(2.dp).then(it),
                                color = if (isError) MaterialTheme.colors.onError else Color.Unspecified
                            )
                        },
                        textField = { modifier ->
                            DenseTextField(
                                value = text,
                                onValueChange = { text = it },
                                modifier = quantityModifier.then(modifier),
                                label = { Text(quantity.label) },
                                singleLine = true
                            )
                        },
                        shouldDropFocus = {
                            if (resource[quantity].orEmpty() != text) {
                                vm.updateResource(localeIsoCode, resId, Plural(resource.items.plus(quantity to text)))
                            }
                            true
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun RowScope.ArrayRow(vm: ArrayResourceViewModel, displayedLocales: List<LocaleIsoCode>, resId: ResourceId) {
    val oldSize by vm.arraySize(resId).collectAsState(1)
    var newSize by remember(oldSize) { mutableStateOf(oldSize) }
    var sizeFieldHasFocus by remember { mutableStateOf(false) }
    DenseTextField(
        value = newSize.toString(),
        onValueChange = { newSize = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
        modifier = Modifier.padding(start = 8.dp).width(64.dp)
            .composed {
                val focusManager = LocalFocusManager.current
                onPressEnter(focusManager::clearFocus)
            }
            .onFocusChanged {
                if (!it.hasFocus && oldSize != newSize) {
                    vm.updateArraySize(resId, newSize)
                }
                sizeFieldHasFocus = it.hasFocus
            },
        label = { Text("Size") },
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = if (sizeFieldHasFocus) MaterialTheme.colors.onSurface.copy(alpha = BackgroundOpacity) else Color.Unspecified,
            unfocusedIndicatorColor = Color.Transparent
        )
    )

    val size by vm.arraySize(resId).collectAsState(1)
    displayedLocales.forEach { localeIsoCode ->
        Spacer(Modifier.width(8.dp))
        val items by vm.resource(resId, localeIsoCode).map { it?.items ?: listOf() }.collectAsState(listOf())
        var newItems by remember(size, items) { mutableStateOf(List(size) { items.getOrElse(it) { "" } }) }

        Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
            newItems.forEachIndexed { index, item ->
                val isError = localeIsoCode == displayedLocales.first() && item.isEmpty()
                DoubleTapToEditDenseTextField(
                    text = {
                        Text(
                            item.ifEmpty { "(empty)" },
                            modifier = Modifier.alpha(if (item.isEmpty()) ContentAlpha.disabled else ContentAlpha.high)
                                .background(color = if (isError) MaterialTheme.colors.error else Color.Unspecified)
                                .padding(2.dp).then(it),
                            color = if (isError) MaterialTheme.colors.onError else Color.Unspecified
                        )
                    },
                    textField = { modifier ->
                        DenseTextField(
                            value = item,
                            onValueChange = { newItems = newItems.mapIndexed { i, oldItem -> if (i == index) it else oldItem } },
                            modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth().then(modifier),
                            singleLine = true
                        )
                    },
                    shouldDropFocus = {
                        if (items != newItems) vm.updateResource(localeIsoCode, resId, StringArray(newItems))
                        true
                    }
                )
            }
        }
    }
}
