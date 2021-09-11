/*
 * Copyright (C) 2018 Jim Pekarek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

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
abstract class Language(val isoCode: String) {
    init {
        when {
            isoCode.isBlank() -> throw IllegalArgumentException("Language isoCode cannot be blank")
            isoCode.lowercase() != isoCode -> throw IllegalArgumentException("isoCode $isoCode should contain lower case letters only")
            isoCode.length == 2 -> Unit // Good
            isoCode.length == 5 && isoCode[2] == '_' -> Unit // Good
            else -> throw IllegalArgumentException("Invalid language isoCode: $isoCode. Should be in the format \"en\" or \"en_us\"")
        }
    }
}

object English : Language("en")

object Spanish : Language("es")

object French : Language("fr")

object German : Language("de")
