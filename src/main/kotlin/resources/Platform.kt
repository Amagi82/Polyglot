package resources

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Make a resource platform-specific
 */
@Stable
@Serializable
enum class Platform(val iconFileName: String) {
    ANDROID("android_black_24dp.svg"),
    IOS("apple_black_24dp.svg");

    companion object {
        val ANDROID_ONLY = listOf(ANDROID)
        val IOS_ONLY = listOf(IOS)
        val ALL = listOf(ANDROID, IOS)
    }
}
