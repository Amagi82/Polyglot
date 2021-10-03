package locales

import androidx.compose.runtime.Stable

/**
 * locales.Language and optional locales.Region for a translation
 *
 * The default is mandatory, it's what the system will default to if the user's chosen language/region is unavailable
 * Every language should include a default (null) region, and then override for regional differences as needed
 *
 * @param language: The base language
 * @param region: An optional regional dialect, or null for the default version
 * */
//@Stable
//data class Locale(val language: Language, val region: Region? = null) : Comparable<Locale> {
//    // Must use a getter until this is resolved: https://github.com/Kotlin/kotlinx.serialization/issues/716
//    val isoCode: LocaleIsoCode get() = LocaleIsoCode(language.isoCode.value + region?.isoCode?.value?.let { "_$it" }.orEmpty())
//    val name get() = "$isoCode [${language.name}${region?.name?.let { " ($it)" }.orEmpty()}]"
//
//    override fun compareTo(other: Locale): Int = when {
//        language.isoCode.value == default && other.language.isoCode.value != default -> -1
//        other.language.isoCode.value == default && language.isoCode.value != default -> 1
//        else -> isoCode.value.compareTo(other.isoCode.value)
//    }
//
//    /**
//     * @property default: The current locale set as default. Android and iOS handle this differently.
//     * With Android, the base values folder gets the default strings, and values-en, values-de, etc get the localized translations
//     * With iOS, there is no base folder, all localizations are placed in their respective folders, e.g. en.proj, es.proj, de.proj
//     * */
//    companion object {
//        var default = "en"
//
////        val all: List<Locale> by lazy {
////            Language.names.keys.fold(mutableListOf()) { acc, languageIsoCode ->
////                acc.add(Locale(language = Language[languageIsoCode]))
////                Language.regions[languageIsoCode]?.forEach { regionIsoCode ->
////                    acc.add(Locale(language = Language[languageIsoCode], region = Region[regionIsoCode]))
////                }
////                acc
////            }
////        }
//    }
//}

@JvmInline
value class LocaleIsoCode(val value: String)

//val LocaleIsoCode.isDefault: Boolean get() = value == Locale.default
