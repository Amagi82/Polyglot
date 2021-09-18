package generators

import locales.LanguageIsoCode
import locales.Locale
import locales.LocaleIsoCode
import project.Platform
import project.Plural
import project.Resource
import project.Str
import project.StringArray
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.awt.Desktop
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class ResourceGenerator(protected val locale: LocaleIsoCode, formatters: List<StringFormatter>) {
    protected abstract val platform: Platform
    protected abstract fun addString(res: Str)
    protected abstract fun addStringArray(res: StringArray)
    protected abstract fun addPlurals(res: Plural)
    abstract fun generateFiles()

    private val formatters: List<StringFormatter> = formatters.filter { it.platforms?.contains(platform) != false }

    /**
     * Should be called in the init block of the resource generator
     * */
    protected fun addAll(resources: Collection<Resource>) {
        for (res in resources) {
            if (res.platforms.contains(platform) && !res.shouldSkip(locale)) {
                when (res) {
                    is Str -> addString(res)
                    is Plural -> addPlurals(res)
                    is StringArray -> addStringArray(res)
                }
            }
        }
    }

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
            defaultLanguage: LanguageIsoCode,
            platforms: List<Platform> = Platform.ALL,
            formatters: List<StringFormatter> = StringFormatter.defaultFormatters,
            outputFile: (Platform) -> File = { File(defaultOutputFolder, it.name) },
            openFolder: Boolean = true,
            generator: (
                platform: Platform,
                localeIsoCode: LocaleIsoCode,
                formatters: List<StringFormatter>
            ) -> ResourceGenerator? = { platform, localeIsoCode, fmt ->
                val folder = outputFile(platform).also(File::mkdirs)
                when (platform) {
                    Platform.ANDROID -> AndroidResourceGenerator(folder, localeIsoCode, fmt, resources)
                    Platform.IOS -> IosResourceGenerator(folder, localeIsoCode, fmt, resources)
                }
            }
        ) {
            Locale.default = defaultLanguage
            val locales = resources.flatMapTo(mutableSetOf(), Resource::locales)
            for (locale in locales) {
                for (platform in platforms) {
                    generator(platform, locale, formatters)?.generateFiles()
                    if (openFolder) openFolder(outputFile(platform))
                }
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
