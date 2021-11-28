package project

import androidx.compose.runtime.Immutable

/**
 * ResourceGroup are optional tags for organizing resources into categories
 * @property name: name for the resource group
 */
@Immutable
@JvmInline
value class ResourceGroup(val name: String = "") : Comparable<ResourceGroup> {
    override fun compareTo(other: ResourceGroup): Int = name.compareTo(other.name)
}
