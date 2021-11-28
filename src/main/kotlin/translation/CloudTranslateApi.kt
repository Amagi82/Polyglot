package translation

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import locales.LocaleIsoCode

object CloudTranslateApi {
    suspend fun translate(apiKey: String, from: LocaleIsoCode, to: LocaleIsoCode, text: List<String>): Result<TranslateResponse> = runCatching {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        client.use {
            it.get<TranslateResponse>("https://translate.googleapis.com/language/translate/v2") {
                url.parameters.apply {
                    append("source", from.value)
                    append("target", to.value)
                    append("format", "text")
                    append("key", apiKey)
                    appendAll("q", text)
                }
            }
        }
    }.also(::println)

    suspend fun supportedLanguages(apiKey: String): Result<LanguageResponse> = runCatching {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        client.use {
            it.get<LanguageResponse>("https://translation.googleapis.com/language/translate/v2/languages") {
                url.parameters.apply {
                    append("key", apiKey)
                }
            }
        }
    }.also(::println)
}

@Serializable
data class TranslateResponse(val data: Translations) {

    @Serializable
    data class Translations(val translations: List<TranslatedText>) {

        @Serializable
        data class TranslatedText(val translatedText: String)
    }
}

@Serializable
data class LanguageResponse(val data: Languages) {

    @Serializable
    data class Languages(val languages: List<Language>) {

        @Serializable
        data class Language(val language: String)
    }
}
