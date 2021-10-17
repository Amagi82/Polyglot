package project

import androidx.compose.runtime.Stable

/**
 * ResourceInfo provides metadata for a given Resource.
 *
 * @property group - optional group to bundle the resource in for organizational purposes, ignored if empty
 * @property platforms: this resource is only included in the provided platform(s)
 * @property type: expected type of this resource
 */
@Stable
data class ResourceInfo(
    val group: String = "",
    val platforms: List<Platform> = Platform.ALL,
    val type: Type = Type.STRING
) {
    @Stable
    enum class Type {
        STRING, PLURAL, ARRAY
    }
}
