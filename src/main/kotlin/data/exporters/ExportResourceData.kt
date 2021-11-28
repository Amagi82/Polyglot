package data.exporters

import locales.LocaleIsoCode
import project.*

data class ExportResourceData<R : Resource>(
    val type: ResourceType,
    val resourceGroups: Map<ResourceGroup, Set<ResourceId>>,
    val excludedResourcesByPlatform: Map<Platform, Set<ResourceId>>,
    val localizedResourcesById: Map<ResourceId, Map<LocaleIsoCode, R>>
)
