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
 * A Resource is a String, Plural, or String Array resource to be localized.
 *
 * @param id - must be snake case with all lowercase letters. iOS doesn't care about this, but Android does.
 * @param localizationType - a String, Plural, or StringArray
 * @param platforms - only localize the resource for these platform(s)
 */
data class Resource(
    val id: String,
    val localizationType: LocalizationType,
    val platforms: List<Platform>,
) {
    constructor(id: String, localizationType: LocalizationType, platforms: Array<out Platform>) : this(id, localizationType, platforms.toList())

    init {
        if (id.isBlank()) throw IllegalArgumentException("Resource id cannot be blank")
        if (!id.first().isLetter()) throw IllegalArgumentException("Resource id: $id must start with a letter")
        if (id.filter { it.isLetterOrDigit() || it == '_' }.lowercase() != id) throw IllegalArgumentException("Resource id: $id may only contain lower case letters, numbers, and underscores")
    }
}

// Convenience initializers
fun string(
    id: String,
    vararg translations: Pair<Language, String>,
    platforms: Array<Platform> = Platform.ALL
) = Resource(
    id = id,
    platforms = platforms,
    localizationType = Str(*translations)
)

fun plural(
    id: String,
    quantities: Quantities,
    platforms: Array<Platform> = Platform.ALL
) = Resource(id = id, localizationType = quantities, platforms = platforms)

fun stringArray(
    id: String,
    vararg items: Str,
    platforms: Array<Platform> = Platform.ALL
) = Resource(id = id, localizationType = StringArray(items.toList()), platforms = platforms)
