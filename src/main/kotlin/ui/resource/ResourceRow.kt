package ui.resource

import R
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.mouseClickable
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
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import locales.LocaleIsoCode
import project.*
import ui.core.IconButton
import ui.core.onPressEnter
import ui.core.onPressEsc


@Composable
fun <T : Resource, M : Metadata<M>> ResourceRow(
    vm: ResourceTypeViewModel<T, M>,
    metadata: M,
    displayedLocales: List<LocaleIsoCode>,
    resources: Map<LocaleIsoCode, T>,
    resId: ResourceId,
    isMultiSelectEnabled: Boolean,
    isSelected: Boolean,
    onClick: (isCtrlPressed: Boolean, isShiftPressed: Boolean) -> Unit
) {
    Row(
        modifier = Modifier.background(if (isSelected) MaterialTheme.colors.onSurface.copy(alpha = 0.12f) else Color.Unspecified)
            .mouseClickable(enabled = isMultiSelectEnabled) { onClick(keyboardModifiers.isCtrlPressed, keyboardModifiers.isShiftPressed) }
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditableIdField(resId = resId, isSelectable = !isMultiSelectEnabled, removeResource = vm::removeResource, updateResourceId = vm::updateResourceId)
        var isPluralExpanded by remember { mutableStateOf(false) }

        when {
            vm is StringResourceViewModel -> Unit
            vm is PluralResourceViewModel -> {
                IconButton(
                    resourcePath = if (isPluralExpanded) R.drawable.compress else R.drawable.expand,
                    contentDescription = if (isPluralExpanded) "Collapse quantity options" else "Show all quantity options"
                ) {
                    isPluralExpanded = !isPluralExpanded
                }
            }
            vm is ArrayResourceViewModel && metadata is ArrayMetadata -> {
                var size by remember(metadata) { mutableStateOf(metadata.size) }
                var sizeFieldHasFocus by remember { mutableStateOf(false) }
                TextField(
                    value = size.toString(),
                    onValueChange = { size = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                    modifier = Modifier.padding(start = 8.dp).width(64.dp)
                        .composed {
                            val focusManager = LocalFocusManager.current
                            onPressEnter(focusManager::clearFocus)
                            onPressEsc {
                                size = metadata.size
                                focusManager.clearFocus()
                            }
                        }
                        .onFocusChanged {
                            if (!it.hasFocus && metadata.size != size) {
                                vm.updateArraySize(resId, size)
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
            }
        }

        displayedLocales.forEachIndexed { i, locale ->
            Spacer(Modifier.width(8.dp))
            val resource = resources[locale]
            when {
                resource is Str? && vm is StringResourceViewModel -> {
                    StringField(
                        localeIsoCode = locale,
                        resource = resource ?: Str(),
                        isDefaultLocale = i == 0,
                        isSelectable = !isMultiSelectEnabled,
                        resId = resId,
                        updateResource = vm::updateResource
                    )
                }
                resource is Plural? && vm is PluralResourceViewModel -> {
                    PluralFields(
                        localeIsoCode = locale,
                        resource = resource ?: Plural(),
                        isDefaultLocale = i == 0,
                        isSelectable = !isMultiSelectEnabled,
                        isExpanded = isPluralExpanded,
                        resId = resId,
                        updateResource = vm::updateResource
                    )
                }
                resource is StringArray? && vm is ArrayResourceViewModel && metadata is ArrayMetadata -> {
                    ArrayFields(
                        localeIsoCode = locale,
                        resource = resource ?: StringArray(),
                        isDefaultLocale = i == 0,
                        isSelectable = !isMultiSelectEnabled,
                        size = metadata.size,
                        resId = resId,
                        updateResource = vm::updateResource
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        PlatformEditor(resId = resId, platforms = metadata.platforms, togglePlatform = vm::togglePlatform)
    }
    Divider()
}

@Composable
private fun RowScope.StringField(
    localeIsoCode: LocaleIsoCode,
    resource: Str,
    isDefaultLocale: Boolean,
    isSelectable: Boolean,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, Str) -> Unit,
) {
    var text by remember(resource) { mutableStateOf(resource.text) }
    val isError = isDefaultLocale && !resource.isValid
    DoubleClickToEditTextField(
        text = {
            Text(
                resource.text.ifEmpty { "(empty)" },
                modifier = Modifier.weight(1f)
                    .alpha(if (resource.text.isEmpty()) ContentAlpha.disabled else ContentAlpha.high)
                    .background(color = if (isError) MaterialTheme.colors.error else Color.Unspecified)
                    .padding(2.dp).then(it),
                color = if (isError) MaterialTheme.colors.onError else Color.Unspecified
            )
        },
        textField = { modifier ->
            TextFieldWithCursorPositionEnd(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f).then(modifier)
            )
        },
        isSelectable = isSelectable,
        shouldDropFocus = {
            if (resource.text != text) updateResource(resId, localeIsoCode, Str(text))
            true
        },
        cancel = { text = resource.text }
    )
}

@Composable
private fun RowScope.PluralFields(
    localeIsoCode: LocaleIsoCode,
    resource: Plural,
    isDefaultLocale: Boolean,
    isSelectable: Boolean,
    isExpanded: Boolean,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, Plural) -> Unit
) {
    Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
        val quantityModifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()
        Quantity.values().forEach { quantity ->
            var text by remember(resource) { mutableStateOf(resource[quantity].orEmpty()) }
            if (isExpanded || quantity.isRequired || text.isNotEmpty()) {
                val isError = isDefaultLocale && quantity.isRequired && text.isEmpty()
                DoubleClickToEditTextField(
                    text = {
                        Text(
                            text = "${quantity.label}: ${resource[quantity].orEmpty().ifEmpty { "(empty)" }}",
                            modifier = quantityModifier
                                .alpha(if (text.isEmpty() && (!quantity.isRequired || !isExpanded)) ContentAlpha.disabled else ContentAlpha.high)
                                .background(color = if (isError) MaterialTheme.colors.error else Color.Unspecified)
                                .padding(2.dp).then(it),
                            color = if (isError) MaterialTheme.colors.onError else Color.Unspecified
                        )
                    },
                    textField = { modifier ->
                        TextFieldWithCursorPositionEnd(
                            value = text,
                            onValueChange = { text = it },
                            modifier = quantityModifier.then(modifier),
                            label = { Text(quantity.label) }
                        )
                    },
                    isSelectable = isSelectable,
                    shouldDropFocus = {
                        if (resource[quantity].orEmpty() != text) {
                            updateResource(resId, localeIsoCode, Plural(resource.items.plus(quantity to text)))
                        }
                        true
                    },
                    cancel = { text = resource[quantity].orEmpty() }
                )
            }
        }
    }
}


@Composable
private fun RowScope.ArrayFields(
    localeIsoCode: LocaleIsoCode,
    resource: StringArray,
    isDefaultLocale: Boolean,
    isSelectable: Boolean,
    size: Int,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, StringArray) -> Unit
) {
    val items = remember(resource, size) { List(size) { resource.items.getOrNull(it) ?: "" } }

    Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
        items.forEachIndexed { index, item ->
            var text by remember(items) { mutableStateOf(item) }
            val isError = isDefaultLocale && text.isEmpty()
            DoubleClickToEditTextField(
                text = {
                    Text(
                        text.ifEmpty { "(empty)" },
                        modifier = Modifier.alpha(if (text.isEmpty()) ContentAlpha.disabled else ContentAlpha.high)
                            .background(color = if (isError) MaterialTheme.colors.error else Color.Unspecified)
                            .padding(2.dp).then(it),
                        color = if (isError) MaterialTheme.colors.onError else Color.Unspecified
                    )
                },
                textField = { modifier ->
                    TextFieldWithCursorPositionEnd(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth().then(modifier)
                    )
                },
                isSelectable = isSelectable,
                shouldDropFocus = {
                    if (item != text) updateResource(resId, localeIsoCode, StringArray(items.mapIndexed { i, item -> if (i == index) text else item }))
                    true
                },
                cancel = { text = item }
            )
        }
    }
}

/**
 * Displays a normal Text, double click to switch to TextField and edit
 */
@Composable
private fun DoubleClickToEditTextField(
    text: @Composable (modifier: Modifier) -> Unit,
    textField: @Composable (modifier: Modifier) -> Unit,
    isSelectable: Boolean,
    shouldDropFocus: () -> Boolean,
    cancel: () -> Unit
) {
    var editMode by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    if (editMode) {
        val focusRequester = remember { FocusRequester() }
        var hasBeenFocused by remember { mutableStateOf(false) }

        textField(Modifier.focusRequester(focusRequester)
            .onPressEnter { if (shouldDropFocus()) focusManager.clearFocus() }
            .onPressEsc { cancel(); focusManager.clearFocus() }
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
        text(
            Modifier.defaultMinSize(minHeight = 40.dp)
                .run { if (isSelectable) combinedClickable(onDoubleClick = { editMode = true }) {} else this }
                .wrapContentHeight(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun RowScope.EditableIdField(
    resId: ResourceId,
    isSelectable: Boolean,
    removeResource: (ResourceId) -> Unit,
    updateResourceId: (old: ResourceId, new: ResourceId) -> Boolean
) {
    val idModifier = Modifier.weight(1f).padding(vertical = 4.dp)
    var id by remember { mutableStateOf(resId) }
    var error by remember { mutableStateOf("") }
    DoubleClickToEditTextField(
        text = { Text(resId.value, modifier = idModifier.then(it)) },
        textField = { modifier ->
            Column(modifier = idModifier) {
                TextFieldWithCursorPositionEnd(
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
        isSelectable = isSelectable,
        shouldDropFocus = {
            when {
                resId == id -> true
                id.value.isEmpty() -> {
                    removeResource(resId)
                    false
                }
                !updateResourceId(resId, id) -> {
                    error = "id already exists"
                    false
                }
                else -> false
            }
        },
        cancel = {
            error = ""
            id = resId
        })
}

/**
 * Inside DoubleClickToEditTextField, since the TextField doesn't initially exist, the cursor ends up positioned at the
 * beginning of the text rather than the end, where it feels more natural. This overrides that behavior and places the
 * cursor at the end
 */
@Composable
private fun TextFieldWithCursorPositionEnd(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
    val textFieldValue = textFieldValueState.copy(text = value)

    TextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValueState = it
            if (value != it.text) {
                onValueChange(it.text)
            }
        },
        modifier = modifier,
        singleLine = singleLine,
        label = label,
        isError = isError
    )
}

@Composable
private fun PlatformEditor(resId: ResourceId, platforms: List<Platform>, togglePlatform: (ResourceId, Platform) -> Unit) {
    Platform.values().forEach { platform ->
        IconButton(platform.iconId, Modifier.alpha(if (platform in platforms) 1f else 0.1f), contentDescription = platform.name) {
            togglePlatform(resId, platform)
        }
    }
}
