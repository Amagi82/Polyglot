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
import locales.LocaleIsoCode
import project.*
import ui.core.DenseTextField
import ui.core.IconButton
import ui.core.onPressEnter


@Composable
fun <T : Resource> ResourceRow(
    vm: ResourceTypeViewModel<T>,
    metadata: Metadata,
    displayedLocales: List<LocaleIsoCode>,
    resources: Map<LocaleIsoCode, T>,
    resId: ResourceId
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EditableIdField(resId = resId, removeResource = vm::removeResource, updateResourceId = vm::updateResourceId)
        var isPluralExpanded by remember { mutableStateOf(false) }
        var arraySize by remember { mutableStateOf(0) }

        when (vm) {
            is StringResourceViewModel -> Unit
            is PluralResourceViewModel -> {
                IconButton(
                    resourcePath = if (isPluralExpanded) R.drawable.compress else R.drawable.expand,
                    contentDescription = if (isPluralExpanded) "Collapse quantity options" else "Show all quantity options"
                ) {
                    isPluralExpanded = !isPluralExpanded
                }
            }
            is ArrayResourceViewModel -> {
                val arraySizes by vm.arraySizes.collectAsState()
                arraySize = arraySizes[resId] ?: 0
                var sizeFieldHasFocus by remember { mutableStateOf(false) }
                DenseTextField(
                    value = arraySize.toString(),
                    onValueChange = { arraySize = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                    modifier = Modifier.padding(start = 8.dp).width(64.dp)
                        .composed {
                            val focusManager = LocalFocusManager.current
                            onPressEnter(focusManager::clearFocus)
                        }
                        .onFocusChanged {
                            if (!it.hasFocus && arraySizes[resId] != arraySize) {
                                vm.updateArraySize(resId, arraySize)
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
            val resource = resources[locale]
            when {
                resource is Str? && vm is StringResourceViewModel -> {
                    StringField(
                        localeIsoCode = locale,
                        resource = resource ?: Str(),
                        isDefaultLocale = i == 0,
                        resId = resId,
                        updateResource = vm::updateResource
                    )
                }
                resource is Plural? && vm is PluralResourceViewModel -> {
                    PluralFields(
                        localeIsoCode = locale,
                        resource = resource ?: Plural(),
                        isDefaultLocale = i == 0,
                        isExpanded = isPluralExpanded,
                        resId = resId,
                        updateResource = vm::updateResource
                    )
                }
                resource is StringArray? && vm is ArrayResourceViewModel -> {
                    ArrayFields(
                        localeIsoCode = locale,
                        resource = resource ?: StringArray(List(arraySize) { "" }),
                        isDefaultLocale = i == 0,
                        size = arraySize,
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
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, Str) -> Unit,
) {
    Spacer(Modifier.width(8.dp))
    var text by remember(resource) { mutableStateOf(resource.text) }
    val isError = isDefaultLocale && !resource.isValid
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
            if (resource.text != text) updateResource(resId, localeIsoCode, Str(text))
            true
        }
    )
}

@Composable
private fun RowScope.PluralFields(
    localeIsoCode: LocaleIsoCode,
    resource: Plural,
    isDefaultLocale: Boolean,
    isExpanded: Boolean,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, Plural) -> Unit
) {
    Spacer(Modifier.width(8.dp))
    Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
        val quantityModifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()
        Quantity.values().forEach { quantity ->
            var text by remember(resource) { mutableStateOf(resource[quantity].orEmpty()) }
            if (isExpanded || quantity.isRequired || text.isNotEmpty()) {
                val isError = isDefaultLocale && quantity.isRequired && text.isEmpty()
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
                            updateResource(resId, localeIsoCode, Plural(resource.items.plus(quantity to text)))
                        }
                        true
                    }
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
    size: Int,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, StringArray) -> Unit
) {
    Spacer(Modifier.width(8.dp))
    val items = remember(resource, size) { List(size) { resource.items.getOrNull(it) ?: "" } }

    Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
        items.forEachIndexed { index, item ->
            var text by remember(items) { mutableStateOf(item) }
            val isError = isDefaultLocale && text.isEmpty()
            DoubleTapToEditDenseTextField(
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
                    DenseTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth().then(modifier),
                        singleLine = true
                    )
                },
                shouldDropFocus = {
                    if (item != text) updateResource(resId, localeIsoCode, StringArray(items.mapIndexed { i, item -> if (i == index) text else item }))
                    true
                }
            )
        }
    }
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
private fun RowScope.EditableIdField(
    resId: ResourceId,
    removeResource: (ResourceId) -> Unit,
    updateResourceId: (old: ResourceId, new: ResourceId) -> Boolean
) {
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
                    removeResource(resId)
                    false
                }
                !updateResourceId(resId, id) -> {
                    error = "id already exists"
                    false
                }
                else -> false
            }
        })
}

