package locales

import exceptions.MissingResourceException

//@Serializable
data class Localizations(private val map: Map<LocaleIsoCode, String>) {

    constructor(default: String, vararg translations: Pair<LocaleIsoCode, String>) : this(map = mutableMapOf(Locale.default to default).apply {
        translations.forEach { put(it.first, it.second) }
    })

    operator fun contains(locale: LocaleIsoCode) = map.contains(locale)

    fun get(locale: LocaleIsoCode, isRequired: Boolean = locale.isDefault): String? = if (isRequired) getRequired(locale) else map[locale]

    fun getRequired(locale: LocaleIsoCode): String = map[locale] ?: throw MissingResourceException("localization for $locale missing in $map")

    val hasTranslations get() = map.size > 1

    val locales: Set<LocaleIsoCode> get() = map.keys
}
