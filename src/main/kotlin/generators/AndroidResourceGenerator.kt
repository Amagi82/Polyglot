package generators

import locales.LocaleIsoCode
import resources.Platform
import resources.Plural
import resources.Quantity
import resources.Resource
import resources.Str
import resources.StringArray
import locales.isDefault
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.transform.Transformer
import java.io.File

/**
 * Generates Android resources for a given language in the specified folder
 */
class AndroidResourceGenerator(androidFolder: File, locale: LocaleIsoCode, formatters: List<StringFormatter>, resources: Collection<Resource>) :
    ResourceGenerator(locale, formatters) {
    override val platform: Platform = Platform.Android
    private val valuesFolder = File(androidFolder, "values${if (locale.isDefault) "" else "-$locale"}").also(File::mkdirs)
    private val document: Document = createDocument()
    private val resourceElement: Element = document.createElement("resources").also {
        if (locale.isDefault) it.setAttribute("xmlns:tools", "http://schemas.android.com/tools")
        document.appendChild(it)
    }

    init {
        addAll(resources)
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
    override fun addPlurals(res: Plural) {
        resourceElement.appendChild(document.createElement("plurals").apply {
            setAttribute("name", res.id)
            Quantity.values().forEach { quantity ->
                val item = res.quantity(quantity) ?: return@forEach
                val text = item.get(locale, isRequired = quantity.isRequired) ?: return@forEach
                appendChild(document.createElement("item").apply {
                    setAttribute("quantity", quantity.label)
                    appendChild(document.createTextNode(text.sanitized()))
                })
            }
        })
    }

    override fun addString(res: Str) {
        val txt = res.localizations.getRequired(locale).sanitized()
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
    override fun addStringArray(res: StringArray) {
        resourceElement.appendChild(document.createElement("string-array").apply {
            setAttribute("name", res.id)
            if (!res.items.first().hasTranslations) setAttribute("tools:ignore", "MissingTranslation")
            for (item in res.items) {
                appendChild(document, "item", item.getRequired(locale).sanitized())
            }
        })
    }

    companion object {
        private val transformer: Transformer by lazy { createTransformer() }
    }
}
