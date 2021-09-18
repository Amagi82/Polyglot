package locales

typealias Localizations = Map<LocaleIsoCode, String>

fun Localizations.get(locale: LocaleIsoCode, isRequired: Boolean = locale.isDefault): String? = if (isRequired) getRequired(locale) else get(locale)
fun Localizations.getRequired(locale: LocaleIsoCode): String = get(locale) ?: throw IllegalStateException("localization for $locale missing in $this")
val Localizations.locales: Set<LocaleIsoCode> get() = keys
