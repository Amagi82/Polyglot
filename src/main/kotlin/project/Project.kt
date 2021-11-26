package project

import androidx.compose.runtime.Immutable
import java.io.File

/**
 * @param name: Name of the project
 */
@Immutable
@JvmInline
value class Project(val name: String) {
    companion object {
        val projectsFolder = File("projects").apply(File::mkdirs)
        fun projectFolder(projectName: String) = File(projectsFolder, projectName).apply(File::mkdirs)
    }
}
