package data

import project.*

class PluralStore(projectName: String) : ResourceStore<Plural, PluralMetadata>(projectName, ResourceType.PLURALS) {
    override fun createMetadata(group: GroupId, platforms: List<Platform>, arraySize: Int?) = PluralMetadata(group = group, platforms = platforms)

    override fun createResource(values: Map<String, String>, arraySize: Int?): Plural =
        Plural(values.mapKeys { (k, _) -> Quantity.valueOf(k.uppercase()) })

    override fun putResource(baseKey: String, res: Plural) {
        Quantity.values().forEach { quantity ->
            val text = res.items[quantity]
            val key = "$baseKey.${quantity.label}"
            if (text == null) {
                remove(key)
            } else {
                put(key, text)
            }
        }
    }
}
