package data

import project.*

class StringStore(projectName: String) : ResourceStore<Str, StringMetadata>(projectName, ResourceType.STRINGS) {
    override fun createMetadata(group: GroupId, platforms: List<Platform>, arraySize: Int?) = StringMetadata(group = group, platforms = platforms)

    override fun createResource(values: Map<String, String>, arraySize: Int?): Str = Str(values.values.firstOrNull().orEmpty())

    override fun putResource(baseKey: String, res: Str) {
        put(baseKey, res.text)
    }
}
