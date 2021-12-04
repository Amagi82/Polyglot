package data.exporters

import locales.LocaleIsoCode
import org.w3c.dom.Element
import project.*
import project.Platform.ANDROID
import utils.toSnakeCase
import java.io.File

/**
 * Exports Android resources for a given language in the specified folder
 */
fun exportAndroidResources(data: ExportProjectData) {
    val formatters = StringFormatter.defaultFormatters.filter { ANDROID in it.platforms }

    val xmlByLocale: Map<LocaleIsoCode, Element> = data.locales.associateWith {
        createDocument().appendElement("resources")
    }

    /**
     * <string name="dragon">Trogdor the Burninator</string>
     * */
    addAll(data.strings, xmlByLocale) { res ->
        appendTextNode(res.text.sanitized(formatters))
    }

    /**
     * <plurals name="numberOfSongsAvailable">
     *     <item quantity="one">Znaleziono %d piosenkę.</item>
     *     <item quantity="few">Znaleziono %d piosenki.</item>
     *     <item quantity="other">Znaleziono %d piosenek.</item>
     * </plurals>
     */
    addAll(data.plurals, xmlByLocale) { res ->
        res.items.forEach { (quantity, text) ->
            appendElement("item") {
                setAttribute("quantity", quantity.label)
                appendTextNode(text.sanitized(formatters))
            }
        }
    }

    /**
     * <string-array name="country_names">
     *      <item>France</item>
     *      <item>Germany</item>
     * </string-array>
     */
    addAll(data.arrays, xmlByLocale) { res ->
        for (text in res.items) {
            appendElement("item") { appendTextNode(text.sanitized(formatters)) }
        }
    }

    val transformer = createTransformer()
    xmlByLocale.forEach { (locale, xml) ->
        val valuesFolder = File(data.exportUrl, "values${if (locale == data.defaultLocale) "" else "-${locale.value}"}").also(File::mkdirs)
        transformer.transform(xml.ownerDocument, valuesFolder.createChildFile(ANDROID.fileName(ResourceType.STRINGS)))
    }
}

private fun <R : Resource> addAll(
    data: ExportResourceData<R>,
    xmlByLocale: Map<LocaleIsoCode, Element>,
    add: Element.(R) -> Unit
) {
    xmlByLocale.values.forEach { xml ->
        xml.appendTextNode("\n")
    }

    for ((group, resIds) in data.resourceGroups) {
        val localesCommented = mutableMapOf<LocaleIsoCode, Boolean>()

        for (resId in resIds.sorted()) {
            if (resId in data.excludedResourcesByPlatform[ANDROID].orEmpty()) continue
            data.localizedResourcesById[resId]?.forEach { (locale, res) ->
                val xml = xmlByLocale[locale]!!

                if (group.name.isNotEmpty() && localesCommented[locale] != true) {
                    xml.appendTextNode("\n")
                    xml.appendComment(group.name)
                    localesCommented[locale] = true
                }

                xml.appendElement(data.type.androidRootElementTag) {
                    setAttribute("name", resId.value.toSnakeCase().let { if (it in restrictedKeywords) it + '_' else it })
                    add(res)
                }
            }
        }
    }
}

val ResourceType.androidRootElementTag: String
    get() = when (this) {
        ResourceType.STRINGS -> "string"
        ResourceType.PLURALS -> "plurals"
        ResourceType.ARRAYS -> "string-array"
    }

private val restrictedKeywords = arrayOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", "default", "do", "double",
    "else", "enum", "extends", "final", "finally", "float", "for", "if", "implements", "import", "instanceof", "int", "interface",
    "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
)
