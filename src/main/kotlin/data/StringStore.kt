package data

import locales.LocaleIsoCode
import project.*

class StringStore(projectName: String) : ResourceStore<Str, StringMetadata>(projectName, ResourceType.STRINGS) {
    override fun createMetadata(group: GroupId, platforms: List<Platform>, arraySize: Int?) = StringMetadata(group = group, platforms = platforms)

    override fun MutableMap<LocaleIsoCode, Str>.putResource(resId: ResourceId, key: String, value: String) {
        put(LocaleIsoCode(key), Str(value))
    }

    override fun putResource(baseKey: String, res: Str) {
        put(baseKey, res.text)
    }
}
