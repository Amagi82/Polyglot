package generators

import locales.LocaleIsoCode
import org.w3c.dom.Document
import org.w3c.dom.Element
import project.*
import ui.resource.ResourceViewModel
import javax.xml.transform.Transformer
import java.io.File

/**
 * Generates Android resources for a given language in the specified folder
 */
class AndroidResourceGenerator(
    vm: ResourceViewModel,
    locale: LocaleIsoCode,
    formatters: List<StringFormatter>
) : ResourceGenerator(Platform.ANDROID, vm.project.value, locale, formatters) {
    private val valuesFolder = File(outputFolder, "values${if (locale == defaultLocale) "" else "-${locale.value}"}").also(File::mkdirs)
    private val document: Document = createDocument()
    private val resourceElement: Element = document.createElement("resources").also {
        if (locale == defaultLocale) it.setAttribute("xmlns:tools", "http://schemas.android.com/tools")
        document.appendChild(it)
    }

    init {
        addAll(vm)
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
    override fun addPlurals(id: ResourceId, res: Plural) {
        resourceElement.appendChild(document.createElement("plurals").apply {
            setAttribute("name", id.value)
            res.items.forEach { (quantity, text) ->
                appendChild(document.createElement("item").apply {
                    setAttribute("quantity", quantity.label)
                    appendChild(document.createTextNode(text.sanitized()))
                })
            }
        })
    }

    override fun addString(id: ResourceId, res: Str) {
        val txt = res.text.sanitized()
        if (txt.isBlank()) return
        /** <string name="dragon">Trogdor the Burninator</string> */
        resourceElement.appendChild(document.createElement("string").apply {
            setAttribute("name", id.value)
            appendChild(document.createTextNode(txt))
        })
    }

    /**
     * <string-array name="country_names">
     *      <item>France</item>
     *      <item>Germany</item>
     * </string-array>
     */
    override fun addStringArray(id: ResourceId, res: StringArray) {
        resourceElement.appendChild(document.createElement("string-array").apply {
            setAttribute("name", id.value)
            for (text in res.items) {
                appendChild(document, "item", text.sanitized())
            }
        })
    }

    companion object {
        private val transformer: Transformer by lazy { createTransformer() }
    }
}
