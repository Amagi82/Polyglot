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

import models.Language
import models.Platform
import models.Platform.*
import models.Platform.Companion.ALL
import models.StringFormatter
import models.Resource
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class ResourceGenerator(val platform: Platform, formatters: List<StringFormatter>) {
    abstract fun generateFiles()
    abstract fun add(res: Resource)

    private val formatters: List<StringFormatter> = formatters.filter { platform in it.platforms }

    protected fun Element.appendChild(document: Document, tagName: String, textNode: String) {
        appendChild(document.createElement(tagName).also { it.appendChild(document.createTextNode(textNode)) })
    }

    protected fun File.createChildFile(filename: String) = File(this, filename).also { it.createNewFile() }

    protected fun Transformer.transform(document: Document, folder: File, filename: String) {
        transform(DOMSource(document), StreamResult(folder.createChildFile(filename)))
    }

    protected fun String.sanitized(isXml: Boolean = true): String {
        if (isEmpty()) return this

        val out = StringBuilder()
        var i = 0
        var argIndex = 1

        var char: Char
        while (i < length) {
            var next = 1
            char = get(i)
            var isConsumed = false
            for (formatter in formatters) {
                if (char != formatter.arg[0]) continue
                if (regionMatches(thisOffset = i, other = formatter.arg, otherOffset = 0, length = formatter.arg.length)) {
                    out.append(formatter.formatter(argIndex, isXml))
                    isConsumed = true
                    if (formatter.isIndexed) argIndex++
                    next = formatter.arg.length
                    break
                }
            }
            if (!isConsumed) out.append(char)
            i += next
        }
        return out.toString()
    }

    companion object {
        private val transformerFactory: TransformerFactory by lazy { TransformerFactory.newInstance() }
        private val documentBuilder: DocumentBuilder by lazy { DocumentBuilderFactory.newInstance().newDocumentBuilder() }
        private val defaultOutputFolder = File("out")

        fun createDocument(): Document = documentBuilder.newDocument().apply { xmlStandalone = true }
        fun createTransformer(): Transformer = transformerFactory.newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "") //This prevents a Java bug that puts the first element on the same line as the xml declaration
        }

        fun generateFiles(
            resources: Collection<Resource>,
            vararg languages: Language,
            androidFolder: File = File(defaultOutputFolder, "android"),
            iosFolder: File = File(defaultOutputFolder, "ios"),
            formatters: List<StringFormatter> = StringFormatter.defaultFormatters,
            platforms: Array<Platform> = ALL,
            openFolder: Boolean = true
        ) {
            if (ANDROID in platforms) {
                androidFolder.mkdirs()
                for (lang in languages) {
                    AndroidResourceGenerator(androidFolder, lang, formatters, resources).generateFiles()
                }
                if (openFolder) openFolder(androidFolder)
            }
            if (IOS in platforms) {
                iosFolder.mkdirs()
                for (lang in languages) {
                    IosResourceGenerator(iosFolder, lang, formatters, resources).generateFiles()
                }
                if (openFolder) openFolder(iosFolder)
            }
        }

        private fun openFolder(folder: File) {
            try {
                val osName = System.getProperty("os.name")
                when {
                    osName.contains("Mac") -> Runtime.getRuntime().exec("open -R ${folder.canonicalPath}")
                    osName.contains("Windows") -> Runtime.getRuntime().exec("explorer ${folder.canonicalPath}")
                }
            } catch (e: Exception) {
                System.err.println("unable to open folder: $e")
            }
        }
    }
}
