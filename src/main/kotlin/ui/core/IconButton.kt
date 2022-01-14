package ui.core

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource

@Composable
fun IconButton(
    resourcePath: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(painter = painterResource(resourcePath), contentDescription = contentDescription)
    }
}

@Composable
fun IconButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = imageVector.name,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}
