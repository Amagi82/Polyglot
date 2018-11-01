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
 * @param isoCode - this is used for folder names on Android, e.g. values or values.en,
 *                  and on iOS, e.g. en.lproj or es.lproj. If you encounter any cases where
 *                  these values should be different, please open an issue or submit a
 *                  pull request. English, Spanish, French, and German have been added as
 *                  examples.
 */
abstract class Language(val isoCode: String)

object English: Language("en")

object Spanish: Language("es")

object French: Language("fr")

object German: Language("de")