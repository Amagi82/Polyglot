fun string(
    id: String,
    default: String,
    vararg translations: Pair<LocaleIsoCode, String>,
    platforms: List<Platform>? = null,
) = Resource.Str(
    id = id,
    platforms = platforms,
    localizations = Localizations(default, *translations)
)

fun plural(
    id: String,
    zero: Localizations? = null,
    one: Localizations?,
    two: Localizations? = null,
    few: Localizations? = null,
    many: Localizations? = null,
    other: Localizations,
    platforms: List<Platform>? = null
) = Resource.Plural(id = id, platforms = platforms, zero = zero, one = one, two = two, few = few, many = many, other = other)

fun stringArray(
    id: String,
    vararg items: Localizations,
    platforms: List<Platform>? = null
) = Resource.StringArray(id = id, platforms = platforms, items = items.toList())
