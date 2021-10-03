package generators

import data.PolyglotDatabase
import project.Platform
import org.w3c.dom.Document
import org.w3c.dom.Element
import sqldelight.ArrayLocalizations
import sqldelight.PluralLocalizations
import sqldelight.Project
import sqldelight.StringLocalizations
import java.awt.Desktop
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class ResourceGenerator {
    protected abstract val platform: Platform
    protected abstract fun addString(res: StringLocalizations)
    protected abstract fun addStringArray(res: ArrayLocalizations)
    protected abstract fun addPlurals(res: PluralLocalizations)
    protected abstract val formatters: List<StringFormatter>
    abstract fun generateFiles()

    protected fun Element.appendChild(document: Document, tagName: String, textNode: String) {
        appendChild(document.createElement(tagName).apply { appendChild(document.createTextNode(textNode)) })
    }

    protected fun File.createChildFile(filename: String) = File(this, filename).also(File::createNewFile)

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

        fun createDocument(): Document = documentBuilder.newDocument().apply { xmlStandalone = true }
        fun createTransformer(): Transformer = transformerFactory.newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "") //This prevents a Java bug that puts the first element on the same line as the xml declaration
        }

        fun generateFiles(
            project: Project,
            db: PolyglotDatabase,
            platforms: List<Platform> = Platform.ALL,
            formatters: List<StringFormatter> = StringFormatter.defaultFormatters,
            openFolders: Boolean = true,
        ) {
            val androidFormatters = formatters.filter { Platform.ANDROID in it.platforms }
            val iosFormatters = formatters.filter { Platform.IOS in it.platforms }
            for (localeIsoCode in project.locales) {
                val strings = db.stringLocalizationsQueries.selectAllWithLocale(localeIsoCode, project.name).executeAsList()
                val plurals = db.pluralLocalizationsQueries.selectAllWithLocale(localeIsoCode, project.name).executeAsList()
                val arrays = db.arrayLocalizationsQueries.selectAllWithLocale(localeIsoCode, project.name).executeAsList()
                for (platform in platforms) {
                    when (platform) {
                        Platform.ANDROID -> AndroidResourceGenerator(project, localeIsoCode, androidFormatters, strings, plurals, arrays)
                        Platform.IOS -> IosResourceGenerator(project, localeIsoCode, iosFormatters, strings, plurals, arrays)
                    }.generateFiles()
                }
            }
            if (openFolders) {
                project.androidOutputUrl.let(::File).let(::openFolder)
                project.iosOutputUrl.let(::File).let(::openFolder)
            }
        }

        private fun openFolder(folder: File) {
            try {
                Desktop.getDesktop().open(folder)
            } catch (e: Exception) {
                System.err.println("unable to open folder: $e")
            }
        }
    }
}
