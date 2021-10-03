package data

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import locales.LocaleIsoCode
import project.Platform
import sqldelight.*

private val localeIsoCodeAdapter = object : ColumnAdapter<LocaleIsoCode, String> {
    override fun decode(databaseValue: String): LocaleIsoCode = LocaleIsoCode(databaseValue)
    override fun encode(value: LocaleIsoCode): String = value.value
}

private val platformsAdapter = object : ColumnAdapter<List<Platform>, String> {
    override fun decode(databaseValue: String): List<Platform> = if (databaseValue.isBlank()) listOf() else databaseValue.split(',').map(Platform::valueOf)
    override fun encode(value: List<Platform>): String = value.joinToString(",")
}

private val arrayAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> = databaseValue.split('|')
    override fun encode(value: List<String>): String = value.joinToString("|")
}

private val localesAdapter = object : ColumnAdapter<List<LocaleIsoCode>, String> {
    override fun decode(databaseValue: String): List<LocaleIsoCode> = databaseValue.split(',').map(::LocaleIsoCode)
    override fun encode(value: List<LocaleIsoCode>): String = value.joinToString(",") { it.value }
}

val polyglotDatabase = PolyglotDatabase(
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + "polyglot/polyglot.db"),
    ArrayLocalizationsAdapter = ArrayLocalizations.Adapter(localeAdapter = localeIsoCodeAdapter, arrayAdapter = arrayAdapter),
    PluralLocalizationsAdapter = PluralLocalizations.Adapter(localeAdapter = localeIsoCodeAdapter),
    ProjectAdapter = Project.Adapter(localesAdapter = localesAdapter, defaultLocaleAdapter = localeIsoCodeAdapter),
    ResourceAdapter = Resource.Adapter(platformsAdapter = platformsAdapter, typeAdapter = EnumColumnAdapter()),
    StringLocalizationsAdapter = StringLocalizations.Adapter(localeAdapter = localeIsoCodeAdapter)
)
