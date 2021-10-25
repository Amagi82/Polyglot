package project

import androidx.compose.runtime.Immutable
import locales.Locale
import locales.LocaleIsoCode
import utils.PropertyStore
import java.io.File

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
        val props = PropertyStore(
            PROP_ANDROID_OUTPUT to androidOutputUrl,
            PROP_IOS_OUTPUT to iosOutputUrl,
            PROP_DEFAULT_LOCALE to defaultLocale.value,
            PROP_LOCALES to locales.sorted().joinToString(",") { it.value })
        runCatching { props.store(projectFile(name), "Project settings for $name") }.onFailure {
            println("Failed to save settings with $it")
        }
    }

    private fun metadataFile(type: ResourceType) =
        File(projectFolder(name), "metadata.${type.title}.properties").apply(File::createNewFile)

    @Suppress("UNCHECKED_CAST")
    fun <M : Metadata> loadMetadata(type: ResourceType): Map<ResourceId, M> = buildMap {
        PropertyStore(metadataFile(type)).forEach { (k, v) ->
            val splits = v.split('|')
            val group = splits[0]
            val platforms = splits[1].split(',').filter(String::isNotEmpty).map(Platform::valueOf)
            put(
                ResourceId(k),
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
        val props = PropertyStore()
        metadata.forEach { (resId, metadata) ->
            props[resId.value] = "${metadata.group}|${metadata.platforms.sorted().joinToString(separator = ",") { it.name }}${
                when (metadata) {
                    is Str.Metadata -> ""
                    is Plural.Metadata -> "|${metadata.quantities.joinToString(separator = ",", transform = Quantity::name)}"
                    is StringArray.Metadata -> "|${metadata.size}"
                    else -> ""
                }
            }"
        }
        runCatching { props.store(metadataFile(type), "Resource metadata for ${type.title}") }.onFailure {
            println("Failed to save ${type.title} resources with $it")
        }
    }

    fun resourcesFile(type: ResourceType, locale: LocaleIsoCode) =
        File(projectFolder(name), "${type.title}.${locale.value}.properties").apply(File::createNewFile)

    @Suppress("UNCHECKED_CAST")
    fun <M : Metadata, R : Resource<M>> loadResources(type: ResourceType): Map<LocaleIsoCode, Map<ResourceId, R>> =
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

    fun <M : Metadata, R : Resource<M>> saveResources(localizedResources: Map<LocaleIsoCode, Map<ResourceId, R>>, type: ResourceType) {
        localizedResources.forEach { (locale, resources) ->
            val props = PropertyStore()
            resources.forEach { (k, v) ->
                when (v) {
                    is Str -> props[k.value] = v.text
                    is Plural -> v.items.forEach { (quantity, text) -> props["${k.value}.${quantity.label}"] = text }
                    is StringArray -> v.items.forEachIndexed { i, text -> props["${k.value}.$i"] = text }
                }
            }
            runCatching { props.store(resourcesFile(type, locale), "Localized ${type.title} in ${Locale[locale].displayName()}") }.onFailure {
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

        private fun projectFolder(projectName: String) = File(projectsFolder, projectName).apply(File::mkdirs)

        private fun projectFile(projectName: String) = File(projectFolder(projectName), "project.properties").apply(File::createNewFile)

        fun load(projectName: String): Project = with(PropertyStore(projectFile(projectName))) {
            Project(
                name = projectName,
                androidOutputUrl = get(PROP_ANDROID_OUTPUT) ?: "output/android",
                iosOutputUrl = get(PROP_IOS_OUTPUT) ?: "output/ios",
                defaultLocale = LocaleIsoCode(get(PROP_DEFAULT_LOCALE) ?: "en"),
                locales = get(PROP_LOCALES)?.split(",")?.filter(String::isNotEmpty)?.map(::LocaleIsoCode)?.sortedBy { Locale[it].displayName() } ?: listOf(LocaleIsoCode("en"))
            )
        }
    }
}
