package project

import androidx.compose.runtime.Immutable
import java.io.File

/**
 * @param name: Name of the project
 * @property exportUrls: Set this to your project folders and Polyglot will generate files directly to that folder
 * @property defaultLocale: The current locale set as default, which will be used as backup if no suitable translation is found.
 * Android and iOS handle this differently:
 * With Android, the base values folder gets the default strings, and values-en, values-de, etc get the localized translations
 * With iOS, there is no base folder, all localizations are placed in their respective folders, e.g. en.proj, es.proj, de.proj
 * */
@Immutable
data class Project(val name: String) {
    companion object {
        const val PROP_EXPORT_URLS = "exportUrls"
        const val PROP_DEFAULT_LOCALE = "defaultLocale"
        const val PROP_LOCALES = "locales"

        val projectsFolder = File("projects").apply(File::mkdirs)

        fun projectFolder(projectName: String) = File(projectsFolder, projectName).apply(File::mkdirs)
    }
}
