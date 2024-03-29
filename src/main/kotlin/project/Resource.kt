package project

import androidx.compose.runtime.Immutable
import kotlin.reflect.KClass

/**
 * A Resource is a type of localized content: A string, a plural, or a string array.
 * Strings, Plurals, and Arrays each require different formatting on Android and iOS.
 */
@Immutable
sealed interface Resource {
    val isValid: Boolean
}

@Immutable
@JvmInline
value class Str(val text: String = "") : Resource {
    override val isValid: Boolean get() = text.isNotEmpty()
}

@Immutable
@JvmInline
value class Plural(val items: Map<Quantity, String> = mapOf(Quantity.ONE to "", Quantity.OTHER to "")) : Resource {
    operator fun get(quantity: Quantity) = items[quantity]

    override val isValid: Boolean get() = Quantity.values().filter { it.isRequired }.none { items[it].isNullOrEmpty() }
}

@Immutable
@JvmInline
value class StringArray(val items: List<String> = listOf()) : Resource {
    override val isValid: Boolean get() = items.isNotEmpty() && items.none(String::isEmpty)
}

val <R : Resource> KClass<R>.type
    get() = when (this) {
        Str::class -> ResourceType.STRINGS
        Plural::class -> ResourceType.PLURALS
        StringArray::class -> ResourceType.ARRAYS
        else -> throw IllegalStateException("Could not resolve ResourceType from: $this")
    }
