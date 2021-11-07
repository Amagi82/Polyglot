package ui.core

import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
fun TextFieldDefaults.outlinedTextFieldColorsOnPrimary() = outlinedTextFieldColors(
    cursorColor = MaterialTheme.colors.onPrimary,
    focusedBorderColor = MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.high),
    unfocusedBorderColor = MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.disabled),
    focusedLabelColor = MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.high),
    unfocusedLabelColor = MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.medium),
)
