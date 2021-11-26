package data

import kotlinx.coroutines.flow.update
import project.*

class ArrayStore(projectName: String) : ResourceStore<StringArray, ArrayMetadata>(projectName, ResourceType.ARRAYS) {
    override fun createMetadata(group: GroupId, platforms: List<Platform>, arraySize: Int?) = ArrayMetadata(
        group = group,
        platforms = platforms,
        size = arraySize ?: 0
    )

    override fun createResource(values: Map<String, String>, arraySize: Int?): StringArray =
        StringArray(List(arraySize ?: values.keys.maxOrNull()?.toInt() ?: 0) { i -> values["$i"].orEmpty() })

    override fun putResource(baseKey: String, res: StringArray) {
        res.items.forEachIndexed { i, text -> put("$baseKey.$i", text) }
    }

    fun updateArraySize(resId: ResourceId, size: Int) {
        val oldSize = metadataById.value[resId]?.size ?: 0
        metadataById.update { it.plus(resId to (it[resId]?.copy(size = size) ?: ArrayMetadata(size = size))).toSortedMap() }
        localizedResourcesById.update {
            it.plus(resId to it[resId]?.mapValues { (_, array) -> StringArray(List(size) { i -> array.items.getOrElse(i) { "" } }) }.orEmpty())
        }

        put(ArrayMetadata.sizeKey(resId), "$size")

        if (oldSize > size) {
            for (key in keys) {
                if (key.substringBefore('.') == resId.value && key.last().isDigit() && key.substringAfterLast('.').toInt() > size) {
                    remove(key)
                }
            }
        }
        save()
    }
}
