package project

import androidx.compose.runtime.Stable

/**
 * Make a resource platform-specific
 */
@Stable
enum class Platform(val iconId: String, val displayName: String) {
    ANDROID(R.drawable.android, "Android"),
    IOS(R.drawable.apple, "iOS");

    companion object {
        val ANDROID_ONLY = listOf(ANDROID)
        val IOS_ONLY = listOf(IOS)
        val ALL = listOf(ANDROID, IOS)
    }
}
