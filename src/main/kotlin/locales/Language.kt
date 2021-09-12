package locales

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Two or Three digit code (e.g. en, es, fr, de, tzm), used to place the translation in the appropriate folder, e.g.
 * Android: values.en, values.es, etc
 * iOS: en.lproj, es.lproj, etc
 */
typealias LanguageIsoCode = String

/**
 * @param isoCode - ISO 639 language codes + optional ISO 3166 country codes.
 *
 * Android and iOS use these to choose the appropriate localization for a given user.
 *
 * Generally, the language code alone should be used for most values, with country code provided
 * when you want to customize the translation for a given region. If you only provide es_ec and en,
 * the device may show the English localization even if the user prefers Spanish.
 *
 * This is used by Polyglot to organize resources into appropriate folders, e.g.:
 * Android: values (default, usually English), values.es, values.fr_ca, etc.
 * iOS: en.lproj, es.lproj, fr_ca.lproj, etc.
 *
 * English, Spanish, French, and German have been added for convenience.
 */
@Stable
@Serializable
data class Language(val isoCode: LanguageIsoCode, val name: String) {
    init {
        when {
            isoCode.isBlank() -> throw IllegalArgumentException("locales.Language isoCode cannot be blank")
            isoCode.lowercase() != isoCode -> throw IllegalArgumentException("isoCode $isoCode should contain lower case letters only")
            isoCode.length == 2 -> Unit // Good
            isoCode.length == 5 && isoCode[2] == '_' -> Unit // Good
            else -> throw IllegalArgumentException("Invalid language isoCode: $isoCode. Should be in the format \"en\" or \"en_us\"")
        }
    }

