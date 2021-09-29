package project

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * Make a resource platform-specific
 */
@Stable
enum class Platform(val iconId: String, val color: Color) {
    ANDROID(R.drawable.android, Color.Green),
    IOS(R.drawable.apple, Color.Blue);

    companion object {
        val ANDROID_ONLY = listOf(ANDROID)
        val IOS_ONLY = listOf(IOS)
        val ALL = listOf(ANDROID, IOS)
    }
}
