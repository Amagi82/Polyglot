package data

import locales.LocaleIsoCode
import project.*

class PluralStore(projectName: String) : ResourceStore<Plural, PluralMetadata>(projectName, ResourceType.PLURALS) {
    override fun createMetadata(group: GroupId, platforms: List<Platform>, arraySize: Int?) = PluralMetadata(group = group, platforms = platforms)

    override fun MutableMap<LocaleIsoCode, Plural>.putResource(resId: ResourceId, key: String, value: String) {
        val locale = LocaleIsoCode(key.substringBefore('.'))
        val quantity = key.substringAfter('.').uppercase().let(Quantity::valueOf)
        put(locale, Plural(get(locale)?.items?.plus(quantity to value) ?: mapOf(quantity to value)))
    }

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
