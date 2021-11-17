package importers

import generators.androidRootElementTag
import locales.LocaleIsoCode
import org.w3c.dom.Document
import org.w3c.dom.Element
import project.*
import project.Platform.ANDROID
import ui.resource.ResourceViewModel
import utils.extensions.toLowerCamelCase
import java.io.File

suspend fun importAndroidResources(vm: ResourceViewModel, file: File, overwrite: Boolean): List<File> =
    importResources(vm) { strings, plurals, arrays ->
        val files = if (file.isDirectory) file.findAllStringsFilesInDirectory() else listOf(file)
        files.forEach { fileToImport ->
            val parentFolderName = fileToImport.parentFile.name
            val locale = LocaleIsoCode(if (parentFolderName.contains('-')) parentFolderName.substringAfter('-') else "en")
            val document = fileToImport.parseDocument()

            importStrings(document).mergeWith(ANDROID, locale, overwrite, strings)
            importPlurals(document).mergeWith(ANDROID, locale, overwrite, plurals)
            importArrays(document).mergeWith(ANDROID, locale, overwrite, arrays)
        }
        files
    }

private fun importStrings(doc: Document): Map<ResourceId, Str> =
    doc.elements(ResourceType.STRINGS).associate { it.resourceId to Str(it.unescapedText) }

private fun importPlurals(doc: Document): Map<ResourceId, Plural> =
    doc.elements(ResourceType.PLURALS).associate { plural -> plural.resourceId to Plural(plural.items.associate { it.quantity to it.unescapedText }) }

private fun importArrays(doc: Document): Map<ResourceId, StringArray> =
    doc.elements(ResourceType.ARRAYS).associate { it.resourceId to StringArray(it.items.map(Element::unescapedText)) }

private fun File.findAllStringsFilesInDirectory(): List<File> = buildList {
    val validFolders = arrayOf("app", "src", "main", "res")
    listFiles()?.forEach { file ->
        if (file.name == "strings.xml") {
            add(file)
        } else if (file.isDirectory && (file.name in validFolders || file.name.startsWith("values"))) {
            addAll(file.findAllStringsFilesInDirectory())
        }
    }
}

private fun Document.elements(type: ResourceType) = getElementsByTagName(type.androidRootElementTag).toList()
private val Element.resourceId: ResourceId get() = ResourceId(getAttribute("name").toLowerCamelCase())
private val Element.quantity: Quantity get() = getAttribute("quantity").uppercase().let(Quantity::valueOf)
private val Element.items: List<Element> get() = getElementsByTagName("item").toList()
private val Element.unescapedText: String get() = textContent.unescaped

private val regex = Regex("(?!%)(\\d\\$)(?=[sdf])")
private val String.unescaped: String get() = replace("\\'", "'").replace(regex, "")