@Composable
private fun PlatformEditor(resId: ResourceId, platforms: List<Platform>, togglePlatform: (ResourceId, Platform) -> Unit) {
    Platform.values().forEach { platform ->
        IconButton(platform.iconId, Modifier.alpha(if (platform in platforms) 1f else 0.1f), contentDescription = platform.name) {
            togglePlatform(resId, platform)
        }
    }
}

/*@Composable
fun <T : Resource> ResourceRow(
    vm: ResourceTypeViewModel<T>,
    metadata: Metadata,
    displayedLocales: List<LocaleIsoCode>,
    resources: Map<LocaleIsoCode, T>,
    resId: ResourceId
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EditableIdField(resId = resId, removeResource = vm::removeResource, updateResourceId = vm::updateResourceId)
        var isPluralExpanded by remember { mutableStateOf(false) }
        var arraySize by remember { mutableStateOf(0) }

        when (vm) {
            is StringResourceViewModel -> Unit
            is PluralResourceViewModel -> {
                IconButton(
                    resourcePath = if (isPluralExpanded) R.drawable.compress else R.drawable.expand,
                    contentDescription = if (isPluralExpanded) "Collapse quantity options" else "Show all quantity options"
                ) {
                    isPluralExpanded = !isPluralExpanded
                }
            }
            is ArrayResourceViewModel -> {
                val arraySizes by vm.arraySizes.collectAsState()
                arraySize = arraySizes[resId] ?: 0
                var sizeFieldHasFocus by remember { mutableStateOf(false) }
                DenseTextField(
                    value = arraySize.toString(),
                    onValueChange = { arraySize = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                    modifier = Modifier.padding(start = 8.dp).width(64.dp)
                        .composed {
                            val focusManager = LocalFocusManager.current
                            onPressEnter(focusManager::clearFocus)
                        }
                        .onFocusChanged {
                            if (!it.hasFocus && arraySizes[resId] != arraySize) {
                                vm.updateArraySize(resId, arraySize)
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
            val resource = resources[locale]
            when {
                resource is Str? && vm is StringResourceViewModel -> {
                    StringField(
                        localeIsoCode = locale,
                        resource = resource ?: Str(),
                        isDefaultLocale = i == 0,
                        resId = resId,
                        updateResource = vm::updateResource
                    )
                }
                resource is Plural? && vm is PluralResourceViewModel -> {
                    PluralFields(
                        localeIsoCode = locale,
                        resource = resource ?: Plural(),
                        isDefaultLocale = i == 0,
                        isExpanded = isPluralExpanded,
                        resId = resId,
                        updateResource = vm::updateResource
                    )
                }
                resource is StringArray? && vm is ArrayResourceViewModel -> {
                    ArrayFields(
                        localeIsoCode = locale,
                        resource = resource ?: StringArray(List(arraySize) { "" }),
                        isDefaultLocale = i == 0,
                        size = arraySize,
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
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, Str) -> Unit,
) {
    Spacer(Modifier.width(8.dp))
    var text by remember(resource) { mutableStateOf(resource.text) }
    val isError = isDefaultLocale && !resource.isValid
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
            if (resource.text != text) updateResource(resId, localeIsoCode, Str(text))
            true
        }
    )
}

@Composable
private fun RowScope.PluralFields(
    localeIsoCode: LocaleIsoCode,
    resource: Plural,
    isDefaultLocale: Boolean,
    isExpanded: Boolean,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, Plural) -> Unit
) {
    Spacer(Modifier.width(8.dp))
    Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
        val quantityModifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()
        Quantity.values().forEach { quantity ->
            var text by remember(resource) { mutableStateOf(resource[quantity].orEmpty()) }
            if (isExpanded || quantity.isRequired || text.isNotEmpty()) {
                val isError = isDefaultLocale && quantity.isRequired && text.isEmpty()
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
                            updateResource(resId, localeIsoCode, Plural(resource.items.plus(quantity to text)))
                        }
                        true
                    }
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
    size: Int,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, StringArray) -> Unit
) {
    Spacer(Modifier.width(8.dp))
    val items = remember(resource, size) { List(size) { resource.items.getOrNull(it) ?: "" } }

    Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
        items.forEachIndexed { index, item ->
            var text by remember(items) { mutableStateOf(item) }
            val isError = isDefaultLocale && text.isEmpty()
            DoubleTapToEditDenseTextField(
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
                    DenseTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth().then(modifier),
                        singleLine = true
                    )
                },
                shouldDropFocus = {
                    if (item != text) updateResource(resId, localeIsoCode, StringArray(items.mapIndexed { i, item -> if (i == index) text else item }))
                    true
                }
            )
        }
    }
}*/


