package locales

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * locales.Language and optional locales.Region for a translation
 *
 * The default is mandatory, it's what the system will default to if the user's chosen language/region is unavailable
 * Every language should include a default (null) region, and then override for regional differences as needed
 *
 * @param language: The base language
 * @param region: An optional regional dialect, or null for the default version
 * */
@Stable
@Serializable
data class Locale(val language: Language, val region: Region? = null) : Comparable<Locale> {
    // Must use a getter until this is resolved: https://github.com/Kotlin/kotlinx.serialization/issues/716
    val isoCode: LocaleIsoCode get() = language.isoCode + region?.isoCode?.let { "_$it" }.orEmpty()
    val name get() = "$isoCode [${language.name}${region?.name?.let { " ($it)" }.orEmpty()}]"

    override fun compareTo(other: Locale): Int = when {
        language.isoCode == default && other.language.isoCode != default -> -1
        other.language.isoCode == default && language.isoCode != default -> 1
        else -> isoCode.compareTo(other.isoCode)
    }

    /**
     * @property default: The current locale set as default. Android and iOS handle this differently.
     * With Android, the base values folder gets the default strings, and values-en, values-de, etc get the localized translations
     * With iOS, there is no base folder, all localizations are placed in their respective folders, e.g. en.proj, es.proj, de.proj
     * */
    companion object {
        var default = "en"
//        const val path = "/locales"

//        val all: List<locales.Locale>
//            get() = locales.Language.names.keys.fold(mutableListOf()) { acc, languageIsoCode ->
//                acc.add(locales.Locale(language = locales.Language[languageIsoCode]))
////                val regions = locales.Language.regionCodes(languageIsoCode)
////                regions?.forEach { regionIsoCode -> acc.add(locales.Locale(language = locales.Language[languageIsoCode], region = locales.Region[regionIsoCode])) }
//                acc
//            }
    }
}

typealias LocaleIsoCode = String

val LocaleIsoCode.isDefault: Boolean get() = this == Locale.default




