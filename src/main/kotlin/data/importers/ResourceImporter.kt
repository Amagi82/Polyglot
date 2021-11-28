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
        strings: MutableResourceData<Str>,
        plurals: MutableResourceData<Plural>,
        arrays: MutableResourceData<StringArray>
    ) -> List<File>
): List<File> {
    val strings = MutableResourceData(vm.strings)
    val plurals = MutableResourceData(vm.plurals)
    val arrays = MutableResourceData(vm.arrays)

    val importedFiles = withContext(Dispatchers.IO) {
        importFiles(strings, plurals, arrays)
    }

    vm.strings.excludedResourcesByPlatform.value = strings.excludedResourcesByPlatform.toSortedMap()
    vm.strings.localizedResourcesById.value = strings.localizedResourcesById

    vm.plurals.excludedResourcesByPlatform.value = plurals.excludedResourcesByPlatform.toSortedMap()
    vm.plurals.localizedResourcesById.value = plurals.localizedResourcesById

    vm.arrays.excludedResourcesByPlatform.value = arrays.excludedResourcesByPlatform.toSortedMap()
    vm.arrays.localizedResourcesById.value = arrays.localizedResourcesById
    return importedFiles
}

fun <R : Resource> Map<ResourceId, R>.mergeWith(
    platform: Platform,
    locale: LocaleIsoCode,
    overwrite: Boolean,
    data: MutableResourceData<R>,
) {
    forEach { (resId, resource) ->
        data.excludedResourcesByPlatform.apply {
            if (data.localizedResourcesById.contains(resId)) {
                compute(platform) { _, excluded -> excluded?.minus(resId) }
            } else {
                Platform.values().forEach { if (it != platform) compute(it) { _, excluded -> excluded.orEmpty().plus(resId) } }
            }
        }

        val localeMap = data.localizedResourcesById.getOrElse(resId) { mapOf() }
        if (overwrite || !localeMap.contains(locale)) data.localizedResourcesById[resId] = localeMap.plus(locale to resource)
    }
}

data class MutableResourceData<R : Resource>(
    val excludedResourcesByPlatform: MutableMap<Platform, Set<ResourceId>>,
    val localizedResourcesById: MutableMap<ResourceId, Map<LocaleIsoCode, R>>
) {
    constructor(vm: ResourceTypeViewModel<R>) : this(
        excludedResourcesByPlatform = vm.excludedResourcesByPlatform.value.toMutableMap(),
        localizedResourcesById = vm.localizedResourcesById.value.toMutableMap()
    )
}

fun File.parseDocument(): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
fun NodeList.toList() = List(length) { item(it) }.filterIsInstance<Element>()
