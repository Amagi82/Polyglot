package generators

import locales.LocaleIsoCode
import org.w3c.dom.Element
import project.*
import project.Platform.ANDROID
import ui.resource.ResourceTypeViewModel
import ui.resource.ResourceViewModel
import utils.extensions.toSnakeCase
import java.io.File

/**
 * Generates Android resources for a given language in the specified folder
 */
fun generateAndroidResources(vm: ResourceViewModel) {
    val project = vm.project.value
    val formatters = StringFormatter.defaultFormatters.filter { ANDROID in it.platforms }

    val xmlByLocale: Map<LocaleIsoCode, Element> = project.locales.associateWith {
        createDocument().appendElement("resources")
    }

    /**
     * <string name="dragon">Trogdor the Burninator</string>
     * */

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
    val outputFolder = ANDROID.outputFolder(project)
    xmlByLocale.forEach { (locale, xml) ->
        val valuesFolder = File(outputFolder, "values${if (locale == project.defaultLocale) "" else "-${locale.value}"}").also(File::mkdirs)
        transformer.transform(xml.ownerDocument, valuesFolder.createChildFile(ANDROID.fileName(ResourceType.STRINGS)))
    }
}

private fun <R : Resource> addAll(
    vm: ResourceTypeViewModel<R>,
    xmlByLocale: Map<LocaleIsoCode, Element>,
    add: Element.(R) -> Unit
) {
    vm.resourceMetadata.value.forEach { (resId, metadata) ->
        if (ANDROID !in metadata.platforms) return@forEach
        vm.resourcesByLocale.value.forEach forEachLocale@{ (locale, resourceMap) ->
            val resource = resourceMap[resId] ?: return@forEachLocale
            xmlByLocale[locale]!!.appendElement(vm.type.androidRootElementTag) {
                setAttribute("name", resId.value.toSnakeCase())
                add(resource)
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
