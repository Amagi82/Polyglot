package data.exporters

import locales.LocaleIsoCode
import project.Metadata
import project.Resource
import project.ResourceId
import project.ResourceType

data class ResourceData<R : Resource, M : Metadata<M>>(
    val type: ResourceType,
    val metadataById: Map<ResourceId, M>,
    val localizedResourcesById: Map<ResourceId, Map<LocaleIsoCode, R>>
)
