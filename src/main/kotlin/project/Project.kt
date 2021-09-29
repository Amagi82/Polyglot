package project

import androidx.compose.runtime.Stable
import utils.Settings
import java.io.File

//@Stable
//data class Project(
//    val name: String,
//    val resources: Map<String, Resource>,
//) {
//    private val sourceFile get() = File(saveFolder, "$name.json").apply(File::createNewFile)
//    private val tempFile get() = File(tempFolder, "$name.json").apply(File::createNewFile)
//
//    suspend fun cache() = save(file = tempFile)
//    suspend fun save() = save(file = sourceFile)
//
//    private suspend fun save(file: File) {
////        withContext(Dispatchers.IO) {
////            file.bufferedWriter().use {
////                runCatching { it.write(json.encodeToString(this@Project)) }
////                    .onFailure { println("Failed to load $name.json with $it") }
////            }
////        }
//    }
//
//    companion object {
//        private val saveFolder = File("polyglot/saved").apply(File::mkdirs)
//        private val tempFolder = File("polyglot/tmp").apply(File::mkdirs)
//
//        val all = buildList<Project> {
////            tempFolder.listFiles()?.forEach { file ->
////                add(json.decodeFromStream(file.inputStream()))
////            }
////            saveFolder.listFiles()?.forEach { file ->
////                if (none { it.name == file.nameWithoutExtension }) {
////                    add(json.decodeFromStream(file.inputStream()))
////                }
////            }
//            sortBy { it.name }
//        }
//
//        val current: Project? get() = Settings.currentProject?.let { current -> all.find { it.name == current } }
//    }
//}
