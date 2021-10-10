package project

import androidx.compose.runtime.Stable
import locales.LocaleIsoCode
import java.util.*

/**
 * A Resource is a String, Plural, or StringArray to be localized.
 * Strings, Plurals, and Arrays each require different formatting on Android and iOS.
 *
 * @property group - optional group to bundle the resource in for organizational purposes, ignored if empty
 * @property platforms: this resource is only included in the provided platform(s)
 */
@Stable
data class Resource(
    val group: String = "",
    val platforms: List<Platform> = Platform.ALL,
    val type: Type = Type.STRING
) {
    @Stable
    enum class Type {
        STRING, PLURAL, ARRAY
    }
}

/**
 * @property id: unique identifier for the resource. Android will convert this to snake_case, and iOS will convert this to camelCase
 */
@Stable
@JvmInline
value class ResourceId(val id: String)

typealias Resources = Map<ResourceId, Resource>
typealias Localizations = Map<ResourceId, Localization>
typealias LocalizedResources = Map<LocaleIsoCode, Localizations>

@JvmName("saveResources")
fun Resources.save(projectName: String) {
    val file = Project.resourcesFile(projectName)
    if (isEmpty()) {
        file.delete()
        return
    }
    val props = Properties()
    forEach { (id, resource) ->
        props.setProperty(id.id, "${resource.type.name}|${resource.platforms.joinToString(separator = ",") { it.name }}|${resource.group}")
    }
    runCatching { props.store(file.outputStream(), "") }.onFailure {
        println("Failed to save resources with $it")
    }
}

@JvmName("saveLocalizedResources")
fun LocalizedResources.save(projectName: String) {
    Project.localizedResourceFiles(projectName).filter { LocaleIsoCode(it.nameWithoutExtension) !in keys }.forEach { it.delete() }
    forEach { (locale, resources) ->
        val file = Project.localizedResourcesFile(projectName, locale)
        if (resources.isEmpty()) {
            file.delete()
            return@forEach
        }
        val props = Properties()
        resources.forEach { (k, v) ->
            when (v) {
                is Str -> props.setProperty(k.id, v.text)
                is Plural -> v.items.forEach { (quantity, text) -> props.setProperty("${k.id}.${quantity.name.lowercase()}", text) }
                is StringArray -> v.items.forEachIndexed { i, text -> props.setProperty("${k.id}.$i", text) }
            }

        }
        runCatching { props.store(file.outputStream(), "") }.onFailure {
            println("Failed to save localized resources with $it")
        }
    }
}


@Stable
sealed interface Localization

@Stable
@JvmInline
value class Str(val text: String) : Localization

@Stable
@JvmInline
value class Plural(val items: Map<Quantity, String>) : Localization {
    constructor(
        zero: String? = null,
        one: String?,
        two: String? = null,
        few: String? = null,
        many: String? = null,
        other: String
    ) : this(buildMap {
        if (zero != null) put(Quantity.ZERO, zero)
        if (one != null) put(Quantity.ONE, one)
        if (two != null) put(Quantity.TWO, two)
        if (few != null) put(Quantity.FEW, few)
        if (many != null) put(Quantity.MANY, many)
        put(Quantity.OTHER, other)
    })

    operator fun get(quantity: Quantity) = items[quantity]
}

@Stable
@JvmInline
value class StringArray(val items: List<String>) : Localization
