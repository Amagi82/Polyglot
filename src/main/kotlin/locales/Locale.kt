package locales

import androidx.compose.runtime.Stable

/**
 * Language and optional Region for a translation
 *
 * Android and iOS use these to choose the appropriate localization for a given user.
 * This is used by Polyglot to organize resources into appropriate folders, e.g.:
 * Android: values (default, usually English), values.es, values.fr_ca, etc.
 * iOS: en.lproj, es.lproj, fr_ca.lproj, etc.
 *
 * Generally, the language code alone should be used for most values, with country code provided
 * when you want to customize the translation for a given region. If you only provide es_ec and en,
 * the device may show the English localization even if the user prefers Spanish.
 *
 * The default is mandatory, it's what the system will default to if the user's chosen language/region is unavailable
 * Every language should include a default (null) region, and then override for regional differences as needed
 *
 * @param language: The base language
 * @param region: An optional regional dialect, or null for the default version
 * */
@Stable
data class Locale(val language: Language, val region: Region? = null) : Comparable<Locale> {
    val isoCode: LocaleIsoCode = LocaleIsoCode(language.isoCode.value + region?.isoCode?.value?.let { "_$it" }.orEmpty())
    val displayName = "${language.name}${region?.name?.let { " ($it)" }.orEmpty()}${if(isoCode == default)" (Default)" else ""}"

    override fun compareTo(other: Locale): Int = isoCode.compareTo(other.isoCode)

    /**
     * @property default: The current locale set as default. Android and iOS handle this differently.
     * With Android, the base values folder gets the default strings, and values-en, values-de, etc get the localized translations
     * With iOS, there is no base folder, all localizations are placed in their respective folders, e.g. en.proj, es.proj, de.proj
     * */
    companion object {
        var default = LocaleIsoCode("en")

        operator fun get(isoCode: LocaleIsoCode) = all[isoCode] ?: throw IllegalStateException("$isoCode not found in Locale list")

        val all: Map<LocaleIsoCode, Locale> by lazy {
            buildMap {
                Language.names.forEach { (langIsoCode, langName) ->
                    put(LocaleIsoCode(langIsoCode.value), Locale(language = Language(langIsoCode, langName)))
                    Language.regions[langIsoCode]?.forEach { regionIsoCode ->
                        put(
                            LocaleIsoCode("${langIsoCode.value}_${regionIsoCode.value}"),
                            Locale(language = Language[langIsoCode], region = Region[regionIsoCode])
                        )
                    }
                }
            }
        }
    }
}

/**
 * LanguageIsoCode + optional RegionIsoCode, separated by _
 *
 * Used to place the translation in the appropriate folder, e.g.
 * Android: values.fr, values.en_US, values.es_AR, values.fr_GF, etc
 * iOS: fr.lproj, en_US.lproj, es_AR.lproj, fr_GF.lproj, etc
 * */
@Stable
@JvmInline
value class LocaleIsoCode(val value: String) : Comparable<LocaleIsoCode> {
    override fun compareTo(other: LocaleIsoCode): Int = when {
        this == Locale.default && other != Locale.default -> -1
        other == Locale.default && this != Locale.default -> 1
        else -> value.compareTo(other.value)
    }
}
