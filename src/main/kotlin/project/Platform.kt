package project

import androidx.compose.runtime.Immutable
import generators.generateAndroidResources
import generators.generateIOSResources
import importers.importAndroidResources
import importers.importIosResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.resource.ResourceViewModel
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

    fun fileName(type: ResourceType) = when (this) {
        ANDROID -> "strings.xml"
        IOS -> when (type) {
            ResourceType.STRINGS -> "Localizable.strings"
            ResourceType.PLURALS -> "Localizable.stringsdict"
            ResourceType.ARRAYS -> "LocalizableArrays.plist"
        }
    }

    suspend fun importResources(vm: ResourceViewModel, file: File, overwrite: Boolean) = when (this) {
        ANDROID -> importAndroidResources(vm, file, overwrite)
        IOS -> importIosResources(vm, file, overwrite)
    }

    suspend fun exportResources(vm: ResourceViewModel) = withContext(Dispatchers.IO) {
        when (this@Platform) {
            ANDROID -> generateAndroidResources(vm)
            IOS -> generateIOSResources(vm)
        }
    }

    companion object {
        val ANDROID_ONLY = listOf(ANDROID)
        val IOS_ONLY = listOf(IOS)
        val ALL = listOf(ANDROID, IOS)
    }
}
