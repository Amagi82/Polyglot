import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

fun main() {
    embeddedServer(Netty, System.getenv("PORT")?.toInt() ?: 9090) {
        // Enables automatic serialization/deserialization
        install(ContentNegotiation) {
            json()
        }
        // Configures Cross-Origin Resource Sharing
        install(CORS) {
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Delete)
            anyHost()
        }
        install(Compression) {
            gzip()
        }

        routing {
            get("/") {
                call.respondText(
                    Resources.index_html.readText(),
                    ContentType.Text.Html
                )
            }
            static {
                resources("")
                file("languages.json")
                file("language_regions.json")
                file("regions.json")
            }
//            route("/locales") {
//                get {
//                    call.respond(Locale.all)
//                }
//            }
            route("/languages") {
                get {
                    call.respondFile(File(Resources.languages_json.file))
                }

//                post {
//                    val newLocale = call.receive<Locale>()
//                    newLocale.language.let { Language.names.putIfAbsent(it.isoCode, it.name) }
//                    newLocale.region?.let { region ->
//                        Region.names.putIfAbsent(region.isoCode, region.name)
//                        Language.regions.compute(newLocale.language.isoCode) { _, regions ->
//                            regions.orEmpty().plus(region.isoCode).distinct()
//                        }
//                    }
//                    call.respond(HttpStatusCode.OK)
//                }
//                delete("/{isoCode}") {
//                    val isoCode = call.parameters["isoCode"] ?: error("Invalid delete request")
//                    val languageIsoCode = isoCode.takeWhile { it != '_' }
//                    val regionIsoCode = isoCode.substringAfter('_', missingDelimiterValue = "")
//                    if (regionIsoCode.isEmpty()) {
//                        Language.names.remove(languageIsoCode)
//                    } else {
//                        Language.regions.computeIfPresent(languageIsoCode) { _, regions ->
//                            regions.minus(regionIsoCode)
//                        }
//                        Language.regions[languageIsoCode]
//                    }
//                    call.respond(HttpStatusCode.OK)
//                }

//                route("/{lang}"){
//                    get {
//                        val language = call.parameters["lang"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Language must be specified")
//                        call.respond(Language(isoCode = language, name = Resources.languages[language].orEmpty()))
//                    }
//
//                    route("/regions") {
//                        get {
//                            val language = call.parameters["lang"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Language must be specified")
//                            val regions = Resources.languageRegions[language] ?: return@get call.respond(HttpStatusCode.NoContent, "No regions for $language")
//                            call.respond(regions)
//                        }
//
//                        post {
//                            val newLocale = call.receive<Locale>()
//                            newLocale.language.let { Language.names.putIfAbsent(it.isoCode, it.name) }
//                            newLocale.region?.let { region ->
//                                Region.names.putIfAbsent(region.isoCode, region.name)
//                                Language.regions.compute(newLocale.language.isoCode) { _, regions ->
//                                    regions.orEmpty().plus(region.isoCode).distinct()
//                                }
//                            }
//                            call.respond(HttpStatusCode.OK)
//                        }
//                        delete("/{isoCode}") {
//                            val isoCode = call.parameters["isoCode"] ?: error("Invalid delete request")
//                            val languageIsoCode = isoCode.takeWhile { it != '_' }
//                            val regionIsoCode = isoCode.substringAfter('_', missingDelimiterValue = "")
//                            if (regionIsoCode.isEmpty()) {
//                                Language.names.remove(languageIsoCode)
//                            } else {
//                                Language.regions.computeIfPresent(languageIsoCode) { _, regions ->
//                                    regions.minus(regionIsoCode)
//                                }
//                                Language.regions[languageIsoCode]
//                            }
//                            call.respond(HttpStatusCode.OK)
//                        }
//                    }
//                }
            }
            route("/regions") {
                get {
                    call.respondFile(File(Resources.regions_json.file))
                }

//                post {
//                    val newLocale = call.receive<Locale>()
//                    newLocale.language.let { Language.names.putIfAbsent(it.isoCode, it.name) }
//                    newLocale.region?.let { region ->
//                        Region.names.putIfAbsent(region.isoCode, region.name)
//                        Language.regions.compute(newLocale.language.isoCode) { _, regions ->
//                            regions.orEmpty().plus(region.isoCode).distinct()
//                        }
//                    }
//                    call.respond(HttpStatusCode.OK)
//                }
//                delete("/{isoCode}") {
//                    val isoCode = call.parameters["isoCode"] ?: error("Invalid delete request")
//                    val languageIsoCode = isoCode.takeWhile { it != '_' }
//                    val regionIsoCode = isoCode.substringAfter('_', missingDelimiterValue = "")
//                    if (regionIsoCode.isEmpty()) {
//                        Language.names.remove(languageIsoCode)
//                    } else {
//                        Language.regions.computeIfPresent(languageIsoCode) { _, regions ->
//                            regions.minus(regionIsoCode)
//                        }
//                        Language.regions[languageIsoCode]
//                    }
//                    call.respond(HttpStatusCode.OK)
//                }
            }

            route("/language_regions") {
                get {
                    call.respondFile(File(Resources.language_regions_json.file))
                }
            }
        }
    }.start(wait = true)
}

object Resources {
    val index_html = getResource("index.html")
    val languages_json = getResource("languages.json")
    val language_regions_json = getResource("language_regions.json")
    val regions_json = getResource("regions.json")

//    val languages: MutableMap<LanguageIsoCode, String> by lazy { Json.decodeFromString(languages_json.readText()) }
//    val languageRegions: MutableMap<LanguageIsoCode, List<RegionIsoCode>> by lazy { Json.decodeFromString(language_regions_json.readText()) }
//    val regions: MutableMap<RegionIsoCode, String> by lazy { Json.decodeFromString(regions_json.readText()) }

    private fun getResource(fileName: String): URL = this::class.java.getResource(fileName)
}
