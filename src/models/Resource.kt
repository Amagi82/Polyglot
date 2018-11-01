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

import java.lang.IllegalArgumentException

/*
 * A Resource is a String, Plural, or String Array resource to be localized.
 *
 * @param id - should be snake case with all lowercase letters. iOS doesn't care about this,
 *             but Android does.
 * @param platform - specify this resource should only be localized for a given platform
 * @param localizationType - a String, Plural, or StringArray
 */
data class Resource(
        val id: String,
        val platform: Platform = Platform.ALL,
        val localizationType: LocalizationType) {
    init {
        if (id.toLowerCase() != id) throw IllegalArgumentException("Resource ids must be all lower case")
    }
}

