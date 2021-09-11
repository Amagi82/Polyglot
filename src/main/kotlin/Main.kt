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

import models.*
import models.Platform.Companion.ANDROID_ONLY
import models.Platform.Companion.IOS_ONLY
import utils.*


fun main() {
    val resources = resources()
    ResourceGenerator.generateFiles(resources.sortedBy { it.id }, English)
//    AndroidReverseResourceGenerator(File("strings.xml"), name = "resources").generate()
}

private fun resources() = listOf(
    // Misc buttons
    string(id = "btn_cancel", English to "Cancel", platforms = IOS_ONLY),
    string(id = "btn_close", English to "Close", platforms = IOS_ONLY),
    string(id = "btn_continue", English to "Continue"),
    string(id = "btn_read_less", English to "Read less", platforms = ANDROID_ONLY),
    string(id = "btn_read_more", English to "Read more", platforms = ANDROID_ONLY),
    plural(
        id = "expires_in_days",
        quantities = Quantities(
            one = Str(English to "Expires in %d day"),
            other = Str(English to "Expires in %d days"),
        )
    ),
    plural(
        id = "expires_in_hours",
        quantities = Quantities(
            one = Str(English to "Expires in %d hour"),
            other = Str(English to "Expires in %d hours"),
        )
    ),
)
