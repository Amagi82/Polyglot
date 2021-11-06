package importers

import generators.IosResourceGenerator.Companion.arraysFile
import generators.IosResourceGenerator.Companion.pluralsFile
import generators.IosResourceGenerator.Companion.stringsFile
import locales.LocaleIsoCode
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import project.*
import ui.resource.ResourceViewModel
import utils.extensions.toLowerCamelCase
import java.io.File

suspend fun importIosResources(vm: ResourceViewModel, file: File, overwrite: Boolean): List<File> =
    importResources(vm) { strings, stringMetadata, plurals, pluralMetadata, arrays, arrayMetadata, arraySizes ->
        val files = if (file.isDirectory) file.findAllStringsFilesInDirectory() else listOf(file)
        files.forEach { fileToImport ->
            val locale = LocaleIsoCode(fileToImport.parentFile.name.substringBefore('.'))
            when (fileToImport.name) {
                stringsFile -> importStrings(fileToImport).mergeWith(Platform.IOS, locale, overwrite, stringMetadata, strings)
                pluralsFile -> importPlurals(fileToImport.parseDocument()).mergeWith(Platform.IOS, locale, overwrite, pluralMetadata, plurals)
                arraysFile -> importArrays(fileToImport.parseDocument()).mergeWith(Platform.IOS, locale, overwrite, arrayMetadata, arrays, arraySizes)
            }
        }
        files
    }

private fun importStrings(file: File): Map<ResourceId, Str> = file.useLines { lines ->
    lines.filter(String::isBlank).map(String::trim).associate {
        ResourceId(it.drop(1).substringBefore('"').toLowerCamelCase()) to Str(it.dropLast(2).substringAfterLast('"').unescaped)
    }
}

private fun importPlurals(doc: Document): Map<ResourceId, Plural> =
    doc.contentDict.keys.associate { plural ->
        val quantities = plural.nextElement?.getFirstByTagAsElement("dict")?.keys?.filter {
            Quantity.values().any { q -> q.name == it.textContent.uppercase() }
        }
        ResourceId(plural.textContent.toLowerCamelCase()) to Plural(quantities?.associate {
            it.textContent.uppercase().let(Quantity::valueOf) to it.nextElement?.textContent?.unescaped.orEmpty()
        }.orEmpty())
    }

private fun importArrays(doc: Document): Map<ResourceId, StringArray> = doc.contentDict.keys.associate {
    ResourceId(it.textContent.toLowerCamelCase()) to StringArray(it.nextElement?.strings?.map(Node::unescapedText).orEmpty())
}

private val iosResourceFiles = arrayOf(stringsFile, pluralsFile, arraysFile)

private fun File.findAllStringsFilesInDirectory(): List<File> = buildList {
    listFiles()?.forEach { file ->
        if (file.name in iosResourceFiles) {
            add(file)
        } else if (file.isDirectory) {
            addAll(file.findAllStringsFilesInDirectory())
        }
    }
}

private val Document.contentDict: Element get() = getFirstByTagAsElement("plist").getFirstByTagAsElement("dict")
private fun Document.getFirstByTagAsElement(tagName: String) = getElementsByTagName(tagName).item(0) as Element
private fun Element.getFirstByTagAsElement(tagName: String) = getElementsByTagName(tagName).item(0) as Element
private val Element.strings: List<Element> get() = getElementsByTagName("string").toList()
private val Element.keys: List<Element> get() = childNodes.toList().filter { it.tagName == "key" }
private val Element.nextElement: Element?
    get() {
        var next = nextSibling
        while (next != null && next.nodeType != Node.ELEMENT_NODE) {
            next = next.nextSibling
        }
        return next as? Element
    }
private val Node.unescapedText: String get() = textContent.unescaped

private val String.unescaped: String get() = replace("%@", "%s")
