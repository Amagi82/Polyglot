package locales

import androidx.compose.runtime.Stable
import utils.extensions.loadResource
import kotlinx.serialization.Serializable

/**
 * Two or Three digit code (e.g. en, es, fr, de, tzm), used to place the translation in the appropriate folder, e.g.
 * Android: values.en, values.es, etc
 * iOS: en.lproj, es.lproj, etc
 */
typealias LanguageIsoCode = String

/**
 * @param isoCode - ISO 639 language codes + optional ISO 3166 country codes.
 *
 * Android and iOS use these to choose the appropriate localization for a given user.
 *
 * Generally, the language code alone should be used for most values, with country code provided
 * when you want to customize the translation for a given region. If you only provide es_ec and en,
 * the device may show the English localization even if the user prefers Spanish.
 *
 * This is used by Polyglot to organize resources into appropriate folders, e.g.:
 * Android: values (default, usually English), values.es, values.fr_ca, etc.
 * iOS: en.lproj, es.lproj, fr_ca.lproj, etc.
 *
 * English, Spanish, French, and German have been added for convenience.
 */
@Stable
@Serializable
data class Language(val isoCode: LanguageIsoCode, val name: String) {
    init {
        when {
            isoCode.isBlank() -> throw IllegalArgumentException("locales.Language isoCode cannot be blank")
            isoCode.lowercase() != isoCode -> throw IllegalArgumentException("isoCode $isoCode should contain lower case letters only")
            isoCode.length == 2 -> Unit // Good
            isoCode.length == 5 && isoCode[2] == '_' -> Unit // Good
            else -> throw IllegalArgumentException("Invalid language isoCode: $isoCode. Should be in the format \"en\" or \"en_us\"")
        }
    }

    companion object {
        operator fun get(isoCode: LanguageIsoCode) = Language(isoCode = isoCode, name = names[isoCode]!!)

        /**
         * Name of language from language isoCode, e.g. ["en"] = English, ["fr"] = French
         */
        val names = loadResource<Map<LanguageIsoCode, String>>("languages.json")

        /**
         * List of regions where a language is spoken
         */
        val regions = loadResource<Map<LanguageIsoCode, List<RegionIsoCode>>>("language_regions.json")
    }
}
