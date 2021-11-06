package importers

import locales.LocaleIsoCode
import org.w3c.dom.Document
import org.w3c.dom.Element
import project.*
import ui.resource.ResourceViewModel
import utils.extensions.toLowerCamelCase
import java.io.File

suspend fun importAndroidResources(vm: ResourceViewModel, file: File, overwrite: Boolean): List<File> =
    importResources(vm) { strings, stringMetadata, plurals, pluralMetadata, arrays, arrayMetadata, arraySizes ->
        val files = if (file.isDirectory) file.findAllStringsFilesInDirectory() else listOf(file)
        files.forEach { fileToImport ->
            val parentFolderName = fileToImport.parentFile.name
            val locale = LocaleIsoCode(if (parentFolderName.contains('-')) parentFolderName.substringAfter('-') else "en")
            val document = fileToImport.parseDocument()

            importStrings(document).mergeWith(Platform.ANDROID, locale, overwrite, stringMetadata, strings)
            importPlurals(document).mergeWith(Platform.ANDROID, locale, overwrite, pluralMetadata, plurals)
            importArrays(document).mergeWith(Platform.ANDROID, locale, overwrite, arrayMetadata, arrays, arraySizes)
        }
        files
    }

private fun importStrings(doc: Document): Map<ResourceId, Str> =
    doc.strings.associate { it.resourceId to Str(it.unescapedText) }

private fun importPlurals(doc: Document): Map<ResourceId, Plural> =
    doc.plurals.associate { plural -> plural.resourceId to Plural(plural.items.associate { it.quantity to it.unescapedText }) }

private fun importArrays(doc: Document): Map<ResourceId, StringArray> =
    doc.arrays.associate { it.resourceId to StringArray(it.items.map(Element::unescapedText)) }

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

private val Document.strings: List<Element> get() = getElementsByTagName("string").toList()
private val Document.plurals: List<Element> get() = getElementsByTagName("plurals").toList()
private val Document.arrays: List<Element> get() = getElementsByTagName("string-array").toList()
private val Element.resourceId: ResourceId get() = ResourceId(getAttribute("name").toLowerCamelCase())
private val Element.quantity: Quantity get() = getAttribute("quantity").uppercase().let(Quantity::valueOf)
private val Element.items: List<Element> get() = getElementsByTagName("item").toList()
private val Element.unescapedText: String get() = textContent.unescaped

private val regex = Regex("(?!%)(\\d\\$)(?=[sdf])")
private val String.unescaped: String get() = replace("\\'", "'").replace(regex, "")
