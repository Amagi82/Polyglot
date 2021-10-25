package locales

import androidx.compose.runtime.Immutable

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
@Immutable
data class Locale(val language: Language, val region: Region? = null) {
    val isoCode: LocaleIsoCode = LocaleIsoCode(language.isoCode.value + region?.isoCode?.value?.let { "_$it" }.orEmpty())
    fun displayName(isDefault: Boolean = false) = "${language.name}${region?.name?.let { " ($it)" }.orEmpty()}${if (isDefault) " (Default)" else ""}"

    companion object {
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
 *
 * @property isBaseLanguage: False if the isoCode is a regional dialect
 * */
@Immutable
@JvmInline
value class LocaleIsoCode(val value: String) : Comparable<LocaleIsoCode> {
    val isBaseLanguage: Boolean get() = !value.contains('_')
    override fun compareTo(other: LocaleIsoCode): Int = value.compareTo(other.value)
}
