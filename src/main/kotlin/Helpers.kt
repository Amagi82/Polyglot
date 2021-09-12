import locales.Locale
import locales.LocaleIsoCode
import locales.Localizations
import resources.Platform
import resources.Plural
import resources.Str
import resources.StringArray

fun string(
    id: String,
    default: String,
    vararg translations: Pair<LocaleIsoCode, String>,
    platforms: List<Platform> = Platform.ALL,
) = Str(
    id = id,
    platforms = platforms,
    localizations = mapOf(Locale.default to default, *translations)
)

fun plural(
    id: String,
    zero: Localizations? = null,
    one: Localizations?,
    two: Localizations? = null,
    few: Localizations? = null,
    many: Localizations? = null,
    other: Localizations,
    platforms: List<Platform> = Platform.ALL
) = Plural(id = id, platforms = platforms, zero = zero, one = one, two = two, few = few, many = many, other = other)

//fun stringArray(
//    id: String,
//    vararg items: Localizations,
//    platforms: List<Platform> = Platform.ALL
//) = StringArray(id = id, platforms = platforms, items = items.toList())