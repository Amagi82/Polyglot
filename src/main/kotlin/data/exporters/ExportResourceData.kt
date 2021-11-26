package data.exporters

import locales.LocaleIsoCode
import project.*

data class ExportResourceData<R : Resource, M : Metadata<M>>(
    val type: ResourceType,
    val metadataByIdByGroup: Map<GroupId, Map<ResourceId, M>>,
    val localizedResourcesById: Map<ResourceId, Map<LocaleIsoCode, R>>
)
