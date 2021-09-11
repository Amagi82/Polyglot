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
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.transform.Transformer
import java.io.File

/**
 * Generates Android resources for a given language in the specified folder
 */
class AndroidResourceGenerator(
    androidFolder: File,
    private val language: Language,
    formatters: List<StringFormatter>,
    resources: Collection<Resource>
) : ResourceGenerator(Platform.ANDROID, formatters) {

    private val subFolder = File(androidFolder, "values${if (language == English) "" else "-${language.isoCode}"}").also { it.mkdirs() }
    private val document: Document = createDocument()
    private val resourceElement: Element = document.createElement("resources").also {
        if (language == English) it.setAttribute("xmlns:tools", "http://schemas.android.com/tools")
        document.appendChild(it)
    }

    private var prevPrefix = ""

    init {
        resources.forEach(::add)
    }

    override fun add(res: Resource) {
        if (platform !in res.platforms) return
        val prefix = res.id.takeWhile { it != '_' }
        if (prefix != prevPrefix) {
            prevPrefix = prefix
            val comment = when (prefix) {
                "btn" -> "Buttons"
                "acc" -> "Accessibility hints"
                "daily" -> "Daily Challenges"
                "icp" -> "Individual Course Pages"

                else -> prefix.replaceFirstChar(Char::titlecaseChar)
            }
            resourceElement.appendChild(document.createTextNode("\n"))
            resourceElement.appendChild(document.createComment(comment))
        }
        when (res.localizationType) {
            is Str -> addString(res.id, res.localizationType)
            is Quantities -> addPlurals(res.id, res.localizationType)
            is StringArray -> addStringArray(res.id, res.localizationType)
        }
    }

    override fun generateFiles() {
        transformer.transform(document, subFolder, "strings.xml")
    }

    /**
     * <plurals name="numberOfSongsAvailable">
     *     <item quantity="one">Znaleziono %d piosenkę.</item>
     *     <item quantity="few">Znaleziono %d piosenki.</item>
     *     <item quantity="other">Znaleziono %d piosenek.</item>
     * </plurals>
     */
    private fun addPlurals(id: String, quantities: Quantities) {
        resourceElement.appendChild(document.createElement("plurals").apply {
            setAttribute("name", id)
            Quantity.values().forEach { quantity ->
                val item = quantities.quantity(quantity) ?: return@forEach
                appendChild(document.createElement("item").apply {
                    setAttribute("quantity", quantity.label)
                    appendChild(document.createTextNode(item.fromLocale(language).sanitized()))
                })
            }
        })
    }

    private fun addString(id: String, str: Str) {
        val txt = str.fromLocale(language).sanitized()
        if (txt.isBlank()) {
            println("blank string found for id: $id")
        }
        //<string name="dragon">Trogdor the Burninator</string>
        resourceElement.appendChild(document.createElement("string").apply {
            setAttribute("name", id)
            appendChild(document.createTextNode(txt))
        })
    }

    /**
     * <string-array name="country_names">
     *      <item>France</item>
     *      <item>Germany</item>
     * </string-array>
     */
    private fun addStringArray(id: String, stringArray: StringArray) {
        resourceElement.appendChild(document.createElement("string-array").apply {
            setAttribute("name", id)
            if (stringArray.items.first().localizations.size == 1) setAttribute("tools:ignore", "MissingTranslation")
            for (item in stringArray.items) {
                appendChild(document, "item", item.fromLocale(language).sanitized())
            }
        })
    }

    companion object {
        private val transformer: Transformer by lazy { createTransformer() }
    }
}
