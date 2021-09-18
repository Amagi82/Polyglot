package ui.theme

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*

private val blueGrey900 = Color(0xFF263238)
private val blueGrey900Dark = Color(0xFF000A12)

private val blueGrey200 = Color(0xFFB0BEC5)
private val blueGrey700 = Color(0xFF455A64)

private val lime500 = Color(0xFFCDDC39)
private val lime500Dark = Color(0xFF99AA00)

private val lime200 = Color(0xFFE6EE9C)

private val LightColorPalette = lightColors(
    primary = blueGrey900,
    primaryVariant = blueGrey900Dark,
    secondary = lime500,
    secondaryVariant = lime500Dark
)

private val DarkColorPalette = darkColors(
    primary = blueGrey200,
    primaryVariant = blueGrey700,
    secondary = lime200
)

@Composable
fun PolyglotTheme(darkTheme: Boolean, content: @Composable () -> Unit) = DesktopMaterialTheme(
    colors = if (darkTheme) DarkColorPalette else LightColorPalette,
    content = content
)
