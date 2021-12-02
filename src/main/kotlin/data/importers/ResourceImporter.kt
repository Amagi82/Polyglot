package data.importers

import data.ResourceStore
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
        strings: ResourceStore<Str>,
        plurals: ResourceStore<Plural>,
        arrays: ResourceStore<StringArray>
    ) -> List<File>
): List<File> {
    val importedFiles = withContext(Dispatchers.IO) {
        importFiles(vm.strings.propertyStore, vm.plurals.propertyStore, vm.arrays.propertyStore)
    }
    return importedFiles
}

fun <R : Resource> Map<ResourceId, R>.mergeWith(
    platform: Platform,
    locale: LocaleIsoCode,
    overwrite: Boolean,
    data: ResourceStore<R>,
) {
    val excludedResources = data.excludedResourcesByPlatform.value[platform]
    val localizedResourcesById = data.localizedResourcesById.value
    val addToGroup = mutableSetOf<ResourceId>()
    forEach { (resId, resource) ->
        if (excludedResources?.contains(resId) == true) {
            data.togglePlatform(resId, platform)
        } else if (!localizedResourcesById.contains(resId)) {
            Platform.values().forEach { if (it != platform) data.togglePlatform(resId, it) }
            addToGroup += resId
        }
        if (overwrite || localizedResourcesById[resId]?.contains(locale) != true) {
            data.updateResource(resId, locale, resource)
        }
    }
    data.putSelectedInGroup(ResourceGroup(), addToGroup)
}

fun File.parseDocument(): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
fun NodeList.toList() = List(length) { item(it) }.filterIsInstance<Element>()
