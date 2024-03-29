package ui.core

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*

// Return true to stop propagation of this event.
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onPressEnter(onEvent: () -> Unit): Modifier = onPreviewKeyEvent {
    (it.key == Key.Enter && it.type == KeyEventType.KeyUp).also { isEnter -> if (isEnter) onEvent() }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onPressEsc(onEvent: () -> Unit): Modifier = onPreviewKeyEvent {
    (it.key == Key.Escape && it.type == KeyEventType.KeyUp).also { isEscape -> if (isEscape) onEvent() }
}
