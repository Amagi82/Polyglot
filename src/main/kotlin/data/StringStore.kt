package data

import project.*

class StringStore(projectName: String) : ResourceStore<Str>(projectName, ResourceType.STRINGS) {
    override fun resource(values: Map<String, String>): Str = Str(values.values.firstOrNull().orEmpty())

    override fun putResource(baseKey: String, res: Str) {
        put(baseKey, res.text)
    }
}
