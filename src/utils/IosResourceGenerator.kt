/*
 * Copyright (C) 2018 Jim Pekarek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import models.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.lang.StringBuilder
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer

/*
 * Generates iOS resources for a given language in the specified folder
 * This creates separate files for String resources, Plurals, and Arrays, which each use different
 * file types and formatting. It also generates an R file for type-safe resource access, e.g.
 * R.string.some_resource_id or R.plural.some_plural_id(quantity: 3, arg0: 3, arg1: "Mice") or
 * R.array.some_array_id, similar to Android, but providing the actual resource, not an integer.
 * Swift extension functions are generated for convenience.
 */
class IosResourceGenerator(private val iosFolder: File, private val language: Language, formatters: List<StringFormatter>) :
        ResourceGenerator(Platform.IOS, formatters.filter { it.platform != Platform.ANDROID }) {
    private val subFolder = File(iosFolder, "${language.isoCode}.lproj").also { it.mkdirs() }

    private val stringsWriter = BufferedWriter(FileWriter(subFolder.createChildFile("Localizable.strings")))

    private val pluralsDocument: Document = createDocument()
    private val pluralsResourceElement: Element = pluralsDocument.createAndAppendPlistElement()

    private val arraysDocument: Document = createDocument()
    private val arraysResourceElement: Element = arraysDocument.createAndAppendPlistElement()

    private val shouldCreateReferences = language is English && iosFolder.listFiles().none { it.name == "R.swift" }
    private val stringReferences = if (shouldCreateReferences) StringBuilder() else null
    private val pluralReferences = if (shouldCreateReferences) StringBuilder() else null
    private val stringArrayReferences = if (shouldCreateReferences) StringBuilder() else null

    override fun add(res: Resource) {
        if (res.platform == Platform.ANDROID) return
        when (res.localizationType) {
            is Str -> addString(res.id, res.localizationType)
            is Plural -> addPlurals(res.id, res.localizationType)
            is StringArray -> addStringArray(res.id, res.localizationType)
        }
    }

    override fun addAll(resources: Collection<Resource>) {
        resources.forEach(::add)
    }

    override fun generateFiles() {
        stringsWriter.close()
        transformer.transform(pluralsDocument, subFolder, "Localizable.stringsdict")
        transformer.transform(arraysDocument, subFolder, "LocalizableArrays.plist")
        generateReferences()
        generateStringLocalizationExtensions()
    }

    /*
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
    private fun addPlurals(id: String, plural: Plural) {
        var exampleText: String? = null
        pluralsResourceElement.appendChild(pluralsDocument, KEY, id)
        pluralsResourceElement.appendChild(pluralsDocument.createElement("dict").apply {
            appendChild(pluralsDocument, KEY, "NSStringLocalizedFormatKey")
            appendChild(pluralsDocument, STRING, "%#@value@")
            appendChild(pluralsDocument, KEY, "value")
            appendChild(pluralsDocument.createElement("dict").apply {
                appendChild(pluralsDocument, KEY, "NSStringFormatSpecTypeKey")
                appendChild(pluralsDocument, STRING, "NSStringPluralRuleType")
                appendChild(pluralsDocument, KEY, "NSStringFormatValueTypeKey")
                appendChild(pluralsDocument, STRING, "d")
                Quantities.values().forEach { quantity ->
                    val item = plural.quantity(quantity) ?: return@forEach
                    val txt = item.fromLocale(language).sanitized(isXml = true)
                    if (exampleText == null) exampleText = txt
                    appendChild(pluralsDocument, KEY, quantity.label)
                    appendChild(pluralsDocument, STRING, txt)
                }
            })
        })
        addReference(id, exampleText ?: "", plural)
    }

    private fun addReference(id: String, exampleText: String, localizationType: LocalizationType) {
        when (localizationType) {
            is Str -> stringReferences?.apply {
                appendln()
                appendReferenceComment(exampleText)
                if (exampleText.any { it == '%' }) appendReferenceFormattingArgs(id, exampleText, false)
                else appendln("\t\tstatic let $id = \"$id\".localized()")
            }
            is Plural -> pluralReferences?.apply {
                appendln()
                appendReferenceComment(exampleText)
                appendReferenceFormattingArgs(id, exampleText, true)
            }
            is StringArray -> stringArrayReferences?.apply {
                appendln()
                appendReferenceComment(exampleText)
                appendln("\t\tstatic let $id = \"$id\".localizedArray()")
            }
        }
    }

    private fun StringBuilder.appendReferenceComment(exampleText: String) {
        if (exampleText.isNotEmpty()) appendln("\t\t// en: $exampleText")
    }

    private fun StringBuilder.appendReferenceFormattingArgs(id: String, exampleText: String, isPlural: Boolean) {
        var i = exampleText.indexOf('%')

        append("\t\tstatic func $id(")
        if (isPlural) {
            append("quantity: Int")
            if (i != -1) append(", ")
        }

        var ct = 0
        while (i != -1) {
            val type = when (exampleText.getOrNull(i + 1)) {
                '@' -> "String"
                'd' -> "Int"
                'f' -> "Float"
                else -> null
            }
            if (type != null) {
                if (ct != 0) append(", ")
                append("_ arg$ct: $type")
                ct++
            }
            i = exampleText.indexOf('%', i + 1)
        }
        appendln(") -> String {")
        append("\t\t\treturn \"$id\".localized")

        if (isPlural) {
            append("Plural(quantity")
            if (ct > 0) append(", ")
        } else append('(')

        (0 until ct).forEach {
            if (it != 0) append(", ")
            append("arg$it")
        }
        appendln(')')
    }

    private fun addString(id: String, str: Str) {
        // "identifier" = "Localized text";
        val txt = str.fromLocale(language).sanitized(isXml = false)
        stringsWriter.appendln("\"$id\" = \"$txt\";")
        addReference(id, txt, str)
    }

    /*
     * <key>alert_cancel_reasons</key>
     * <array>
     *   <string>Time</string>
     *   <string>Wage</string>
     * </array>
     */
    private fun addStringArray(id: String, stringArray: StringArray) {
        arraysResourceElement.appendChild(arraysDocument, KEY, id)
        arraysResourceElement.appendChild(arraysDocument.createElement("array").apply {
            for (item in stringArray.items) {
                appendChild(arraysDocument, STRING, item.fromLocale(language).sanitized(isXml = true))
            }
        })
        addReference(id, "", stringArray)
    }

    private fun generateReferences() {
        if (!shouldCreateReferences) return
        BufferedWriter(FileWriter(iosFolder.createChildFile("R.swift"))).apply {
            appendln("/*")
            appendln(" * This is a generated file, do not edit!")
            appendln(" * Generated by Polyglot")
            appendln(" */")
            appendln()
            appendln("struct R{")
            appendln("\tstruct string{")
            if (stringReferences != null) append(stringReferences)
            appendln("\t}")
            appendln("\tstruct plural{")
            if (pluralReferences != null) append(pluralReferences)
            appendln("\t}")
            appendln("\tstruct array{")
            if (stringArrayReferences != null) append(stringArrayReferences)
            appendln("\t}")
        }.close()
    }

    private fun generateStringLocalizationExtensions() {
        if (iosFolder.listFiles().any { it.name == "String+Localization.swift" }) return
        BufferedWriter(FileWriter(iosFolder.createChildFile("String+Localization.swift"))).apply {
            appendln("/*")
            appendln(" * This is a generated file, do not edit!")
            appendln(" * Generated by Polyglot")
            appendln(" */")
            appendln()
            appendln("import Foundation")
            appendln()
            appendln("extension String {")
            appendln()
            appendln("\t/*")
            appendln("\t * Returns a localized string. If the key is not found, it will default to using itself.")
            appendln("\t */")
            appendln("\tfunc localized() -> String {")
            appendln("\t\treturn NSLocalizedString(self, value: self, comment: \"\")")
            appendln("\t}")
            appendln()
            appendln("\t/*")
            appendln("\t * Returns a localized string formatted with any args, using self as the key. It will default to")
            appendln("\t * using itself if no value is found for the key.")
            appendln("\t */")
            appendln("\tfunc localized(_ args: CVarArg...) -> String {")
            appendln("\t\treturn String(format: NSLocalizedString(self, value: self, comment: \"\"), arguments: args)")
            appendln("\t}")
            appendln()
            appendln("\tfunc localizedPlural(quantity: Int, _ args: CVarArg...) -> String {")
            appendln("\t\tvar args = args")
            appendln("\t\targs.insert(quantity, at: 0)")
            appendln("\t\treturn String(format: NSLocalizedString(self, value: self, comment: \"\"), arguments: args)")
            appendln("\t}")
            appendln()
            appendln("\tfunc localizedArray() -> [String] {")
            appendln("\t\tif let array = localizedDict?[self] as? [String] {")
            appendln("\t\t\treturn array")
            appendln("\t\t}")
            appendln("\t\treturn [String]()")
            appendln("\t}")
            appendln("}")
            appendln()
            appendln("private var localizedDict: NSDictionary? = {")
            appendln("\tif let path = Bundle.main.path(forResource: \"LocalizableArrays\", ofType: \"plist\") {")
            appendln("\t\treturn NSDictionary(contentsOfFile: path)")
            appendln("\t}")
            appendln("\treturn nil")
            appendln("}()")
        }.close()
    }

    private fun Document.createAndAppendPlistElement(): Element {
        val plist = createElement("plist").apply { setAttribute("version", "1.0") }
        val dict = createElement("dict")
        appendChild(plist)
        plist.appendChild(dict)
        return dict
    }

    companion object {
        private const val KEY = "key"
        private const val STRING = "string"

        private val transformer: Transformer by lazy {
            createTransformer().apply {
                setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Apple//DTD PLIST 1.0//en")
                setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.apple.com/DTDs/PropertyList-1.0.dtd")
            }
        }
    }
}