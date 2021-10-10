package project

import androidx.compose.runtime.Stable

/**
 * A Resource is a String, Plural, or StringArray to be localized.
 * Strings, Plurals, and Arrays each require different formatting on Android and iOS.
 *
 * @property group - optional group to bundle the resource in for organizational purposes, ignored if empty
 * @property platforms: this resource is only included in the provided platform(s)
 */
@Stable
data class Resource(
    val group: String = "",
    val platforms: List<Platform> = Platform.ALL,
    val type: Type = Type.STRING
) {
    @Stable
    enum class Type {
        STRING, PLURAL, ARRAY
    }
}
