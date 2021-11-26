package data.exporters

import locales.LocaleIsoCode
import project.*

data class ExportProjectData(
    val defaultLocale: LocaleIsoCode,
    val locales: List<LocaleIsoCode>,
    val exportUrl: String,
    val strings: ExportResourceData<Str, StringMetadata>,
    val plurals: ExportResourceData<Plural, PluralMetadata>,
    val arrays: ExportResourceData<StringArray, ArrayMetadata>
)
