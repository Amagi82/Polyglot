package project

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Make a resource platform-specific
 */
@Stable
@Serializable
enum class Platform(val iconFileName: String) {
    ANDROID(R.drawable.android),
    IOS(R.drawable.apple);

    companion object {
        val ANDROID_ONLY = listOf(ANDROID)
        val IOS_ONLY = listOf(IOS)
        val ALL = listOf(ANDROID, IOS)
    }
}
