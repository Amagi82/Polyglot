package data

import project.*

class PluralStore(projectName: String) : ResourceStore<Plural>(projectName, ResourceType.PLURALS) {
    override fun resource(values: Map<String, String>): Plural = Plural(values.mapKeys { (k, _) -> Quantity.valueOf(k.uppercase()) })

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
