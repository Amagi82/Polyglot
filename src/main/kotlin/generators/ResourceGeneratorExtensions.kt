package generators

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun String.sanitized(formatters: List<StringFormatter>, isXml: Boolean = true): String {
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

fun Element.appendElement(tagName: String, apply: Element.() -> Unit) {
    appendElement(tagName).apply(apply)
}

fun Element.appendElement(tagName: String): Element = ownerDocument.createElement(tagName).also(::appendChild)

fun Element.appendTextNode(textNode: String) {
    ownerDocument.createTextNode(textNode).also(::appendChild)
}

fun Document.appendElement(tagName: String): Element = createElement(tagName).also(::appendChild)

fun File.createChildFile(filename: String) = File(this, filename).also(File::createNewFile)

fun Transformer.transform(document: Document, file: File) = transform(DOMSource(document), StreamResult(file))

fun createDocument(): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().apply { xmlStandalone = true }
fun createTransformer(apply: Transformer.() -> Unit = {}): Transformer = TransformerFactory.newInstance().newTransformer().apply {
    setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    setOutputProperty(OutputKeys.INDENT, "yes")
    setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
    setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "") //This prevents a Java bug that puts the first element on the same line as the xml declaration
    apply(apply)
}
