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
 * StringFormatter is used to replace arguments in your shared strings file to export properly across platforms.
 * For example, you may wish to localize %STORE to "Play Store" and "App Store" for Android and iOS, respectively
 *
 * @param arg - searches for this argument in a string, e.g. %d
 * @param platforms - formatters will only modify strings on the specified platform
 * @param isIndexed - Android string formatting is indexed, e.g. %s becomes %1$s or %2$s
 * @param formatter - When a match is found, this function replaces the arg.
 *                    index is only incremented if isIndexed == true
 *                    isXml is true if the file being created is in XML format. Always true on Android, false for normal strings in iOS
 *                    returns a String that replaces the arg
 */
class StringFormatter(
    val arg: String,
    val platforms: Array<Platform>,
    val isIndexed: Boolean = false,
    val formatter: (index: Int, isXml: Boolean) -> String
) {
    init {
        if (arg.isEmpty()) throw IllegalArgumentException("StringFormatter arg cannot be empty")
    }

    companion object {
        val defaultFormatters: List<StringFormatter> by lazy {
            listOf(
                StringFormatter("\n", Platform.ALL) { _, _ -> "\\n" },
                StringFormatter("\'", Platform.ANDROID_ONLY) { _, _ -> "\\'" },
                StringFormatter("%s", Platform.ANDROID_ONLY, isIndexed = true) { index, _ -> "%$index\$s" },
                StringFormatter("%d", Platform.ANDROID_ONLY, isIndexed = true) { index, _ -> "%$index\$d" },
                StringFormatter("%f", Platform.ANDROID_ONLY, isIndexed = true) { index, _ -> "%$index\$f" },
                StringFormatter("%s", Platform.IOS_ONLY) { _, _ -> "%@" },
                StringFormatter("\"", Platform.IOS_ONLY) { _, isXml -> if (isXml) "\\\"" else "\"" })
        }
    }
}
