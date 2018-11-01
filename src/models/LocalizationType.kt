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

/*
 * Strings, Plurals, and Arrays each require different formatting on Android and iOS.
 */
sealed class LocalizationType {

    /*
     * String resource, abbreviated to avoid confusion
     */
    data class Str(val localizations: List<Localization>) : LocalizationType() {
        constructor(vararg localizations: Localization) : this(localizations.toList())
        constructor(vararg localizations: Pair<Language, String>) : this(localizations.map { Localization(it.first, it.second) })

        fun fromLocale(language: Language) = localizations.first { it.language == language }.txt
    }

    /*
     * Plural resource
     */
    data class Plural(
            val zero: Str? = null,
            val one: Str?,
            val two: Str? = null,
            val few: Str? = null,
            val many: Str? = null,
            val other: Str) : LocalizationType() {

        fun quantity(quantity: Quantities) = when (quantity) {
            Quantities.ZERO -> zero
            Quantities.ONE -> one
            Quantities.TWO -> two
            Quantities.FEW -> few
            Quantities.MANY -> many
            Quantities.OTHER -> other
        }
    }

    /*
     * String array resource
     */
    data class StringArray(val items: List<Str>) : LocalizationType()
}