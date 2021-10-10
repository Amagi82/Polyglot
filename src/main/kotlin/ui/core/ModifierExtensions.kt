package ui.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*

// Return true to stop propagation of this event.
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onPressEnter(onEvent: () -> Boolean): Modifier = onPreviewKeyEvent {
    it.key == Key.Enter && it.type == KeyEventType.KeyUp && onEvent()
}
