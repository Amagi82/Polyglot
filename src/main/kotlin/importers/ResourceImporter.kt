package importers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import locales.LocaleIsoCode
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import project.*
import ui.resource.ResourceViewModel
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

suspend fun importResources(
    vm: ResourceViewModel,
    importFiles: (
        strings: MutableMap<LocaleIsoCode, Map<ResourceId, Str>>,
        stringMetadata: MutableMap<ResourceId, Metadata>,
        plurals: MutableMap<LocaleIsoCode, Map<ResourceId, Plural>>,
        pluralMetadata: MutableMap<ResourceId, Metadata>,
        arrays: MutableMap<LocaleIsoCode, Map<ResourceId, StringArray>>,
        arrayMetadata: MutableMap<ResourceId, Metadata>,
        arraySizes: MutableMap<ResourceId, Int>
    ) -> List<File>
): List<File> {
    val strings = vm.strings.resourcesByLocale.value.toMutableMap()
    val stringMetadata = vm.strings.resourceMetadata.value.toMutableMap()
    val plurals = vm.plurals.resourcesByLocale.value.toMutableMap()
    val pluralMetadata = vm.plurals.resourceMetadata.value.toMutableMap()
    val arrays = vm.arrays.resourcesByLocale.value.toMutableMap()
    val arrayMetadata = vm.arrays.resourceMetadata.value.toMutableMap()
    val arraySizes = vm.arrays.arraySizes.value.toMutableMap()

    val importedFiles = withContext(Dispatchers.IO) {
        importFiles(strings, stringMetadata, plurals, pluralMetadata, arrays, arrayMetadata, arraySizes)
    }

    vm.strings.resourcesByLocale.value = strings
    vm.strings.resourceMetadata.value = stringMetadata
    vm.plurals.resourcesByLocale.value = plurals
    vm.plurals.resourceMetadata.value = pluralMetadata
    vm.arrays.resourcesByLocale.value = arrays
    vm.arrays.resourceMetadata.value = arrayMetadata
    vm.arrays.arraySizes.value = arraySizes

    return importedFiles
}

fun <R : Resource> Map<ResourceId, R>.mergeWith(
    platform: Platform,
    locale: LocaleIsoCode,
    overwrite: Boolean,
    metadata: MutableMap<ResourceId, Metadata>,
    resources: MutableMap<LocaleIsoCode, Map<ResourceId, R>>,
    arraySizes: MutableMap<ResourceId, Int>? = null,
) {
    resources.merge(locale, this) { old, new -> if (overwrite) old + new else new + old }
    forEach { (resId, resource) ->
        metadata.compute(resId) { _, metadata ->
            when {
                metadata == null -> Metadata(type = resource::class.type, platforms = listOf(platform))
                metadata.platforms.contains(platform) -> metadata
                else -> metadata.copy(platforms = metadata.platforms.plus(platform).sorted())
            }
        }
        arraySizes?.merge(resId, (resource as StringArray).items.size) { old, new -> if (overwrite) new else old }
    }
}

fun File.parseDocument(): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
fun NodeList.toList() = List(length) { item(it) }.filterIsInstance<Element>()
