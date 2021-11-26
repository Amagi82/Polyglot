package data.importers

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
        strings: MutableResourceData<Str, StringMetadata>,
        plurals: MutableResourceData<Plural, PluralMetadata>,
        arrays: MutableResourceData<StringArray, ArrayMetadata>
    ) -> List<File>
): List<File> {
    val strings = MutableResourceData(vm.strings)
    val plurals = MutableResourceData(vm.plurals)
    val arrays = MutableResourceData(vm.arrays)

    val importedFiles = withContext(Dispatchers.IO) {
        importFiles(strings, plurals, arrays)
    }

    vm.strings.localizedResourcesById.value = strings.localizedResourcesById
    vm.strings.metadataById.value = strings.metadataById.toSortedMap()
    vm.plurals.localizedResourcesById.value = plurals.localizedResourcesById
    vm.plurals.metadataById.value = plurals.metadataById.toSortedMap()
    vm.arrays.localizedResourcesById.value = arrays.localizedResourcesById
    vm.arrays.metadataById.value = arrays.metadataById.toSortedMap()

    return importedFiles
}

fun <R : Resource, M : Metadata<M>> Map<ResourceId, R>.mergeWith(
    platform: Platform,
    locale: LocaleIsoCode,
    overwrite: Boolean,
    data: MutableResourceData<R, M>,
) {
    forEach { (resId, resource) ->
        val localeMap = data.localizedResourcesById.getOrElse(resId) { mapOf() }
        if (overwrite || !localeMap.contains(locale)) data.localizedResourcesById[resId] = localeMap.plus(locale to resource)
        data.metadataById.compute(resId) { _, metadata ->
            when {
                metadata == null -> Metadata(type = resource::class.type, platforms = listOf(platform))
                metadata.platforms.contains(platform) -> metadata
                else -> metadata.copyImpl(platforms = metadata.platforms.plus(platform).sorted())
            }
        }
    }
}

data class MutableResourceData<R : Resource, M : Metadata<M>>(
    val metadataById: MutableMap<ResourceId, M>,
    val localizedResourcesById: MutableMap<ResourceId, Map<LocaleIsoCode, R>>
) {
    constructor(vm: ResourceTypeViewModel<R, M>) : this(
        metadataById = vm.metadataById.value.toMutableMap(),
        localizedResourcesById = vm.localizedResourcesById.value.toMutableMap()
    )
}

fun File.parseDocument(): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
fun NodeList.toList() = List(length) { item(it) }.filterIsInstance<Element>()
