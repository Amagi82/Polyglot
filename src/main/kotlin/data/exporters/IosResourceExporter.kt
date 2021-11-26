package data.exporters

import locales.LocaleIsoCode
import org.w3c.dom.Element
import project.*
import project.Platform.IOS
import project.ResourceType.*
import utils.toLowerCamelCase
import java.io.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer

/**
 * Exports iOS resources for a given language in the specified folder
 * This creates separate files for String resources, Plurals, and Arrays, which each use different
 * file types and formatting. It also generates an R file for type-safe resource access, e.g.
 * R.string.some_resource_id or R.plural.some_plural_id(quantity: 3, arg0: 3, arg1: "Mice") or
 * R.array.some_array_id, similar to Android, but providing the actual resource, not an integer.
 * Swift extension functions are generated for convenience.
 */
fun exportIOSResources(data: ExportProjectData) {
    val formatters = StringFormatter.defaultFormatters.filter { IOS in it.platforms }
    val outputFolder = File(data.exportUrl)
    val outputFolders = data.locales.associateWith { locale ->
        File(outputFolder, "${locale.value}.lproj").also(File::mkdirs)
    }

    val stringWritersByLocale = outputFolders.mapValues { it.value.createChildFile(IOS.fileName(STRINGS)).bufferedWriter() }

    val pluralRootXmlElementsByLocale: Map<LocaleIsoCode, Element> = data.plurals.localizedResourcesById.flatMapTo(mutableSetOf()) { it.value.keys }
        .filter { it in data.locales }
        .associateWith { createDocumentWithPlistDictElement() }
    val arrayRootXmlElementsByLocale: Map<LocaleIsoCode, Element> = data.arrays.localizedResourcesById.flatMapTo(mutableSetOf()) { it.value.keys }
        .filter { it in data.locales }
        .associateWith { createDocumentWithPlistDictElement() }

    /**
     * "identifier" = "Localized text";
     */
    addAll(data.strings) { id, locale, res ->
        val txt = res.text.sanitized(formatters, isXml = false)
        stringWritersByLocale[locale]!!.appendLine("\"${id.value.toLowerCamelCase()}\" = \"$txt\";")
    }

    /**
     * <key>duration_days</key>
     * <dict>
     *   <key>NSStringLocalizedFormatKey</key>
     *   <string>%#@value@</string>
     *   <key>value</key>
     *   <dict>
     *       <key>NSStringFormatSpecTypeKey</key>
     *       <string>NSStringPluralRuleType</string>
     *       <key>NSStringFormatValueTypeKey</key>
     *       <string>d</string>
     *       <key>one</key>
     *       <string>%d day</string>
     *       <key>other</key>
     *       <string>%d days</string>
     *   </dict>
     * </dict>
     */
    addAll(data.plurals, pluralRootXmlElementsByLocale) { res ->
        appendElement("dict") {
            appendElement(KEY) { appendTextNode("NSStringLocalizedFormatKey") }
            appendElement(STRING) { appendTextNode("%#@value@") }
            appendElement(KEY) { appendTextNode("value") }
            appendElement("dict") {
                appendElement(KEY) { appendTextNode("NSStringFormatSpecTypeKey") }
                appendElement(STRING) { appendTextNode("NSStringPluralRuleType") }
                appendElement(KEY) { appendTextNode("NSStringFormatValueTypeKey") }
                appendElement(STRING) { appendTextNode("d") }
                res.items.forEach { (quantity, text) ->
                    appendElement(KEY) { appendTextNode(quantity.label) }
                    appendElement(STRING) { appendTextNode(text.sanitized(formatters)) }
                }
            }
        }
    }

    /**
     * <key>alert_cancel_reasons</key>
     * <array>
     *   <string>Time</string>
     *   <string>Wage</string>
     * </array>
     */
    addAll(data.arrays, arrayRootXmlElementsByLocale) { res ->
        appendElement("array") {
            for (text in res.items) {
                appendElement(STRING) { appendTextNode(text.sanitized(formatters)) }
            }
        }
    }

    val transformer: Transformer = createTransformer {
        setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Apple//DTD PLIST 1.0//en")
        setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.apple.com/DTDs/PropertyList-1.0.dtd")
    }

    stringWritersByLocale.forEach { (_, writer) -> writer.close() }
    pluralRootXmlElementsByLocale.forEach { (locale, xml) ->
        transformer.transform(xml.ownerDocument, outputFolders[locale]!!.createChildFile(IOS.fileName(PLURALS)))
    }
    arrayRootXmlElementsByLocale.forEach { (locale, xml) ->
        transformer.transform(xml.ownerDocument, outputFolders[locale]!!.createChildFile(IOS.fileName(ARRAYS)))
    }

    outputFolder.createChildFile("R.swift").bufferedWriter().use { writer ->
        writer.appendLine(generatedFileWarning)
        writer.appendLine("struct R {")
        writer.appendReferences(data.strings, data.defaultLocale, formatters)
        writer.appendReferences(data.plurals, data.defaultLocale, formatters)
        writer.appendReferences(data.arrays, data.defaultLocale, formatters)
        writer.appendLine('}')
    }
    outputFolder.createChildFile("String+Localization.swift").bufferedWriter().use {
        it.appendLine(generatedFileWarning)
        it.appendLine(
            """
                import Foundation
                
                extension String {
                
                    /*
                     * Returns a localized string. If the key is not found, it will default to using itself.
                     */
                    func localized() -> String {
                        NSLocalizedString(self, value: self, comment: "")
                    }
                    
                    /*
                     * Returns a localized string formatted with any args, using self as the key. It will default to
                     * using itself if no value is found for the key.
                     */
                    func localized(_ args: CVarArg...) -> String {
                        String(format: NSLocalizedString(self, value: self, comment: ""), arguments: args)
                    }
                    
                    func localizedPlural(quantity: Int, _ args: CVarArg...) -> String {
                        var args = args
                        args.insert(quantity, at: 0)
                        return String(format: NSLocalizedString(self, value: self, comment: ""), arguments: args)
                    }
                    
                    func localizedArray() -> [String] {
                        localizedDict?[self] as? [String] ?? []
                    }
                }
                
                private var localizedDict: NSDictionary? = {
                    if let path = Bundle.main.path(forResource: "LocalizableArrays", ofType: "plist") {
                        return NSDictionary(contentsOfFile: path)
                    }
                    return nil
                }()
                """.trimIndent()
        )
    }
}

