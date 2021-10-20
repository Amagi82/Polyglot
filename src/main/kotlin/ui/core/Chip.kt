package ui.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp
import ui.core.theme.onPrimarySurface

@Composable
fun Chip(
    text: @Composable RowScope.() -> Unit,
    onClick: () -> Unit = {},
    isClickEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    leadingIcon: @Composable RowScope.() -> Unit = {},
    trailingIcon: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = color.takeOrElse { MaterialTheme.colors.onSurface.copy(alpha = 0.12f) })
            .clickable(enabled = isClickEnabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon()
        text()
        trailingIcon()
    }
}
