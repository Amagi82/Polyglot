package data

import kotlinx.coroutines.flow.update
import locales.LocaleIsoCode
import project.*

class ArrayStore(projectName: String) : ResourceStore<StringArray>(projectName, ResourceType.ARRAYS) {
    override fun resource(values: Map<String, String>): StringArray =
        StringArray(List((values.keys.maxOrNull()?.toInt() ?: 0) + 1) { i -> values["$i"].orEmpty() })

    override fun putResource(baseKey: String, res: StringArray) {
        res.items.forEachIndexed { i, text -> put("$baseKey.$i", text) }
    }

    fun updateArraySize(resId: ResourceId, defaultLocale: LocaleIsoCode, size: Int) {
        localizedResourcesById.update {
            it.plus(resId to it[resId]?.mapValues { (_, array) -> StringArray(List(size) { i -> array.items.getOrElse(i) { "" } }) }.orEmpty())
        }

        for (key in keys) {
            if (key.substringBefore('.') == resId.value && key.last().isDigit() && key.substringAfterLast('.').toInt() > size) {
                remove(key)
            }
        }
        for (i in 0 until size) {
            putIfAbsent("${resId.value}.${defaultLocale.value}.$i", "(empty)")
        }
        save()
    }
}
