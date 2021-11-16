package project

import androidx.compose.runtime.Immutable

@Immutable
data class Metadata(
    val type: ResourceType,
    val group: GroupId = GroupId(""),
    val platforms: List<Platform> = Platform.ALL
){
    companion object{
        const val PROP_GROUP = "group"
        const val PROP_PLATFORMS = "platforms"

        fun groupKey(resId: ResourceId) = "${resId.value}.$PROP_GROUP"
        fun platformKey(resId: ResourceId) = "${resId.value}.$PROP_PLATFORMS"
    }
}
