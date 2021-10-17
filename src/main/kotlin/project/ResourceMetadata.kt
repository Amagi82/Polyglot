package project

import java.util.*


typealias ResourceMetadata = Map<ResourceId, ResourceInfo>

@JvmName("saveResources")
fun ResourceMetadata.save(projectName: String) {
    val file = Project.resourceMetadataFile(projectName)
    val props = Properties()
    forEach { (id, resource) ->
        props.setProperty(id.id, "${resource.type.name}|${resource.platforms.joinToString(separator = ",") { it.name }}|${resource.group}")
    }
    runCatching { props.store(file.outputStream(), "") }.onFailure {
        println("Failed to save resources with $it")
    }
}
