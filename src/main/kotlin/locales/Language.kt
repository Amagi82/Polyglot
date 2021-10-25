package locales

import androidx.compose.runtime.Immutable
import utils.extensions.loadResource

/**
 * @param isoCode ISO 639 language codes, e.g. fr, ru, sv
 * @param name Anglicized name of the language, .e.g. French, Russian, Swedish
 */
@Immutable
data class Language(val isoCode: LanguageIsoCode, val name: String) {
    init {
        when {
            isoCode.value.isBlank() -> throw IllegalArgumentException("Language isoCode cannot be blank")
            isoCode.value.lowercase()
                .filter(Char::isLetter) != isoCode.value -> throw IllegalArgumentException("Language isoCode $isoCode should contain lowercase letters only")
            isoCode.value.length in 2..3 -> Unit // Good
            else -> throw IllegalArgumentException("Invalid language isoCode: $isoCode. Must comply with ISO 639, lowercase two or three letter abbreviations, e.g. \"en\"")
        }
    }

    companion object {
        operator fun get(isoCode: LanguageIsoCode) = Language(isoCode = isoCode, name = names[isoCode]!!)

        /**
         * Name of language from language isoCode, e.g. ["en"] = English, ["fr"] = French
         */
        val names: Map<LanguageIsoCode, String> = loadResource("languages.properties") { (k, v) -> LanguageIsoCode(k) to v }

        /**
         * List of regions where a language is spoken
         */
        val regions: Map<LanguageIsoCode, List<RegionIsoCode>> = loadResource("language_regions.properties") { (k, v) ->
            LanguageIsoCode(k) to v.split(',').filter(String::isNotEmpty).map(::RegionIsoCode)
        }
    }
}

/**
 * ISO 639 Two or Three digit code (e.g. en, es, fr, de, tzm)
 */
@Immutable
@JvmInline
value class LanguageIsoCode(val value: String)
