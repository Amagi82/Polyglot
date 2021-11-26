package project

import androidx.compose.runtime.Immutable
import data.exporters.exportAndroidResources
import data.exporters.exportIOSResources
import data.importers.importAndroidResources
import data.importers.importIosResources
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

    val lowercase get() = name.lowercase()
    val defaultOutputUrl: String get() = "output/$lowercase"

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
        val data = vm.projectData(this@Platform)
        when (this@Platform) {
            ANDROID -> exportAndroidResources(data)
            IOS -> exportIOSResources(data)
        }
    }

    companion object {
        val ANDROID_ONLY = listOf(ANDROID)
        val IOS_ONLY = listOf(IOS)
        val ALL = listOf(ANDROID, IOS)
    }
}
