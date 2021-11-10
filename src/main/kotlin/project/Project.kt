package project

import androidx.compose.runtime.Immutable
import locales.Locale
import locales.LocaleIsoCode
import utils.PropertyStore
import java.io.File

/**
 * @param name: Name of the project
 * @param exportUrls: Set this to your project folders and Polyglot will generate files directly to that folder
 * @param defaultLocale: The current locale set as default, which will be used as backup if no suitable translation is found.
 * Android and iOS handle this differently:
 * With Android, the base values folder gets the default strings, and values-en, values-de, etc get the localized translations
 * With iOS, there is no base folder, all localizations are placed in their respective folders, e.g. en.proj, es.proj, de.proj
 * */
@Immutable
data class Project(
    val name: String,
    val exportUrls: Map<Platform, String> = mapOf(),
    val defaultLocale: LocaleIsoCode = LocaleIsoCode("en"),
    val locales: List<LocaleIsoCode> = listOf(defaultLocale)
) {

    fun save() {
        val props = PropertyStore(
            PROP_DEFAULT_LOCALE to defaultLocale.value,
            PROP_LOCALES to locales.sorted().joinToString(",") { it.value })
        Platform.values().forEach { platform ->
            props["$PROP_EXPORT_URLS.${platform.name.lowercase()}"] = exportUrls[platform] ?: "output/${platform.name.lowercase()}"
        }
        runCatching { props.store(projectFile(name), "Project settings for $name") }.onFailure {
            println("Failed to save settings with $it")
        }
    }

    private fun metadataFile(type: ResourceType) =
        File(projectFolder(name), "metadata.${type.title}.properties").apply(File::createNewFile)

    @Suppress("UNCHECKED_CAST")
    fun loadMetadata(type: ResourceType): Map<ResourceId, Metadata> = buildMap {
        PropertyStore(metadataFile(type)).entries.groupBy { it.key.substringBefore('.') }.forEach { (k, v) ->
            val group = v.find { it.key.endsWith("group") }?.value.orEmpty()
            val platforms = v.find { it.key.endsWith("platforms") }?.value.orEmpty().split(',').filter(String::isNotEmpty).map(Platform::valueOf)
            put(ResourceId(k), Metadata(type, GroupId(group), platforms))
        }
    }

    fun saveMetadata(metadata: Map<ResourceId, Metadata>, type: ResourceType) {
        val props = PropertyStore()
        metadata.forEach { (resId, metadata) ->
            props["${resId.value}.group"] = metadata.group.value
            props["${resId.value}.platforms"] = metadata.platforms.sorted().joinToString(separator = ",") { it.name }
        }
        runCatching { props.store(metadataFile(type), "Resource metadata for ${type.title}") }.onFailure {
            println("Failed to save ${type.title} resources with $it")
        }
    }

    private val arraySizesFile get() = File(projectFolder(name), "sizes.${ResourceType.ARRAYS.title}.properties").apply(File::createNewFile)

    fun loadArraySizes(): Map<ResourceId, Int> = PropertyStore(arraySizesFile).entries.associate { (k, v) -> ResourceId(k) to (v.toIntOrNull() ?: 1) }

    fun saveArraySizes(sizesMap: Map<ResourceId, Int>) {
        val props = PropertyStore(sizesMap.entries.associate { (k, v) -> k.value to "$v" })
        runCatching { props.store(arraySizesFile, "Required sizes for ${ResourceType.ARRAYS.title}") }.onFailure {
            println("Failed to save sizes of ${ResourceType.ARRAYS.title} with $it")
        }
    }

    fun resourcesFile(type: ResourceType, locale: LocaleIsoCode) =
        File(projectFolder(name), "${type.title}.${locale.value}.properties").apply(File::createNewFile)

    @Suppress("UNCHECKED_CAST")
    fun <R : Resource> loadResources(type: ResourceType): Map<LocaleIsoCode, Map<ResourceId, R>> =
        projectFolder(name).listFiles()?.filter { it.name.startsWith(type.title) && it.extension == "properties" }?.associate { file ->
            val locale = LocaleIsoCode(file.nameWithoutExtension.substringAfter('.'))
            val props = PropertyStore(resourcesFile(type, locale))
            locale to buildMap<ResourceId, R> {
                when (type) {
                    ResourceType.STRINGS -> props.entries.forEach { (k, v) -> put(ResourceId(k), Str(v) as R) }
                    ResourceType.PLURALS -> props.entries.groupBy { (k, _) -> k.substringBefore('.') }.forEach { (k, v) ->
                        put(ResourceId(k), Plural(v.associate { Quantity.valueOf(it.key.substringAfter('.').uppercase()) to it.value }) as R)
                    }
                    ResourceType.ARRAYS -> props.entries.groupBy { (k, _) -> k.substringBefore('.') }.forEach { (k, v) ->
                        put(ResourceId(k), StringArray(v.sortedBy { it.key }.map { it.value }) as R)
                    }
                }
            }
        }.orEmpty()

    fun <R : Resource> saveResources(localizedResources: Map<LocaleIsoCode, Map<ResourceId, R>>, type: ResourceType) {
        localizedResources.forEach { (locale, resources) ->
            val props = PropertyStore()
            resources.forEach { (k, v) ->
                when (v) {
                    is Str -> props[k.value] = v.text
                    is Plural -> v.items.forEach { (quantity, text) ->
                        val key = "${k.value}.${quantity.label}"
                        if (quantity.isRequired || text.isNotEmpty()) props[key] = text
                        else props.remove(key)
                    }
                    is StringArray -> v.items.forEachIndexed { i, text -> props["${k.value}.$i"] = text }
                }
            }
            runCatching { props.store(resourcesFile(type, locale), "Localized ${type.title} in ${Locale[locale].displayName()}") }.onFailure {
                println("Failed to save localized ${type.title} resources with $it")
            }
        }
    }

    companion object {
        private const val PROP_EXPORT_URLS = "exportUrls"
        private const val PROP_DEFAULT_LOCALE = "defaultLocale"
        private const val PROP_LOCALES = "locales"

        val projectsFolder = File("projects").apply(File::mkdirs)

        fun projectFolder(projectName: String) = File(projectsFolder, projectName).apply(File::mkdirs)

        private fun projectFile(projectName: String) = File(projectFolder(projectName), "project.properties").apply(File::createNewFile)

        fun load(projectName: String): Project = with(PropertyStore(projectFile(projectName))) {
            Project(
                name = projectName,
                exportUrls = filter { it.key.startsWith(PROP_EXPORT_URLS) }
                    .map { it.key.substringAfter('.').uppercase().let(Platform::valueOf) to it.value }.toMap(),
                defaultLocale = LocaleIsoCode(get(PROP_DEFAULT_LOCALE) ?: "en"),
                locales = get(PROP_LOCALES)?.split(",")?.filter(String::isNotEmpty)?.map(::LocaleIsoCode)?.sortedBy { Locale[it].displayName() } ?: listOf(
                    LocaleIsoCode("en")
                )
            )
        }
    }
}
