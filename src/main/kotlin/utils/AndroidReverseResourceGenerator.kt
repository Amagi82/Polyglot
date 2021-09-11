package utils

import org.w3c.dom.*
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

class AndroidReverseResourceGenerator(sourceXML: File, private val name: String) {
    init {
        if (name.isBlank()) throw IllegalArgumentException("name cannot be blank")
        if (name[0].isUpperCase()) throw IllegalArgumentException("name must start with a lowercase letter")
        if (name.filter(Char::isLetterOrDigit) != name) throw IllegalArgumentException("name must contain only letters or numbers")
    }

    private val document: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(sourceXML)
    private val destFile = File("build/generated/src", "$name.kt").apply { delete(); createNewFile() }

    fun generate() {
        destFile.bufferedWriter().use { writer ->
            writer.appendLine(
                """
                import models.*
                import models.Platform.Companion.ANDROID_ONLY
                import utils.*
                
                fun $name() = listOf(
            """.trimIndent()
            )
            val elements = mutableListOf<Element>()
            listOf("string", "plurals", "string-array").forEach { tagName ->
                document.getElementsByTagName(tagName).forEach<Element> { elements.add(it) }
            }
            elements.sortBy { it.id }
            var prevPrefix: String? = null
            elements.forEach { element ->
                val id = element.id
                val prefix = id.takeWhile { it != '_' }
                if (prefix != prevPrefix) {
                    prevPrefix = prefix
                    writer.appendLine()
                    val comment = when (prefix) {
                        "btn" -> "Buttons"
                        "acc" -> "Accessibility hints"
                        "daily" -> "Daily Challenges"
                        "icp" -> "Individual Course Pages"
                        "rate" -> "Rate Brilliant"
                        else -> prefix.replaceFirstChar(Char::titlecaseChar)
                    }
                    writer.appendLine("\t// $comment")
                }
                writer.append("\t")
                when (element.tagName) {
                    "string" -> {
                        writer.append("string(id = \"$id\", English to \"${element.value}\", ")
                    }
                    "plurals" -> {
                        writer.appendLine("plural(id = \"$id\",")
                        writer.appendLine("\t\tquantities = Quantities(")

                        element.forEachElementNamed("item") { item ->
                            val quantity = item.attributes["quantity"]?.textContent ?: return@forEachElementNamed
                            writer.appendLine("\t\t\t$quantity = Str(English to \"${item.value}\"),")
                        }
                        writer.append("\t\t), ")
                    }
                    "string-array" -> {
                        writer.appendLine("stringArray(id = \"$id\",")
                        element.forEachElementNamed("item") { item ->
                            writer.appendLine("\t\tStr(English to \"${item.value}\"),")
                        }
                        writer.append("\t\t")
                    }
                    else -> throw IllegalArgumentException("unknown type")
                }
                writer.appendLine("platforms = ANDROID_ONLY),")
            }
            writer.appendLine(')')
        }
    }

    private val Element.value: String get() = textContent.unescaped()

    private val Node.id: String get() = attributes["name"]?.textContent ?: throw IllegalArgumentException("node $this missing id")

    private fun String.unescaped() = replace("\\'", "'").replace(pluralRegex, "%$1")

    operator fun NamedNodeMap.get(name: String): Node? = getNamedItem(name)

    @Suppress("UNCHECKED_CAST")
    private inline fun <T : Node> NodeList.forEach(action: (T) -> Unit) {
        for (element in this) action(element as T)
    }

    private operator fun NodeList.iterator() = object : Iterator<Node> {
        private var index = 0
        override fun hasNext(): Boolean = index < length
        override fun next(): Node = item(index++)
    }

    private inline fun Element.forEachElementNamed(tagName: String, action: (Element) -> Unit) {
        getElementsByTagName(tagName).forEach(action)
    }

    companion object {
        private val pluralRegex = Regex("%\\d\\$([sdf])")
    }
}
