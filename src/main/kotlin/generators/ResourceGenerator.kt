package generators

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import locales.LocaleIsoCode
import org.w3c.dom.Document
import org.w3c.dom.Element
import project.*
import ui.resource.*
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class ResourceGenerator(
    private val platform: Platform,
    project: Project,
    private val locale: LocaleIsoCode,
    private val formatters: List<StringFormatter>
) {
    protected val defaultLocale = project.defaultLocale
    protected val outputFolder = platform.outputFolder(project)
    protected abstract fun addString(id: ResourceId, res: Str)
    protected abstract fun addStringArray(id: ResourceId, res: StringArray)
    protected abstract fun addPlurals(id: ResourceId, res: Plural)
    abstract fun generateFiles()

    protected fun addAll(vm: ResourceViewModel) {
        addAll(vm.strings, ::addString)
        addAll(vm.plurals, ::addPlurals)
        addAll(vm.arrays, ::addStringArray)
    }

    private fun <M : Metadata, R : Resource<M>> addAll(vm: ResourceTypeViewModel<M, R>, add: (ResourceId, R) -> Unit) {
        vm.resourceMetadata.value.forEach { (resId, metadata) ->
            if (platform !in metadata.platforms) return@forEach
            val resource = vm.resourcesByLocale.value[locale]?.get(resId) ?: return@forEach
            add(resId, resource)
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

        fun createDocument(): Document = documentBuilder.newDocument().apply { xmlStandalone = true }
        fun createTransformer(): Transformer = transformerFactory.newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "") //This prevents a Java bug that puts the first element on the same line as the xml declaration
        }

        suspend fun generateFiles(
            vm: ResourceViewModel,
            platforms: List<Platform> = Platform.ALL,
            formatters: List<StringFormatter> = StringFormatter.defaultFormatters
        ) = withContext(Dispatchers.IO) {
            val project = vm.project.value
            val androidFormatters = formatters.filter { Platform.ANDROID in it.platforms }
            val iosFormatters = formatters.filter { Platform.IOS in it.platforms }
            platforms.forEach {
                val folder = it.outputFolder(project)
                if (folder.exists()) folder.deleteRecursively()
                folder.mkdirs()
            }
            project.locales.forEach { localeIsoCode ->
                for (platform in platforms) {
                    when (platform) {
                        Platform.ANDROID -> AndroidResourceGenerator(vm, localeIsoCode, androidFormatters)
                        Platform.IOS -> IosResourceGenerator(vm, localeIsoCode, iosFormatters)
                    }.generateFiles()
                }
            }
        }
    }
}
