package locales

import exceptions.MissingResourceException

typealias Localizations = Map<LocaleIsoCode, String>

fun Localizations.get(locale: LocaleIsoCode, isRequired: Boolean = locale.isDefault): String? = if (isRequired) getRequired(locale) else get(locale)
fun Localizations.getRequired(locale: LocaleIsoCode): String = get(locale) ?: throw MissingResourceException("localization for $locale missing in $this")
val Localizations.locales: Set<LocaleIsoCode> get() = keys

//@Stable
//@Serializable
//@JvmInline
//value class Localizations(val map: Map<LocaleIsoCode, String>) {
//
//    constructor(default: String, vararg translations: Pair<LocaleIsoCode, String>) : this(map = mutableMapOf(Locale.default to default).apply {
//        translations.forEach { put(it.first, it.second) }
//    })
//
//    operator fun contains(locale: LocaleIsoCode) = map.contains(locale)
//
//    fun get(locale: LocaleIsoCode, isRequired: Boolean = locale.isDefault): String? = if (isRequired) getRequired(locale) else map[locale]
//
//    fun getRequired(locale: LocaleIsoCode): String = map[locale] ?: throw MissingResourceException("localization for $locale missing in $map")
//
//    val hasTranslations get() = map.size > 1
//
//    val locales: Set<LocaleIsoCode> get() = map.keys
//}
