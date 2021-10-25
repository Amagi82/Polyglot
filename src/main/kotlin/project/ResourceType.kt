package project

import androidx.compose.runtime.Immutable

@Immutable
enum class ResourceType {
    STRINGS, PLURALS, ARRAYS;

    val title: String get() = name.lowercase()
}
