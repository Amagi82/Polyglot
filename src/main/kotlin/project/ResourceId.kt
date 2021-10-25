package project

import androidx.compose.runtime.Stable

/**
 * @property value: unique identifier for the resource. Android will convert this to snake_case, and iOS will convert this to camelCase
 */
@Stable
@JvmInline
value class ResourceId(val value: String) : Comparable<ResourceId> {
    override fun compareTo(other: ResourceId): Int = value.compareTo(other.value)
}
