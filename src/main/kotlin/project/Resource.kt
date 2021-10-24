package project

import androidx.compose.runtime.Stable

/**
 * A Resource is a type of localized content: A string, a plural, or a string array.
 * Strings, Plurals, and Arrays each require different formatting on Android and iOS.
 */
@Stable
sealed interface Resource

@Stable
@JvmInline
value class Str(val text: String = "") : Resource

@Stable
@JvmInline
value class Plural(val items: Map<Quantity, String> = mapOf(Quantity.ONE to "", Quantity.OTHER to "")) : Resource {
    constructor(
        zero: String? = null,
        one: String?,
        two: String? = null,
        few: String? = null,
        many: String? = null,
        other: String
    ) : this(buildMap {
        if (zero != null) put(Quantity.ZERO, zero)
        if (one != null) put(Quantity.ONE, one)
        if (two != null) put(Quantity.TWO, two)
        if (few != null) put(Quantity.FEW, few)
        if (many != null) put(Quantity.MANY, many)
        put(Quantity.OTHER, other)
    })

    operator fun get(quantity: Quantity) = items[quantity]
}

@Stable
@JvmInline
value class StringArray(val items: List<String> = listOf()) : Resource
