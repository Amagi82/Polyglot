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
    abstract fun generateFiles()
    protected abstract fun add(res: Resource)
    protected abstract val platform: Platform

    private val formatters: List<StringFormatter> = formatters.filter { it.platforms?.contains(platform) != false }

    /**
     * Should be called in the init block of the resource generator
     * */
    protected fun addAll(resources: Collection<Resource>) {
        for (res in resources) {
            if (res.platforms?.contains(platform) != false && !res.shouldSkip(locale)) add(res)
        }
    }

    protected fun Element.appendChild(document: Document, tagName: String, textNode: String) {
        appendChild(document.createElement(tagName).also { it.appendChild(document.createTextNode(textNode)) })
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
                if (i + formatter.arg.length >= length) continue
                if (substring(i, i + formatter.arg.length) == formatter.arg) {
                    out.append(formatter.formatter(i, isXml))
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
        private fun defaultRootFolder() = File("${System.getProperty("user.home")}${File.separator}Desktop${File.separator}localization").also(File::mkdirs)

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
            rootFolder: File = defaultRootFolder(),
            platforms: List<Platform> = listOf(Platform.Android, Platform.iOS),
            formatters: List<StringFormatter> = StringFormatter.defaultFormatters,
            generator: (
                rootFolder: File,
                platform: Platform,
                localeIsoCode: LocaleIsoCode,
                formatters: List<StringFormatter>
            ) -> ResourceGenerator? = { folder, platform, localeIsoCode, fmt ->
                when (platform) {
                    Platform.Android -> AndroidResourceGenerator(File(folder, platform.name).also(File::mkdirs), localeIsoCode, fmt, resources)
                    Platform.iOS -> IosResourceGenerator(File(folder, platform.name).also(File::mkdirs), localeIsoCode, fmt, resources)
                    else -> null
                }
            }
        ) {
            Locale.default = defaultLanguage
            val locales = resources.flatMapTo(mutableSetOf(), Resource::locales)
            for (locale in locales) {
                for (platform in platforms) {
                    generator(rootFolder, platform, locale, formatters)?.generateFiles()
                }
            }
            openFolder(rootFolder)
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
