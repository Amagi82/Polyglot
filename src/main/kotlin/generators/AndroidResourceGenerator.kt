package generators

import locales.LocaleIsoCode
import org.w3c.dom.Element
import project.*
import project.Platform.ANDROID
import ui.resource.ResourceTypeViewModel
import ui.resource.ResourceViewModel
import utils.toSnakeCase
import java.io.File

/**
 * Generates Android resources for a given language in the specified folder
 */
fun generateAndroidResources(vm: ResourceViewModel) {
    val formatters = StringFormatter.defaultFormatters.filter { ANDROID in it.platforms }

    val xmlByLocale: Map<LocaleIsoCode, Element> = vm.locales.value.associateWith {
        createDocument().appendElement("resources")
    }

    /**
     * <string name="dragon">Trogdor the Burninator</string>
     * */
    addAll(vm.strings, xmlByLocale) { res ->
        appendTextNode(res.text.sanitized(formatters))
    }

    /**
     * <plurals name="numberOfSongsAvailable">
     *     <item quantity="one">Znaleziono %d piosenkę.</item>
     *     <item quantity="few">Znaleziono %d piosenki.</item>
     *     <item quantity="other">Znaleziono %d piosenek.</item>
     * </plurals>
     */
    addAll(vm.plurals, xmlByLocale) { res ->
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
    addAll(vm.arrays, xmlByLocale) { res ->
        for (text in res.items) {
            appendElement("item") { appendTextNode(text.sanitized(formatters)) }
        }
    }

    val transformer = createTransformer()
    val exportUrl = vm.exportUrls.value[ANDROID] ?: ANDROID.defaultOutputUrl
    val defaultLocale = vm.defaultLocale.value
    xmlByLocale.forEach { (locale, xml) ->
        val valuesFolder = File(exportUrl, "values${if (locale == defaultLocale) "" else "-${locale.value}"}").also(File::mkdirs)
        transformer.transform(xml.ownerDocument, valuesFolder.createChildFile(ANDROID.fileName(ResourceType.STRINGS)))
    }
}

private fun <R : Resource, M : Metadata<M>> addAll(
    vm: ResourceTypeViewModel<R, M>,
    xmlByLocale: Map<LocaleIsoCode, Element>,
    add: Element.(R) -> Unit
) {
    vm.metadataById.value.forEach { (resId, metadata) ->
        if (ANDROID !in metadata.platforms) return@forEach
        vm.localizedResourcesById.value[resId]?.forEach { (locale, res) ->
            xmlByLocale[locale]!!.appendElement(vm.type.androidRootElementTag) {
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
