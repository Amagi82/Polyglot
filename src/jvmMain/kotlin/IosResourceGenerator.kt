import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.lang.StringBuilder
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer

/**
 * Generates iOS resources for a given language in the specified folder
 * This creates separate files for String resources, Plurals, and Arrays, which each use different
 * file types and formatting. It also generates an R file for type-safe resource access, e.g.
 * R.string.some_resource_id or R.plural.some_plural_id(quantity: 3, arg0: 3, arg1: "Mice") or
 * R.array.some_array_id, similar to Android, but providing the actual resource, not an integer.
 * Swift extension functions are generated for convenience.
 */
class IosResourceGenerator(private val iosFolder: File, locale: LocaleIsoCode, formatters: List<StringFormatter>, resources: Collection<Resource>) :
    ResourceGenerator(locale, formatters) {
    override val platform: Platform get() = Platform.iOS
    private val localizationFolder = File(iosFolder, "$locale.lproj").also(File::mkdirs)

    private val stringsWriter = BufferedWriter(FileWriter(localizationFolder.createChildFile("Localizable.strings")))

    private val pluralsDocument: Document = createDocument()
    private val pluralsResourceElement: Element = pluralsDocument.createAndAppendPlistElement()

    private val arraysDocument: Document = createDocument()
    private val arraysResourceElement: Element = arraysDocument.createAndAppendPlistElement()

    private val shouldCreateReferences = locale.isDefault
    private val stringReferences = if (shouldCreateReferences) StringBuilder() else null
    private val pluralReferences = if (shouldCreateReferences) StringBuilder() else null
    private val stringArrayReferences = if (shouldCreateReferences) StringBuilder() else null

    init {
        if (shouldCreateReferences) {
            iosFolder.listFiles { file -> file.name == "R.swift" }?.forEach(File::delete)
        }
        addAll(resources)
    }

    override fun add(res: Resource) {
        when (res) {
            is Str -> addString(res)
            is Plural -> addPlurals(res)
            is StringArray -> addStringArray(res)
        }
    }

    override fun generateFiles() {
        stringsWriter.close()
        transformer.transform(pluralsDocument, localizationFolder, "Localizable.stringsdict")
        transformer.transform(arraysDocument, localizationFolder, "LocalizableArrays.plist")
        generateReferences()
        generateStringLocalizationExtensions()
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
    private fun addPlurals(res: Plural) {
        var exampleText: String? = null
        pluralsResourceElement.appendChild(pluralsDocument, KEY, res.id)
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
                    val item = res.quantity(quantity) ?: return@forEach
                    val txt = (item.get(locale, isRequired = quantity.isRequired) ?: return@forEach).sanitized(isXml = true)
                    if (exampleText == null) exampleText = txt
                    appendChild(pluralsDocument, KEY, quantity.label)
                    appendChild(pluralsDocument, STRING, txt)
                }
            })
        })
        addReference(res, exampleText.orEmpty())
    }

    private fun addReference(res: Resource, exampleText: String) {
        when (res) {
            is Str -> stringReferences?.apply {
                appendLine()
                appendReferenceComment(exampleText)
                if (exampleText.contains('%')) appendReferenceFormattingArgs(res.id, exampleText, false)
                else appendLine("\t\tstatic let ${res.id} = \"${res.id}\".localized()")
            }
            is Plural -> pluralReferences?.apply {
                appendLine()
                appendReferenceComment(exampleText)
                appendReferenceFormattingArgs(res.id, exampleText, true)
            }
            is StringArray -> stringArrayReferences?.apply {
                appendLine()
                appendReferenceComment(exampleText)
                appendLine("\t\tstatic let ${res.id} = \"${res.id}\".localizedArray()")
            }
        }
    }

    private fun StringBuilder.appendReferenceComment(exampleText: String) {
        if (exampleText.isNotEmpty()) appendLine("\t\t// en: $exampleText")
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
        appendLine(") -> String {")
        append("\t\t\treturn \"$id\".localized")

        if (isPlural) {
            append("Plural(quantity")
            if (ct > 0) append(", ")
        } else append('(')

        (0 until ct).forEach {
            if (it != 0) append(", ")
            append("arg$it")
        }
        appendLine(')')
        appendLine("\t\t}")
    }

    private fun addString(res: Str) {
        // "identifier" = "Localized text";
        val txt = res[locale].sanitized(isXml = false)
        stringsWriter.appendLine("\"${res.id}\" = \"$txt\";")
        addReference(res, txt)
    }

    /**
     * <key>alert_cancel_reasons</key>
     * <array>
     *   <string>Time</string>
     *   <string>Wage</string>
     * </array>
     */
    private fun addStringArray(res: StringArray) {
        arraysResourceElement.appendChild(arraysDocument, KEY, res.id)
        arraysResourceElement.appendChild(arraysDocument.createElement("array").apply {
            for (item in res.items) {
                appendChild(arraysDocument, STRING, item.getRequired(locale).sanitized(isXml = true))
            }
        })
        addReference(res, "")
    }

    private fun generateReferences() {
        if (!shouldCreateReferences) return
        iosFolder.createChildFile("R.swift").bufferedWriter().use {
            it.appendLine(GENERATED_FILE_HEADER)
            it.appendLine()
            it.appendLine("struct R{")
            it.appendLine("\tstruct string{")
            if (stringReferences != null) it.append(stringReferences)
            it.appendLine("\t}")
            it.appendLine("\tstruct plural{")
            if (pluralReferences != null) it.append(pluralReferences)
            it.appendLine("\t}")
            it.appendLine("\tstruct array{")
            if (stringArrayReferences != null) it.append(stringArrayReferences)
            it.appendLine("\t}")
            it.appendLine("}")
        }
    }

    private fun generateStringLocalizationExtensions() {
        if (iosFolder.listFiles()?.any { it.name == "String+Localization.swift" } == true) return
        iosFolder.createChildFile("String+Localization.swift").writeText(STRING_LOCALIZATION_EXTENSIONS)
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

        private const val GENERATED_FILE_HEADER = """
/*
 * This is a generated file, do not edit!
 * Generated by Polyglot
 */"""

        private const val STRING_LOCALIZATION_EXTENSIONS = """$GENERATED_FILE_HEADER
import Foundation

extension String {

    /*
     * Returns a localized string. If the key is not found, it will default to using itself.
     */
    func localized() -> String {
        return NSLocalizedString(self, value: self, comment: "")
    }

    /*
     * Returns a localized string formatted with any args, using self as the key. It will default to
     * using itself if no value is found for the key.
     */
    func localized(_ args: CVarArg...) -> String {
        return String(format: NSLocalizedString(self, value: self, comment: ""), arguments: args)
    }

    func localizedPlural(quantity: Int, _ args: CVarArg...) -> String {
        var args = args
        args.insert(quantity, at: 0)
        return String(format: NSLocalizedString(self, value: self, comment: ""), arguments: args)
    }

    func localizedArray() -> [String] {
        if let array = localizedDict?[self] as? [String] {
            return array
        }
        return [String]()
    }
}

private var localizedDict: NSDictionary? = {
    if let path = Bundle.main.path(forResource: "LocalizableArrays", ofType: "plist") {
        return NSDictionary(contentsOfFile: path)
    }
    return nil
}()"""
    }
}
