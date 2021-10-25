package ui.core.theme

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.material.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*

private val blueGrey200 = Color(0xFFB0BEC5)
private val blueGrey700 = Color(0xFF455A64)
private val blueGrey900 = Color(0xFF263238)
private val blueGrey900Dark = Color(0xFF000A12)

private val indigo300 = Color(0xFF7986CB)
private val indigo700 = Color(0xFF303F9F)
private val indigo900 = Color(0x1A237E)

private val lime200 = Color(0xFFE6EE9C)
private val lime500 = Color(0xFFCDDC39)
private val lime500Dark = Color(0xFF99AA00)
private val lime700 = Color(0xFFAFB42B)

private val LightColorPalette = lightColors(
    primary = indigo700,
    primaryVariant = indigo900,
    secondary = lime500,
    secondaryVariant = lime700
)

private val DarkColorPalette = darkColors(
    primary = indigo300,
    primaryVariant = indigo700,
    secondary = lime200,
    background = Color(0xFF212121),
    surface = Color(0xFF212121),
)

@Composable
fun PolyglotTheme(darkTheme: Boolean, content: @Composable () -> Unit) = MaterialTheme(
    colors = if (darkTheme) DarkColorPalette else LightColorPalette,
    content = content
)

val Colors.onPrimarySurface: Color get() = contentColorFor(primarySurface)
