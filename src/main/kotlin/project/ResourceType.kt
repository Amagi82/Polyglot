package project

import androidx.compose.runtime.Immutable

@Immutable
enum class ResourceType {
    STRINGS, PLURALS, ARRAYS;

    val index: Int
        get() = when (this) {
            STRINGS -> 0
            PLURALS -> 1
            ARRAYS -> 2
        }
}
