package ui.core

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager

// Return true to stop propagation of this event.
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onPressEnter(onEvent: () -> Boolean): Modifier = onPreviewKeyEvent {
    it.key == Key.Enter && it.type == KeyEventType.KeyUp && onEvent()
}

fun Modifier.focusNextOnEnter() = composed {
    val focusManager = LocalFocusManager.current
    onPressEnter { focusManager.moveFocus(FocusDirection.Next); true }
}