    companion object {
//        operator fun get(isoCode: locales.LanguageIsoCode) = locales.Language(isoCode = isoCode, name = names[isoCode]!!)
//        const val path = "/languages"

        /**
         * English, Spanish, German, etc from language isoCode
         */
//        val names: MutableMap<locales.LanguageIsoCode, String> = Json.decodeFromString("")
           /* mutableMapOf<locales.LanguageIsoCode, String>().apply {
            put("en", "English")
            put("fr", "French")
            put("es", "Spanish")
            put("de", "German")
            put("ja", "Japanese")
            put("it", "Italian")
            put("zh", "Chinese")
            put("hi", "Hindi")
            put("nl", "Dutch")
            put("pt", "Portuguese")
            put("ru", "Russian")
            put("ko", "Korean")
            put("sv", "Swedish")
            put("ar", "Arabic")
            put("pl", "Polish")
            put("da", "Danish")
            put("tr", "Turkish")
            put("el", "Greek")
            put("uk", "Ukrainian")
            put("fi", "Finnish")

            put("af", "Afrikaans")
            put("agq", "Aghem")
            put("ak", "Akan")
            put("am", "Amharic")
            put("as", "Assamese")
            put("asa", "Asu")
            put("az", "Azerbaijani")
            put("bas", "Basaa")
            put("be", "Belarusian")
            put("bem", "Bemba")
            put("bez", "Bena")
            put("bg", "Bulgarian")
            put("bm", "Bambara")
            put("bn", "Bengali")
            put("bo", "Tibetan")
            put("br", "Breton")
            put("brx", "Bodo")
            put("bs", "Bosnian")
            put("ca", "Catalan")
            put("cgg", "Chiga")
            put("chr", "Cherokee")
            put("cs", "Czech")
            put("cy", "Welsh")
            put("dav", "Taita")
            put("dje", "Zarma")
            put("dua", "Duala")
            put("dyo", "Jola-Fonyi")
            put("dz", "Dzongkha")
            put("ebu", "Embu")
            put("ee", "Ewe")
            put("eo", "Esperanto")
            put("et", "Estonian")
            put("eu", "Basque")
            put("ewo", "Ewondo")
            put("fa", "Persian")
            put("ff", "Fulah")
            put("fil", "Filipino")
            put("fo", "Faroese")
            put("ga", "Irish")
            put("gl", "Galician")
            put("gsw", "Swiss German")
            put("gu", "Gujarati")
            put("guz", "Gusii")
            put("gv", "Manx")
            put("ha", "Hausa")
            put("haw", "Hawaiian")
            put("iw", "Hebrew")
            put("hr", "Croatian")
            put("hu", "Hungarian")
            put("hy", "Armenian")
            put("in", "Indonesian")
            put("ig", "Igbo")
            put("ii", "Sichuan Yi")
            put("is", "Icelandic")
            put("jgo", "Ngomba")
            put("jmc", "Machame")
            put("ka", "Georgian")
            put("kab", "Kabyle")
            put("kam", "Kamba")
            put("kde", "Makonde")
            put("kea", "Kabuverdianu")
            put("khq", "Koyra Chiini")
            put("ki", "Kikuyu")
            put("kk", "Kazakh")
            put("kkj", "Kako")
            put("kl", "Kalaallisut")
            put("kln", "Kalenjin")
            put("km", "Khmer")
            put("kn", "Kannada")
            put("kok", "Konkani")
            put("ks", "Kashmiri")
            put("ksb", "Shambala")
            put("ksf", "Bafia")
            put("kw", "Cornish")
            put("ky", "Kyrgyz")
            put("lag", "Langi")
            put("lg", "Ganda")
            put("lkt", "Lakota")
            put("ln", "Lingala")
            put("lo", "Lao")
            put("lt", "Lithuanian")
            put("lu", "Luba-Katanga")
            put("luo", "Luo")
            put("luy", "Luyia")
            put("lv", "Latvian")
            put("mas", "Masai")
            put("mer", "Meru")
            put("mfe", "Morisyen")
            put("mg", "Malagasy")
            put("mgh", "Makhuwa-Meetto")
            put("mgo", "Meta'")
            put("mk", "Macedonian")
            put("ml", "Malayalam")
            put("mn", "Mongolian")
            put("mr", "Marathi")
            put("ms", "Malay")
            put("mt", "Maltese")
            put("mua", "Mundang")
            put("my", "Burmese")
            put("naq", "Nama")
            put("nb", "Norwegian Bokmål")
            put("nd", "North Ndebele")
            put("ne", "Nepali")
            put("nmg", "Kwasio")
            put("nn", "Norwegian Nynorsk")
            put("nnh", "Ngiemboon")
            put("nus", "Nuer")
            put("nyn", "Nyankole")
            put("om", "Oromo")
            put("or", "Oriya")
            put("pa", "Punjabi")
            put("ps", "Pashto")
            put("rm", "Romansh")
            put("rn", "Rundi")
            put("ro", "Romanian")
            put("rof", "Rombo")
            put("rw", "Kinyarwanda")
            put("rwk", "Rwa")
            put("saq", "Samburu")
            put("sbp", "Sangu")
            put("seh", "Sena")
            put("ses", "Koyraboro Senni")
            put("sg", "Sango")
            put("shi", "Tachelhit")
            put("si", "Sinhala")
            put("sk", "Slovak")
            put("sl", "Slovenian")
            put("sn", "Shona")
            put("so", "Somali")
            put("sq", "Albanian")
            put("sr", "Serbian")
            put("sw", "Swahili")
            put("swc", "Congo Swahili")
            put("ta", "Tamil")
            put("te", "Telugu")
            put("teo", "Teso")
            put("th", "Thai")
            put("ti", "Tigrinya")
            put("tlh", "Klingon")
            put("to", "Tongan")
            put("twq", "Tasawaq")
            put("tzm", "Central Atlas Tamazight")
            put("ug", "Uyghur")
            put("ur", "Urdu")
            put("uz", "Uzbek")
            put("vai", "Vai")
            put("vi", "Vietnamese")
            put("vun", "Vunjo")
            put("xog", "Soga")
            put("yav", "Yangben")
            put("yo", "Yoruba")
            put("zgh", "Standard Moroccan Tamazight")
            put("zu", "Zulu")
        }*/

        /**
         * @param isoCode: locales.Language isoCode, e.g. en, es, fr
         * @return list of region isoCodes where the language is spoken
         */
//        fun regionCodes(isoCode: locales.LanguageIsoCode) = regions[isoCode]

//        val regions: MutableMap<locales.LanguageIsoCode, List<locales.RegionIsoCode>> = Json.decodeFromString("")
            /*mutableMapOf<locales.LanguageIsoCode, List<locales.RegionIsoCode>>().apply {
            put("af", listOf("NA", "ZA"))
            put("agq", listOf("CM"))
            put("ak", listOf("GH"))
            put("am", listOf("ET"))
            put(
                "ar",
                listOf(
                    "AE",
                    "BH",
                    "DJ",
                    "DZ",
                    "EG",
                    "EH",
                    "ER",
                    "IL",
                    "IQ",
                    "JO",
                    "KM",
                    "KW",
                    "LB",
                    "LY",
                    "MA",
                    "MR",
                    "OM",
                    "PS",
                    "QA",
                    "SA",
                    "SD",
                    "SO",
                    "SS",
                    "SY",
                    "TD",
                    "TN",
                    "YE"
                )
            )
            put("as", listOf("IN"))
            put("asa", listOf("TZ"))
            put("az", listOf("AZ"))
            put("bas", listOf("CM"))
            put("be", listOf("BY"))
            put("bem", listOf("ZM"))
            put("bez", listOf("TZ"))
            put("bg", listOf("BG"))
            put("bm", listOf("ML"))
            put("bn", listOf("BD", "IN"))
            put("bo", listOf("CN", "IN"))
            put("br", listOf("FR"))
            put("brx", listOf("IN"))
            put("bs", listOf("BA"))
            put("ca", listOf("AD", "ES", "FR", "IT"))
            put("cgg", listOf("UG"))
            put("chr", listOf("US"))
            put("cs", listOf("CZ"))
            put("cy", listOf("GB"))
            put("da", listOf("DK", "GL"))
            put("dav", listOf("KE"))
            put("de", listOf("AT", "BE", "CH", "DE", "LI", "LU"))
            put("dje", listOf("NE"))
            put("dua", listOf("CM"))
            put("dyo", listOf("SN"))
            put("dz", listOf("BT"))
            put("ebu", listOf("KE"))
            put("ee", listOf("GH", "TG"))
            put("el", listOf("CY", "GR"))
            put(
                "en",
                listOf(
                    "AG",
                    "AI",
                    "AS",
                    "AU",
                    "BB",
                    "BE",
                    "BM",
                    "BS",
                    "BW",
                    "BZ",
                    "CA",
                    "CC",
                    "CK",
                    "CM",
                    "CX",
                    "DG",
                    "DM",
                    "ER",
                    "FJ",
                    "FK",
                    "FM",
                    "GB",
                    "GD",
                    "GG",
                    "GH",
                    "GI",
                    "GM",
                    "GU",
                    "GY",
                    "HK",
                    "IE",
                    "IM",
                    "IN",
                    "IO",
                    "JE",
                    "JM",
                    "KE",
                    "KI",
                    "KN",
                    "KY",
                    "LC",
                    "LR",
                    "LS",
                    "MG",
                    "MH",
                    "MO",
                    "MP",
                    "MS",
                    "MT",
                    "MU",
                    "MW",
                    "NA",
                    "NF",
                    "NG",
                    "NR",
                    "NU",
                    "NZ",
                    "PG",
                    "PH",
                    "PK",
                    "PN",
                    "PR",
                    "PW",
                    "RW",
                    "SB",
                    "SC",
                    "SD",
                    "SG",
                    "SH",
                    "SL",
                    "SS",
                    "SX",
                    "SZ",
                    "TC",
                    "TK",
                    "TO",
                    "TT",
                    "TV",
                    "TZ",
                    "UG",
                    "UM",
                    "US",
                    "VC",
                    "VG",
                    "VI",
                    "VU",
                    "WS",
                    "ZA",
                    "ZM",
                    "ZW"
                )
            )
            put("eo", listOf())
            put(
                "es",
                listOf(
                    "AR",
                    "BO",
                    "CL",
                    "CO",
                    "CR",
                    "CU",
                    "DO",
                    "EA",
                    "EC",
                    "ES",
                    "GQ",
                    "GT",
                    "HN",
                    "IC",
                    "MX",
                    "NI",
                    "PA",
                    "PE",
                    "PH",
                    "PR",
                    "PY",
                    "SV",
                    "US",
                    "UY",
                    "VE"
                )
            )
            put("et", listOf("EE"))
            put("eu", listOf("ES"))
            put("ewo", listOf("CM"))
            put("fa", listOf("AF", "IR"))
            put("ff", listOf("SN"))
            put("fi", listOf("FI"))
            put("fil", listOf("PH"))
            put("fo", listOf("FO"))
            put(
                "fr",
                listOf(
                    "BE",
                    "BF",
                    "BI",
                    "BJ",
                    "BL",
                    "CA",
                    "CD",
                    "CF",
                    "CG",
                    "CH",
                    "CI",
                    "CM",
                    "DJ",
                    "DZ",
                    "FR",
                    "GA",
                    "GF",
                    "GN",
                    "GP",
                    "GQ",
                    "HT",
                    "KM",
                    "LU",
                    "MA",
                    "MC",
                    "MF",
                    "MG",
                    "ML",
                    "MQ",
                    "MR",
                    "MU",
                    "NC",
                    "NE",
                    "PF",
                    "PM",
                    "RE",
                    "RW",
                    "SC",
                    "SN",
                    "SY",
                    "TD",
                    "TG",
                    "TN",
                    "VU",
                    "WF",
                    "YT"
                )
            )
            put("ga", listOf("IE"))
            put("gl", listOf("ES"))
            put("gsw", listOf("CH", "LI"))
            put("gu", listOf("IN"))
            put("guz", listOf("KE"))
            put("gv", listOf("IM"))
            put("ha", listOf("GH", "NE", "NG"))
            put("haw", listOf("US"))
            put("iw", listOf("IL"))
            put("hi", listOf("IN"))
            put("hr", listOf("BA"))
            put("hu", listOf("HU"))
            put("hy", listOf("AM"))
            put("in", listOf("ID"))
            put("ig", listOf("NG"))
            put("ii", listOf("CN"))
            put("is", listOf("IS"))
            put("it", listOf("CH", "IT", "SM"))
            put("ja", listOf("JP"))
            put("jgo", listOf("CM"))
            put("jmc", listOf("TZ"))
            put("ka", listOf("GE"))
            put("kab", listOf("DZ"))
            put("kam", listOf("KE"))
            put("kde", listOf("TZ"))
            put("kea", listOf("CV"))
            put("khq", listOf("ML"))
            put("ki", listOf("KE"))
            put("kk", listOf("KZ"))
            put("kkj", listOf("CM"))
            put("kl", listOf("GL"))
            put("kln", listOf("KE"))
            put("km", listOf("KH"))
            put("kn", listOf("IN"))
            put("ko", listOf("KP", "KR"))
            put("kok", listOf("IN"))
            put("ks", listOf("IN"))
            put("ksb", listOf("TZ"))
            put("ksf", listOf("CM"))
            put("kw", listOf("GB"))
            put("ky", listOf("KG"))
            put("lag", listOf("TZ"))
            put("lg", listOf("UG"))
            put("lkt", listOf("US"))
            put("ln", listOf("AO", "CD", "CF", "CG"))
            put("lo", listOf("LA"))
            put("lt", listOf("LT"))
            put("lu", listOf("CD"))
            put("luo", listOf("KE"))
            put("luy", listOf("KE"))
            put("lv", listOf("LV"))
            put("mas", listOf("KE", "TZ"))
            put("mer", listOf("KE"))
            put("mfe", listOf("MU"))
            put("mg", listOf("MG"))
            put("mgh", listOf("MZ"))
            put("mgo", listOf("CM"))
            put("mk", listOf("MK"))
            put("ml", listOf("IN"))
            put("mn", listOf("MN"))
            put("mr", listOf("IN"))
            put("ms", listOf("BN", "MY", "SG"))
            put("mt", listOf("MT"))
            put("mua", listOf("CM"))
            put("my", listOf("MM"))
            put("naq", listOf("NA"))
            put("nb", listOf("NO", "SJ"))
            put("nd", listOf("ZW"))
            put("ne", listOf("IN", "NP"))
            put("nl", listOf("AW", "BE", "BQ", "CW", "NL", "SR", "SX"))
            put("nmg", listOf("CM"))
            put("nn", listOf("NO"))
            put("nnh", listOf("CM"))
            put("nus", listOf("SD"))
            put("nyn", listOf("UG"))
            put("om", listOf("ET", "KE"))
            put("or", listOf("IN"))
            put("pa", listOf("PK", "IN"))
            put("pl", listOf("PL"))
            put("ps", listOf("AF"))
            put("pt", listOf("AO", "BR", "CV", "GW", "MO", "MZ", "PT", "ST", "TL"))
            put("rm", listOf("CH"))
            put("rn", listOf("BI"))
            put("ro", listOf("MD", "RO"))
            put("rof", listOf("TZ"))
            put("ru", listOf("BY", "KG", "KZ", "MD", "RU", "UA"))
            put("rw", listOf("RW"))
            put("rwk", listOf("TZ"))
            put("saq", listOf("KE"))
            put("sbp", listOf("TZ"))
            put("seh", listOf("MZ"))
            put("ses", listOf("ML"))
            put("sg", listOf("CF"))
            put("shi", listOf("MA"))
            put("si", listOf("LK"))
            put("sk", listOf("SK"))
            put("sl", listOf("SI"))
            put("sn", listOf("ZW"))
            put("so", listOf("DJ", "ET", "KE", "SO"))
            put("sq", listOf("AL", "MK", "XK"))
            put("sr", listOf("BA", "ME", "RS", "XK"))
            put("sv", listOf("AX", "FI", "SE"))
            put("sw", listOf("KE", "TZ", "UG"))
            put("swc", listOf("CD"))
            put("ta", listOf("IN", "LK", "MY", "SG"))
            put("te", listOf("IN"))
            put("teo", listOf("KE", "UG"))
            put("th", listOf("TH"))
            put("ti", listOf("ER", "ET"))
            put("to", listOf("TO"))
            put("tr", listOf("CY", "TR"))
            put("twq", listOf("NE"))
            put("tzm", listOf("MA"))
            put("ug", listOf("CN"))
            put("uk", listOf("UA"))
            put("ur", listOf("IN", "PK"))
            put("uz", listOf("AF", "UZ"))
            put("vai", listOf("LR"))
            put("vi", listOf("VN"))
            put("vun", listOf("TZ"))
            put("xog", listOf("UG"))
            put("yav", listOf("CM"))
            put("yo", listOf("BJ", "NG"))
            put("zgh", listOf("MA"))
            put("zh", listOf("CN", "HK", "MO", "SG", "TW"))
            put("zu", listOf("ZA"))
        }*/
    }
}
