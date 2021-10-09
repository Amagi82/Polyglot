package project

import androidx.compose.runtime.Stable
import locales.LocaleIsoCode
import utils.Settings
import utils.extensions.prop
import java.io.File
import java.util.*

/**
 * @param name: Name of the project
 * @param androidOutputUrl: Set this to your project folder and Polyglot will output the files directly to that folder
 * @param iosOutputUrl: Set this to your project folder and Polyglot will output the files directly to that folder
 * @param locales: List of locales
 * @param defaultLocale: The current locale set as default, which will be used as backup if no suitable translation is found.
 * Android and iOS handle this differently:
 * With Android, the base values folder gets the default strings, and values-en, values-de, etc get the localized translations
 * With iOS, there is no base folder, all localizations are placed in their respective folders, e.g. en.proj, es.proj, de.proj
 * */
@Stable
data class Project(
    val name: String,
    val androidOutputUrl: String = "output/android",
    val iosOutputUrl: String = "output/ios",
    val locales: List<LocaleIsoCode> = listOf(LocaleIsoCode("en")),
    val defaultLocale: LocaleIsoCode = LocaleIsoCode("en")
) {

    fun save() {
        val file = file(name)
        val props = Properties()
        props.setProperty(PROP_ANDROID_OUTPUT, androidOutputUrl)
        props.setProperty(PROP_IOS_OUTPUT, iosOutputUrl)
        props.setProperty(PROP_LOCALES, locales.joinToString(",") { it.value })
        props.setProperty(PROP_DEFAULT_LOCALE, defaultLocale.value)
        runCatching { props.store(file.outputStream(), "") }.onFailure {
            println("Failed to save settings with $it")
        }
    }

    companion object {
        private const val PROP_ANDROID_OUTPUT = "androidOutputUrl"
        private const val PROP_IOS_OUTPUT = "iosOutputUrl"
        private const val PROP_LOCALES = "locales"
        private const val PROP_DEFAULT_LOCALE = "defaultLocale"

        private val folder = File("polyglot/projects").apply(File::mkdirs)

        val files = folder.listFiles()?.filter { it.extension == "properties" }.orEmpty()

        private fun file(name: String) = File(folder, "${name}.properties").apply(File::createNewFile)

        fun load(projectName: String): Project {
            val file = file(projectName)
            val props = Properties().apply { load(file.inputStream()) }
            return Project(
                name = projectName,
                androidOutputUrl = props.getProperty(PROP_ANDROID_OUTPUT, "output/android"),
                iosOutputUrl = props.getProperty(PROP_IOS_OUTPUT, "output/ios"),
                locales = props.getProperty(PROP_LOCALES)?.split(",")?.map(::LocaleIsoCode).orEmpty(),
                defaultLocale = LocaleIsoCode(props.getProperty(PROP_DEFAULT_LOCALE, "en"))
            )
        }
    }
}
