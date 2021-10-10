package project

import androidx.compose.runtime.Stable

/**
 * @property id: unique identifier for the resource. Android will convert this to snake_case, and iOS will convert this to camelCase
 */
@Stable
@JvmInline
value class ResourceId(val id: String) : Comparable<ResourceId> {
    override fun compareTo(other: ResourceId): Int = id.compareTo(other.id)
}