/*
@Composable
fun <T : Resource> ResourceRow(
    vm: ResourceTypeViewModel<T>,
    metadata: Metadata,
    displayedLocales: List<LocaleIsoCode>,
    resources: Map<LocaleIsoCode, T>,
    resId: ResourceId
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EditableIdField(resId = resId, removeResource = vm::removeResource, updateResourceId = vm::updateResourceId)

        when {
            vm is StringResourceViewModel && resources is Map<LocaleIsoCode, Str> -> {
                StringField(
                    displayedLocales = displayedLocales,
                    resources = resources,
                    resId = resId,
                    updateResource = vm::updateResource
                )
            }
            vm is PluralResourceViewModel -> {
                PluralFields(
                    displayedLocales = displayedLocales,
                    resources = resources,
                    resId = resId,
                    updateResource = vm::updateResource
                )
            }
            vm is ArrayResourceViewModel -> {
                ArrayFields(
                    displayedLocales = displayedLocales,
                    resources = resources,
                    resId = resId,
                    updateResource = vm::updateResource
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        PlatformEditor(resId = resId, platforms = metadata.platforms, togglePlatform = vm::togglePlatform)
    }
    Divider()
}

@Composable
private fun RowScope.StringField(
    displayedLocales: List<LocaleIsoCode>,
    resources: Map<LocaleIsoCode, Str>,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, Str) -> Unit,
) {
    Spacer(Modifier.width(8.dp))

    displayedLocales.forEachIndexed { i, locale ->
        val resource = resources[locale] ?: Str()
        var text by remember(resource) { mutableStateOf(resource.text) }
        val isError = i == 0 && !resource.isValid
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
                if (resource.text != text) updateResource(resId, locale, Str(text))
                true
            }
        )
    }
}

@Composable
private fun RowScope.PluralFields(
    displayedLocales: List<LocaleIsoCode>,
    resources: Map<LocaleIsoCode, Plural>,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, Plural) -> Unit
) {
    Spacer(Modifier.width(8.dp))

    var isExpanded by remember { mutableStateOf(false) }

    IconButton(
        resourcePath = if (isExpanded) R.drawable.compress else R.drawable.expand,
        contentDescription = if (isExpanded) "Collapse quantity options" else "Show all quantity options"
    ) {
        isExpanded = !isExpanded
    }
    Spacer(Modifier.width(8.dp))
    displayedLocales.forEachIndexed { i, locale ->
        val resource = resources[locale] ?: Plural()

        Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
            val quantityModifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()
            Quantity.values().forEach { quantity ->
                var text by remember(resource) { mutableStateOf(resource[quantity].orEmpty()) }
                if (isExpanded || quantity.isRequired || text.isNotEmpty()) {
                    val isError = i == 0 && quantity.isRequired && text.isEmpty()
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
                                updateResource(resId, locale, Plural(resource.items.plus(quantity to text)))
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
private fun RowScope.ArrayFields(
    displayedLocales: List<LocaleIsoCode>,
    resources: Map<LocaleIsoCode, StringArray>,
    resId: ResourceId,
    updateResource: (ResourceId, LocaleIsoCode, StringArray) -> Unit
) {
    Spacer(Modifier.width(8.dp))

    val arraySizes by vm.arraySizes.collectAsState()
    var arraySize by remember { mutableStateOf(0) }
    arraySize = arraySizes[resId] ?: 0
    var sizeFieldHasFocus by remember { mutableStateOf(false) }
    DenseTextField(
        value = arraySize.toString(),
        onValueChange = { arraySize = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
        modifier = Modifier.padding(start = 8.dp).width(64.dp)
            .composed {
                val focusManager = LocalFocusManager.current
                onPressEnter(focusManager::clearFocus)
            }
            .onFocusChanged {
                if (!it.hasFocus && arraySizes[resId] != arraySize) {
                    vm.updateArraySize(resId, arraySize)
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

    displayedLocales.forEachIndexed { i, locale ->
        val resource = resources[locale]
        val items = remember(resource, size) { List(size) { resource.items.getOrNull(it) ?: "" } }

        Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
            items.forEachIndexed { index, item ->
                var text by remember(items) { mutableStateOf(item) }
                val isError = i == 0 && text.isEmpty()
                DoubleTapToEditDenseTextField(
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
                        DenseTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth().then(modifier),
                            singleLine = true
                        )
                    },
                    shouldDropFocus = {
                        if (item != text) updateResource(resId, locale, StringArray(items.mapIndexed { i, item -> if (i == index) text else item }))
                        true
                    }
                )
            }
        }
    }
}
*/
