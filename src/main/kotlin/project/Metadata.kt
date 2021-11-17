package project

import androidx.compose.runtime.Immutable

@Immutable
sealed class Metadata<M : Metadata<M>>(val type: ResourceType) {
    abstract val group: GroupId
    abstract val platforms: List<Platform>

    abstract fun copyImpl(group: GroupId = this.group, platforms: List<Platform> = this.platforms): M

    companion object {
        const val PROP_GROUP = "group"
        const val PROP_PLATFORMS = "platforms"

        @Suppress("UNCHECKED_CAST")
        operator fun <M : Metadata<M>> invoke(type: ResourceType, group: GroupId = GroupId(), platforms: List<Platform> = Platform.ALL): M = when (type) {
            ResourceType.STRINGS -> StringMetadata(group = group, platforms = platforms)
            ResourceType.PLURALS -> PluralMetadata(group = group, platforms = platforms)
            ResourceType.ARRAYS -> ArrayMetadata(group = group, platforms = platforms, size = 0)
        } as M

        fun groupKey(resId: ResourceId) = "${resId.value}.$PROP_GROUP"
        fun platformKey(resId: ResourceId) = "${resId.value}.$PROP_PLATFORMS"
    }
}

@Immutable
data class StringMetadata(
    override val group: GroupId = GroupId(),
    override val platforms: List<Platform> = Platform.ALL
) : Metadata<StringMetadata>(ResourceType.STRINGS) {
    override fun copyImpl(group: GroupId, platforms: List<Platform>) = copy(group = group, platforms = platforms)
}

@Immutable
data class PluralMetadata(
    override val group: GroupId = GroupId(),
    override val platforms: List<Platform> = Platform.ALL
) : Metadata<PluralMetadata>(ResourceType.PLURALS) {
    override fun copyImpl(group: GroupId, platforms: List<Platform>) = copy(group = group, platforms = platforms)
}

@Immutable
data class ArrayMetadata(
    override val group: GroupId = GroupId(),
    override val platforms: List<Platform> = Platform.ALL,
    val size: Int = 0
) : Metadata<ArrayMetadata>(ResourceType.ARRAYS) {
    override fun copyImpl(group: GroupId, platforms: List<Platform>) = copy(group = group, platforms = platforms)

    companion object {
        const val PROP_SIZE = "size"
        fun sizeKey(resId: ResourceId) = "${resId.value}.$PROP_SIZE"
    }
}
