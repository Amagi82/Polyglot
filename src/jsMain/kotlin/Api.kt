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

//suspend fun getLocales(): List<Locale> = jsonClient.get("$endpoint/locales")
//
//suspend fun addLocale(locale: Locale) {
//    jsonClient.post<Unit>("$endpoint/locales") {
//        contentType(ContentType.Application.Json)
//        body = locale
//    }
//}
//
//suspend fun deleteLocale(locale: Locale) {
//    jsonClient.delete<Unit>("$endpoint/locales/${locale.isoCode}")
//}

suspend fun getLanguages(): Map<LanguageIsoCode, String> = jsonClient.get("$endpoint/languages")
suspend fun getLanguageRegions(): Map<LanguageIsoCode, List<RegionIsoCode>> = jsonClient.get("$endpoint/language_regions")
suspend fun getRegions(): Map<RegionIsoCode, String> = jsonClient.get("$endpoint/regions")
