import io.ktor.http.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.browser.window

val endpoint = window.location.origin

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

/** Static resources */
suspend fun getStaticLanguages(): Map<LanguageIsoCode, String> = jsonClient.get("$endpoint/languages.json")
suspend fun getStaticLanguageRegions(): Map<LanguageIsoCode, List<RegionIsoCode>> = jsonClient.get("$endpoint/language_regions.json")
suspend fun getStaticRegions(): Map<RegionIsoCode, String> = jsonClient.get("$endpoint/regions.json")

/** Locales */
suspend fun getLocales(): List<Locale> = jsonClient.get("$endpoint/locales")
suspend fun addLocale(locale: Locale) = jsonClient.post<Unit>("$endpoint/locales") {
    contentType(ContentType.Application.Json)
    body = locale
}

suspend fun deleteLocale(locale: Locale) = jsonClient.delete<Unit>("$endpoint/locales/${locale.isoCode}")


/** Resources */
suspend fun getResources(): List<Resource> = jsonClient.get("$endpoint/resources")
suspend fun addResource(resource: Resource) = jsonClient.post<Unit>("$endpoint/resources") {
    contentType(ContentType.Application.Json)
    body = resource
}

suspend fun deleteResource(resource: Resource) = jsonClient.delete<Unit>("$endpoint/resources/${resource.id}")
