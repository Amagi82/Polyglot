package importers

import locales.LocaleIsoCode
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import project.*
import project.Platform.IOS
import project.ResourceType.*
import ui.resource.ResourceViewModel
import utils.toLowerCamelCase
import java.io.File

suspend fun importIosResources(vm: ResourceViewModel, file: File, overwrite: Boolean): List<File> =
    importResources(vm) { strings, plurals, arrays ->
        val files = if (file.isDirectory) file.findAllStringsFilesInDirectory() else listOf(file)
        files.forEach { fileToImport ->
            val locale = LocaleIsoCode(fileToImport.parentFile.name.substringBefore('.'))
            when (fileToImport.name) {
                IOS.fileName(STRINGS) -> importStrings(fileToImport).mergeWith(IOS, locale, overwrite, strings)
                IOS.fileName(PLURALS) -> importPlurals(fileToImport.parseDocument()).mergeWith(IOS, locale, overwrite, plurals)
                IOS.fileName(ARRAYS) -> importArrays(fileToImport.parseDocument()).mergeWith(IOS, locale, overwrite, arrays)
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

private val iosResourceFiles = ResourceType.values().map(IOS::fileName)

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
