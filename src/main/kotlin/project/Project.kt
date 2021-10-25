package project

import androidx.compose.runtime.Immutable
import locales.Locale
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
@Immutable
data class Project(
    val name: String,
    val androidOutputUrl: String = "output/android",
    val iosOutputUrl: String = "output/ios",
    val defaultLocale: LocaleIsoCode = LocaleIsoCode("en"),
    val locales: List<LocaleIsoCode> = listOf(defaultLocale)
) {

    fun save() {
        val file = projectFile(name)
        val props = Properties()
        props.setProperty(PROP_ANDROID_OUTPUT, androidOutputUrl)
        props.setProperty(PROP_IOS_OUTPUT, iosOutputUrl)
        props.setProperty(PROP_DEFAULT_LOCALE, defaultLocale.value)
        props.setProperty(PROP_LOCALES, locales.sorted().joinToString(",") { it.value })
        runCatching { props.store(file.outputStream(), "") }.onFailure {
            println("Failed to save settings with $it")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Metadata> loadMetadata(type: ResourceType): Map<ResourceId, M> = buildMap {
        val props = Properties().apply { load(metadataFile(name, type).inputStream()) }
        props.stringPropertyNames().forEach { k ->
            val v = props.getProperty(k)
            val splits = v.split('|')
            val resId = ResourceId(k)
            val group = splits[0]
            val platforms = splits[1].split(',').filter(String::isNotEmpty).map(Platform::valueOf)
            put(
                resId,
                when (type) {
                    ResourceType.STRINGS -> Str.Metadata(group, platforms)
                    ResourceType.PLURALS -> {
                        val quantities = splits.getOrElse(2) { "" }.split(',').filter(String::isNotEmpty).map(Quantity::valueOf)
                        Plural.Metadata(group, platforms, quantities)
                    }
                    ResourceType.ARRAYS -> StringArray.Metadata(group, platforms, splits.getOrNull(2)?.toInt() ?: 1)
                } as M
            )
        }
    }

    fun <M : Metadata> saveMetadata(metadata: Map<ResourceId, M>, type: ResourceType) {
        val file = metadataFile(name, type)
        val props = Properties()
        metadata.forEach { (resId, metadata) ->
            val extraData = when (metadata) {
                is Str.Metadata -> ""
                is Plural.Metadata -> "|${metadata.quantities.joinToString(separator = ",") { it.name }}"
                is StringArray.Metadata -> "|${metadata.size}"
                else -> ""
            }
            props.setProperty(resId.value, "${metadata.group}|${metadata.platforms.sorted().joinToString(separator = ",") { it.name }}$extraData")
        }
        runCatching { props.store(file.outputStream(), "") }.onFailure {
            println("Failed to save ${type.title} resources with $it")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Metadata, R : Resource<M>> loadResources(type: ResourceType): Map<LocaleIsoCode, Map<ResourceId, R>> =
        projectFolder(name).listFiles()?.filter { it.name.startsWith(type.title) && it.extension == "properties" }?.associate { file ->
            val locale = LocaleIsoCode(file.nameWithoutExtension.substringAfter('.'))
            val props = Properties().apply { load(resourcesFile(name, type, locale).inputStream()) }
            locale to buildMap<ResourceId, R> {
                when (type) {
                    ResourceType.STRINGS -> props.stringPropertyNames().forEach { id -> put(ResourceId(id), Str(props.getProperty(id)) as R) }
                    ResourceType.PLURALS -> props.stringPropertyNames().groupBy { it.substringBefore('.') }.forEach { (id, keys) ->
                        put(ResourceId(id), Plural(keys.associate { Quantity.valueOf(it.substringAfter('.').uppercase()) to props.getProperty(it) }) as R)
                    }
                    ResourceType.ARRAYS -> props.stringPropertyNames().groupBy { it.substringBefore('.') }.forEach { (id, keys) ->
                        put(ResourceId(id), StringArray(keys.sorted().map(props::getProperty)) as R)
                    }
                }
            }
        }.orEmpty()

    fun <M : Metadata, R : Resource<M>> saveResources(localizedResources: Map<LocaleIsoCode, Map<ResourceId, R>>, type: ResourceType) {
        localizedResources.forEach { (locale, resources) ->
            val file = resourcesFile(name, type, locale)
            val props = Properties()
            resources.forEach { (k, v) ->
                when (v) {
                    is Str -> props.setProperty(k.value, v.text)
                    is Plural -> v.items.forEach { (quantity, text) -> props.setProperty("${k.value}.${quantity.label}", text) }
                    is StringArray -> v.items.forEachIndexed { i, text -> props.setProperty("${k.value}.$i", text) }
                }
            }
            runCatching { props.store(file.outputStream(), "") }.onFailure {
                println("Failed to save localized ${type.title} resources with $it")
            }
        }
    }

    companion object {
        private const val PROP_ANDROID_OUTPUT = "androidOutputUrl"
        private const val PROP_IOS_OUTPUT = "iosOutputUrl"
        private const val PROP_DEFAULT_LOCALE = "defaultLocale"
        private const val PROP_LOCALES = "locales"

        val projectsFolder = File("projects").apply(File::mkdirs)

        val projectFolders = projectsFolder.listFiles()?.filter(File::isDirectory).orEmpty()

        private fun projectFolder(projectName: String) = File(projectsFolder, projectName).apply(File::mkdirs)

        private fun projectFile(projectName: String) = File(projectFolder(projectName), "project.properties").apply(File::createNewFile)

        fun metadataFile(projectName: String, type: ResourceType) =
            File(projectFolder(projectName), "metadata.${type.title}.properties").apply(File::createNewFile)

        fun resourcesFile(projectName: String, type: ResourceType, locale: LocaleIsoCode) =
            File(projectFolder(projectName), "${type.title}.${locale.value}.properties").apply(File::createNewFile)

        fun load(projectName: String): Project {
            val file = projectFile(projectName)
            val props = Properties().apply { load(file.inputStream()) }
            return Project(
                name = projectName,
                androidOutputUrl = props.getProperty(PROP_ANDROID_OUTPUT, "output/android"),
                iosOutputUrl = props.getProperty(PROP_IOS_OUTPUT, "output/ios"),
                defaultLocale = LocaleIsoCode(props.getProperty(PROP_DEFAULT_LOCALE, "en")),
                locales = props.getProperty(PROP_LOCALES, "en").split(",").filter(String::isNotEmpty).map(::LocaleIsoCode).sortedBy { Locale[it].displayName() }
            )
        }
    }
}
