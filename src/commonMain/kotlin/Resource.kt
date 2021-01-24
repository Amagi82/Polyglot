import kotlinx.serialization.Serializable

/**
 * A Resource is a String, Plural, or String Array resource to be localized.
 * Strings, Plurals, and Arrays each require different formatting on Android and iOS.
 *
 * @property id: should be snake case with all lowercase letters. iOS doesn't care about this, but Android does.
 * @property platforms: specify this resource should only be localized for a given platform
 * @property locales - a set of all locales
 */
@Serializable
sealed class Resource {
    abstract val id: String
    abstract val platforms: List<Platform>?
    abstract val locales: Set<LocaleIsoCode>

    /**
     * @param locale: A locale to check
     * @return true if the resource is not overridden in this localization
     * */
    abstract fun shouldSkip(locale: LocaleIsoCode): Boolean

    protected fun validateId() {
        if (id.toLowerCase() != id) throw IllegalArgumentException("Resource ids must be all lower case")
    }
}

@Serializable
data class Str(
    override val id: String,
    override val platforms: List<Platform>? = null,
    private val localizations: Localizations
) : Resource() {
    operator fun get(key: LocaleIsoCode) = localizations.getRequired(key)
    val hasTranslations = localizations.hasTranslations
    override val locales: Set<LocaleIsoCode> get() = localizations.locales

    override fun shouldSkip(locale: LocaleIsoCode) = !locale.isDefault && locale !in localizations

    init {
        validateId()
    }
}

@Serializable
data class Plural(
    override val id: String,
    override val platforms: List<Platform>? = null,
    val zero: Localizations? = null,
    val one: Localizations?,
    val two: Localizations? = null,
    val few: Localizations? = null,
    val many: Localizations? = null,
    val other: Localizations
) : Resource() {
    fun quantity(quantity: Quantities) = when (quantity) {
        Quantities.ZERO -> zero
        Quantities.ONE -> one
        Quantities.TWO -> two
        Quantities.FEW -> few
        Quantities.MANY -> many
        Quantities.OTHER -> other
    }

    override val locales: Set<LocaleIsoCode> get() = other.locales

    // Other is required. All others are optional
    override fun shouldSkip(locale: LocaleIsoCode) = !locale.isDefault && locale !in other

    init {
        validateId()
    }
}

@Serializable
data class StringArray(
    override val id: String,
    override val platforms: List<Platform>? = null,
    val items: List<Localizations>
) : Resource() {
    override val locales: Set<LocaleIsoCode> get() = items.first().locales

    // All entries must be translated
    override fun shouldSkip(locale: LocaleIsoCode) = !locale.isDefault && items.none { it.contains(locale) }

    init {
        validateId()
    }
}
