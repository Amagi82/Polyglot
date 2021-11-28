package data.exporters

import locales.LocaleIsoCode
import project.*

data class ExportProjectData(
    val defaultLocale: LocaleIsoCode,
    val locales: List<LocaleIsoCode>,
    val exportUrl: String,
    val strings: ExportResourceData<Str>,
    val plurals: ExportResourceData<Plural>,
    val arrays: ExportResourceData<StringArray>
)
