package generators

import locales.LocaleIsoCode
import project.Platform
import project.Quantity
import org.w3c.dom.Document
import org.w3c.dom.Element
import sqldelight.ArrayLocalizations
import sqldelight.PluralLocalizations
import sqldelight.Project
import sqldelight.StringLocalizations
import utils.extensions.quantity
import javax.xml.transform.Transformer
import java.io.File

/**
 * Generates Android resources for a given language in the specified folder
 */
class AndroidResourceGenerator(
    private val project: Project,
    locale: LocaleIsoCode,
    override val formatters: List<StringFormatter>,
    strings: List<StringLocalizations>,
    plurals: List<PluralLocalizations>,
    arrays: List<ArrayLocalizations>
) : ResourceGenerator() {
    override val platform: Platform = Platform.ANDROID
    private val valuesFolder = File(project.iosOutputUrl, "values${if (locale == project.defaultLocale) "" else "-$locale"}").also(File::mkdirs)
    private val document: Document = createDocument()
    private val resourceElement: Element = document.createElement("resources").also {
        if (locale == project.defaultLocale) it.setAttribute("xmlns:tools", "http://schemas.android.com/tools")
        document.appendChild(it)
    }

    init {
        strings.forEach(::addString)
        plurals.forEach(::addPlurals)
        arrays.forEach(::addStringArray)
    }

    override fun generateFiles() {
        transformer.transform(document, valuesFolder, "strings.xml")
    }

    /**
     * <plurals name="numberOfSongsAvailable">
     *     <item quantity="one">Znaleziono %d piosenkę.</item>
     *     <item quantity="few">Znaleziono %d piosenki.</item>
     *     <item quantity="other">Znaleziono %d piosenek.</item>
     * </plurals>
     */
    override fun addPlurals(res: PluralLocalizations) {
        resourceElement.appendChild(document.createElement("plurals").apply {
            setAttribute("name", res.id)
            Quantity.values().forEach { quantity ->
                val text = res.quantity(quantity) ?: return@forEach
                appendChild(document.createElement("item").apply {
                    setAttribute("quantity", quantity.label)
                    appendChild(document.createTextNode(text.sanitized()))
                })
            }
        })
    }

    override fun addString(res: StringLocalizations) {
        val txt = res.text.sanitized()
        if (txt.isBlank()) return
        /** <string name="dragon">Trogdor the Burninator</string> */
        resourceElement.appendChild(document.createElement("string").apply {
            setAttribute("name", res.id)
            appendChild(document.createTextNode(txt))
        })
    }

    /**
     * <string-array name="country_names">
     *      <item>France</item>
     *      <item>Germany</item>
     * </string-array>
     */
    override fun addStringArray(res: ArrayLocalizations) {
        resourceElement.appendChild(document.createElement("string-array").apply {
            setAttribute("name", res.id)
            for (text in res.array) {
                appendChild(document, "item", text.sanitized())
            }
        })
    }

    companion object {
        private val transformer: Transformer by lazy { createTransformer() }
    }
}
