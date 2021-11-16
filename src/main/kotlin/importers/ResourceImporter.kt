package importers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import locales.LocaleIsoCode
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import project.*
import ui.resource.ResourceTypeViewModel
import ui.resource.ResourceViewModel
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

suspend fun importResources(
    vm: ResourceViewModel,
    importFiles: (
        strings: MutableResourceData<Str>,
        plurals: MutableResourceData<Plural>,
        arrays: MutableResourceData<StringArray>,
        arraySizes: MutableMap<ResourceId, Int>
    ) -> List<File>
): List<File> {
    val strings = MutableResourceData(vm.strings)
    val plurals = MutableResourceData(vm.plurals)
    val arrays = MutableResourceData(vm.arrays)
    val arraySizes = vm.arrays.arraySizes.value.toMutableMap()

    val importedFiles = withContext(Dispatchers.IO) {
        importFiles(strings, plurals, arrays, arraySizes)
    }

    vm.strings.localizedResourcesById.value = strings.localizedResourcesById
    vm.strings.metadataById.value = strings.metadataById.toSortedMap()
    vm.plurals.localizedResourcesById.value = plurals.localizedResourcesById
    vm.plurals.metadataById.value = plurals.metadataById.toSortedMap()
    vm.arrays.localizedResourcesById.value = arrays.localizedResourcesById
    vm.arrays.metadataById.value = arrays.metadataById.toSortedMap()
    vm.arrays.arraySizes.value = arraySizes

    return importedFiles
}

fun <R : Resource> Map<ResourceId, R>.mergeWith(
    platform: Platform,
    locale: LocaleIsoCode,
    overwrite: Boolean,
    data: MutableResourceData<R>,
    arraySizes: MutableMap<ResourceId, Int>? = null,
) {
    forEach { (resId, resource) ->
        val localeMap = data.localizedResourcesById.getOrElse(resId) { mapOf() }
        if (overwrite || !localeMap.contains(locale)) data.localizedResourcesById[resId] = localeMap.plus(locale to resource)
        data.metadataById.compute(resId) { _, metadata ->
            when {
                metadata == null -> Metadata(type = resource::class.type, platforms = listOf(platform))
                metadata.platforms.contains(platform) -> metadata
                else -> metadata.copy(platforms = metadata.platforms.plus(platform).sorted())
            }
        }
        arraySizes?.merge(resId, (resource as StringArray).items.size) { old, new -> if (overwrite) new else old }
    }
}

data class MutableResourceData<R : Resource>(
    val metadataById: MutableMap<ResourceId, Metadata>,
    val localizedResourcesById: MutableMap<ResourceId, Map<LocaleIsoCode, R>>
) {
    constructor(vm: ResourceTypeViewModel<R>) : this(
        metadataById = vm.metadataById.value.toMutableMap(),
        localizedResourcesById = vm.localizedResourcesById.value.toMutableMap()
    )
}

fun File.parseDocument(): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
fun NodeList.toList() = List(length) { item(it) }.filterIsInstance<Element>()
