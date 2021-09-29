package data

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import locales.LocaleIsoCode
import project.Platform
import sqldelight.*

private val langAdapter = object : ColumnAdapter<LocaleIsoCode, String> {
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

val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + "polyglot/polyglot.db")

val polyglotDatabase = PolyglotDatabase(
    driver = driver,
    ArrayLocalizationsAdapter = ArrayLocalizations.Adapter(langAdapter = langAdapter, arrayAdapter = arrayAdapter),
    PluralLocalizationsAdapter = PluralLocalizations.Adapter(langAdapter = langAdapter),
    ProjectAdapter = Project.Adapter(localesAdapter = localesAdapter),
    ResourceAdapter = Resource.Adapter(platformsAdapter = platformsAdapter, typeAdapter = EnumColumnAdapter()),
    StringLocalizationsAdapter = StringLocalizations.Adapter(langAdapter = langAdapter)
)
