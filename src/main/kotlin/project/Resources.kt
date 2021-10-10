package project

import java.util.*


typealias Resources = SortedMap<ResourceId, Resource>

@JvmName("saveResources")
fun Resources.save(projectName: String) {
    val file = Project.resourcesFile(projectName)
    if (isEmpty()) {
        file.delete()
        return
    }
    val props = Properties()
    forEach { (id, resource) ->
        props.setProperty(id.id, "${resource.type.name}|${resource.platforms.joinToString(separator = ",") { it.name }}|${resource.group}")
    }
    runCatching { props.store(file.outputStream(), "") }.onFailure {
        println("Failed to save resources with $it")
    }
}
