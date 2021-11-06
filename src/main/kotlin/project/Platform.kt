package project

import androidx.compose.runtime.Immutable
import java.io.File

/**
 * Make a resource platform-specific
 */
@Immutable
enum class Platform(val iconId: String, val displayName: String) {
    ANDROID(R.drawable.android, "Android"),
    IOS(R.drawable.apple, "iOS");

    fun outputFolder(project: Project) = when (this) {
        ANDROID -> project.androidOutputUrl
        IOS -> project.iosOutputUrl
    }.let(::File)

    val resourceFileExtensions
        get() = when (this) {
            ANDROID -> arrayOf("xml")
            IOS -> arrayOf("strings", "stringsdict", "plist")
        }

    companion object {
        val ANDROID_ONLY = listOf(ANDROID)
        val IOS_ONLY = listOf(IOS)
        val ALL = listOf(ANDROID, IOS)
    }
}
