package project

import androidx.compose.runtime.Stable
import locales.LocaleIsoCode
import java.io.File
import java.util.*

/**
 * @param name: Name of the project
 * @param androidOutputUrl: Set this to your project folder and Polyglot will output the files directly to that folder
 * @param iosOutputUrl: Set this to your project folder and Polyglot will output the files directly to that folder
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
    val defaultLocale: LocaleIsoCode = LocaleIsoCode("en")
) {

    fun save() {
        val file = projectFile(name)
        val props = Properties()
        props.setProperty(PROP_ANDROID_OUTPUT, androidOutputUrl)
        props.setProperty(PROP_IOS_OUTPUT, iosOutputUrl)
        props.setProperty(PROP_DEFAULT_LOCALE, defaultLocale.value)
        runCatching { props.store(file.outputStream(), "") }.onFailure {
            println("Failed to save settings with $it")
        }
    }

    companion object {
        private const val PROP_ANDROID_OUTPUT = "androidOutputUrl"
        private const val PROP_IOS_OUTPUT = "iosOutputUrl"
        private const val PROP_DEFAULT_LOCALE = "defaultLocale"

        private val projects = File("projects").apply(File::mkdirs)

        val projectFolders = projects.listFiles()?.filter { it.isDirectory }.orEmpty()

        private fun projectFolder(projectName: String) = File(projects, projectName).apply(File::mkdirs)

        private fun projectFile(projectName: String) = File(projectFolder(projectName), "project.properties").apply(File::createNewFile)

        fun resourcesFile(projectName: String) = File(projectFolder(projectName), "resources.properties").apply(File::createNewFile)

        fun localizedResourceFiles(projectName: String) =
            projectFolder(projectName).listFiles()?.filter { it != projectFile(projectName) && it != resourcesFile(projectName) }.orEmpty()

        fun localizedResourcesFile(projectName: String, locale: LocaleIsoCode) =
            File(projectFolder(projectName), "${locale.value}.properties").apply(File::createNewFile)

        fun load(projectName: String): Project {
            val file = projectFile(projectName)
            val props = Properties().apply { load(file.inputStream()) }
            return Project(
                name = projectName,
                androidOutputUrl = props.getProperty(PROP_ANDROID_OUTPUT, "output/android"),
                iosOutputUrl = props.getProperty(PROP_IOS_OUTPUT, "output/ios"),
                defaultLocale = LocaleIsoCode(props.getProperty(PROP_DEFAULT_LOCALE, "en"))
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun loadResources(projectName: String): Resources = buildMap {
            val props = Properties().apply { load(resourcesFile(projectName).inputStream()) }
            props.stringPropertyNames().forEach { k ->
                val v = props.getProperty(k)
                val splits = v.split('|')
                put(
                    ResourceId(k),
                    Resource(group = splits[2], platforms = splits[1].split(',').map { Platform.valueOf(it) }, type = Resource.Type.valueOf(splits[0]))
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun loadLocalizedResources(projectName: String): LocalizedResources = localizedResourceFiles(projectName).orEmpty().associate { file ->
            val locale = LocaleIsoCode(file.nameWithoutExtension)
            val props = Properties().apply { load(localizedResourcesFile(projectName, locale).inputStream()) }
            locale to buildMap {
                val (others, strings) = props.stringPropertyNames().partition { it.contains('.') }
                strings.forEach { k -> put(ResourceId(k), Str(props.getProperty(k))) }
                val (arrays, plurals) = others.partition { it.last().isDigit() }
                arrays.groupBy { it.substringBefore('.') }.forEach { (id, keys) ->
                    put(ResourceId(id), StringArray(keys.sorted().map(props::getProperty)))
                }
                plurals.groupBy { it.substringBefore('.') }.forEach { (id, keys) ->
                    put(ResourceId(id), Plural(keys.associate { Quantity.valueOf(it.substringAfter('.').uppercase()) to props.getProperty(it) }))
                }
            }
        }
    }
}
