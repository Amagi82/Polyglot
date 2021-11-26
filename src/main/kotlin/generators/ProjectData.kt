package generators

import locales.LocaleIsoCode
import project.*

data class ProjectData(
    val defaultLocale: LocaleIsoCode,
    val locales: List<LocaleIsoCode>,
    val exportUrl: String,
    val strings: ResourceData<Str, StringMetadata>,
    val plurals: ResourceData<Plural, PluralMetadata>,
    val arrays: ResourceData<StringArray, ArrayMetadata>
)
