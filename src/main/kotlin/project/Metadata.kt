package project

import androidx.compose.runtime.Immutable

@Immutable
data class Metadata(
    val type: ResourceType,
    val group: GroupId = GroupId(""),
    val platforms: List<Platform> = Platform.ALL
)
