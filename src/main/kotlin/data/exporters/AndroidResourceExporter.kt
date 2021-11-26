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

private fun <R : Resource, M : Metadata<M>> addAll(
    data: ExportResourceData<R, M>,
    xmlByLocale: Map<LocaleIsoCode, Element>,
    add: Element.(R) -> Unit
) {
    data.metadataById.forEach { (resId, metadata) ->
        if (ANDROID !in metadata.platforms) return@forEach
        data.localizedResourcesById[resId]?.forEach { (locale, res) ->
            xmlByLocale[locale]!!.appendElement(data.type.androidRootElementTag) {
                setAttribute("name", resId.value.toSnakeCase())
                add(res)
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
