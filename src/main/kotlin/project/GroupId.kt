package project

import androidx.compose.runtime.Immutable

/**
 * Groups are optional tags for organizing resources into categories
 * @property value: group for the resource
 */
@Immutable
@JvmInline
value class GroupId(val value: String = "") : Comparable<GroupId> {
    override fun compareTo(other: GroupId): Int = value.compareTo(other.value)
}