private fun createDocumentWithPlistDictElement(): Element = createDocument().appendElement("plist").run {
    setAttribute("version", "1.0")
    appendElement("dict")
}

private const val KEY = "key"
private const val STRING = "string"

private fun <R : Resource, M : Metadata<M>> addAll(
    data: ExportResourceData<R, M>,
    add: (ResourceId, LocaleIsoCode, R) -> Unit
) {
    data.metadataById.forEach { (resId, metadata) ->
        if (IOS !in metadata.platforms) return@forEach
        data.localizedResourcesById[resId]?.forEach { (locale, resource) ->
            add(resId, locale, resource)
        }
    }
}

private fun <R : Resource, M : Metadata<M>> addAll(
    data: ExportResourceData<R, M>,
    xmlDocumentsByLocale: Map<LocaleIsoCode, Element>,
    add: Element.(R) -> Unit
) {
    addAll(data) { id, locale, resource ->
        xmlDocumentsByLocale[locale]!!.apply {
            appendElement(KEY) { appendTextNode(id.value.toLowerCamelCase()) }
            add(resource)
        }
    }
}

private val generatedFileWarning = """
        /*
        * This is a generated file, do not edit!
        * Generated by Polyglot
        */
        
    """.trimIndent()

private fun <R : Resource, M : Metadata<M>> Writer.appendReferences(
    data: ExportResourceData<R, M>,
    defaultLocale: LocaleIsoCode,
    formatters: List<StringFormatter>
) {
    if (data.metadataById.isEmpty()) return
    appendLine("\tstruct ${data.type.title.dropLast(1)} {")
    data.metadataById.forEach { (resId, _) ->
        val text = when (val res = data.localizedResourcesById[resId]!![defaultLocale]!!) {
            is Str -> res.text.sanitized(formatters, isXml = false)
            is Plural -> res[Quantity.OTHER]!!.sanitized(formatters, isXml = false)
            is StringArray -> res.items.joinToString { it.sanitized(formatters, isXml = false) }
            else -> ""
        }
        appendReferenceComment(text)
        appendReferenceFormattingArgs(resId, text, data.type)
    }
    appendLine("\t}")
}

private fun Writer.appendReferenceComment(exampleText: String) {
    appendLine()
    appendLine()
    if (exampleText.isNotEmpty()) appendLine("\t\t// en: $exampleText")
}

private fun Writer.appendReferenceFormattingArgs(id: ResourceId, exampleText: String, type: ResourceType) {
    var i = exampleText.indexOf('%')
    if (i == -1) {
        appendLine("\t\tstatic let ${id.value} = \"${id.value}\".localized()")
        return
    }

    append("\t\tstatic func $id(")
    if (type == PLURALS) {
        append("quantity: Int")
        if (i != -1) append(", ")
    }

    var ct = 0
    while (i != -1) {
        val argType = when (exampleText.getOrNull(i + 1)) {
            '@' -> "String"
            'd' -> "Int"
            'f' -> "Float"
            else -> null
        }
        if (argType != null) {
            if (ct != 0) append(", ")
            append("_ arg$ct: $argType")
            ct++
        }
        i = exampleText.indexOf('%', i + 1)
    }
    appendLine(") -> String {")
    append("\t\t\t\"$id\".localized")

    if (type == PLURALS) {
        append("Plural(quantity: quantity")
        if (ct > 0) append(", ")
    } else append('(')

    (0 until ct).forEach {
        if (it != 0) append(", ")
        append("arg$it")
    }
    appendLine(')')
    appendLine("\t\t}")
}
