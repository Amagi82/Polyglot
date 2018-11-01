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

package utils

import models.*

fun string(id: String,
           platform: Platform = Platform.ALL,
           en: String,
           es: String,
           fr: String,
           de: String) = Resource(
        id = id,
        platform = platform,
        localizationType = Str(Localization(English, en), Localization(Spanish, es), Localization(French, fr), Localization(German, de)))

fun plural(
        id: String,
        platform: Platform = Platform.ALL,
        plural: Plural) = Resource(id = id, platform = platform, localizationType = plural)

fun stringArray(
        id: String,
        platform: Platform = Platform.ALL,
        items: StringArray) = Resource(id = id, platform = platform, localizationType = items)

//for plurals
fun quantities(zero: Str? = null,
               one: Str?,
               two: Str? = null,
               few: Str? = null,
               many: Str? = null,
               other: Str) = Plural(zero, one, two, few, many, other)

//for string arrays
fun items(vararg items: Str) = StringArray(items.toList())