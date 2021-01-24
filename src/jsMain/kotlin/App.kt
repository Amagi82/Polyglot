import react.*
import react.dom.*
import kotlinext.js.*
import kotlinx.coroutines.*
import kotlinx.css.*
import kotlinx.css.properties.border
import kotlinx.html.*
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import styled.*

private val scope = MainScope()

val App = functionalComponent<RProps> { _ ->
    val (languages, setLanguages) = useState(emptyMap<LanguageIsoCode, String>())
    val (regions, setRegions) = useState(emptyMap<RegionIsoCode, String>())
    val (languageRegions, setLanguageRegions) = useState(emptyMap<LanguageIsoCode, List<RegionIsoCode>>())
    val (locale, setLocale) = useState(Locale(language = Language(isoCode = "en", name = "English")))

    useEffect(dependencies = listOf()) {
        scope.launch {
            setLanguages(getLanguages())
            setRegions(getRegions())
            setLanguageRegions(getLanguageRegions())
        }
    }

    styledHtml {
        css {
            margin(16.pt)
        }

        h1 {
            +"Polyglot Locales"
        }

        p {
            +"Locale: ${locale.name}"
        }

        form {
            val (languageIsoCode, setLanguageIsoCode) = useState(locale.language.isoCode)
            val (regionIsoCode, setRegionIsoCode) = useState(locale.region?.isoCode.orEmpty())

            label { +"Change Locale: " }
            select {
                attrs.name = "Locale"
                languages.forEach { (languageIsoCode, languageName) ->
                    option {
                        attrs.value = languageIsoCode
                        +"$languageName ($languageIsoCode)"
                    }
                }
                attrs.onChangeFunction = { event ->
                    val target = (event.target as HTMLSelectElement)
                    val isoCode = target.value
                    if (isoCode != languageIsoCode) {
                        setRegionIsoCode("")
                    }
                    setLanguageIsoCode(isoCode)
                    val newLocale = Locale(
                        language = Language(isoCode = isoCode, name = languages[isoCode].orEmpty()),
                        region = if (regionIsoCode.isEmpty()) null else Region(isoCode = regionIsoCode, name = regions[regionIsoCode].orEmpty())
                    )
                    setLocale(newLocale)
                }
            }

            select {
                attrs.name = "Locale"
                option(content = "")
                languageRegions[languageIsoCode]?.sortedBy { regions[it] ?: it }?.forEach { regionIsoCode ->
                    option {
                        attrs.value = regionIsoCode
                        +"${regions[regionIsoCode].orEmpty()} ($regionIsoCode)"
                    }
                }
                attrs.onChangeFunction = { event ->
                    val target = (event.target as HTMLSelectElement)
                    val isoCode = target.value
                    setRegionIsoCode(isoCode)
                    val newLocale = Locale(
                        language = Language(isoCode = languageIsoCode, name = languages[languageIsoCode].orEmpty()),
                        region = if (isoCode.isEmpty()) null else Region(isoCode = isoCode, name = regions[isoCode].orEmpty())
                    )
                    setLocale(newLocale)
                }
            }
        }
        child(
            functionalComponent<InputProps> { props ->
                val (languageIsoCode, setLanguageIsoCode) = useState("")
                val (languageName, setLanguageName) = useState("")
                val (regionIsoCode, setRegionIsoCode) = useState("")
                val (regionName, setRegionName) = useState("")

                styledForm {
                    css{
                        marginTop = 24.pt
                        width = LinearDimension.fitContent
                        border(width = 2.pt, style = BorderStyle.solid, color = Color.darkGreen, borderRadius = 4.pt)
                        padding(left = 16.pt, right = 16.pt, bottom = 16.pt)
                    }

                    styledH3 {
                        css{
                           textAlign = TextAlign.center
                        }
                        +"Add Locale"
                    }

                    attrs.onSubmitFunction = { event ->
                        if (languageIsoCode.isNotEmpty() && languageName.isNotEmpty() && regionIsoCode.isEmpty() == regionName.isEmpty()) {
                            event.preventDefault()
                            val newLocale = Locale(
                                language = Language(isoCode = languageIsoCode, name = languageName),
                                region = if (regionIsoCode.isEmpty()) null else Region(isoCode = regionIsoCode, name = regionName)
                            )
                            setLanguageIsoCode("")
                            setLanguageName("")
                            setRegionIsoCode("")
                            setRegionName("")
                            props.onSubmit(newLocale)
                        }
                    }

                    p {
                        +"Language:"
                    }
                    label { +"ISO Code: " }
                    input(InputType.text) {
                        attrs.list = "languageIsoCode"
                        attrs.name = "Language ISO Code"
                        attrs.autoComplete = false
                        attrs.onChangeFunction = { event ->
                            val value = (event.target as HTMLInputElement).value
                            setLanguageIsoCode(value)
                            languages[value]?.let(setLanguageName)
                        }
                        attrs.value = languageIsoCode
                    }
                    dataList {
                        attrs.id = "languageIsoCode"
                        languages.keys.forEach {
                            option(content = it)
                        }
                    }

                    styledLabel {
                        css{
                            marginTop = 8.pt
                        }
                        +"  Name: " }
                    input(InputType.text) {
                        attrs.list = "languageName"
                        attrs.name = "Language Name"
                        attrs.autoComplete = false
                        attrs.onChangeFunction = { event ->
                            val value = (event.target as HTMLInputElement).value
                            setLanguageName(value)
                            languages.entries.find { it.value == value }?.key?.let(setLanguageIsoCode)
                        }
                        attrs.value = languageName
                    }
                    dataList {
                        attrs.id = "languageName"
                        languages.values.forEach {
                            option(content = it)
                        }
                    }

                    p { +"Region (optional):" }
                    label { +"ISO Code: " }
                    input(InputType.text) {
                        attrs.list = "regionIsoCode"
                        attrs.name = "Region ISO Code"
                        attrs.autoComplete = false
                        attrs.onChangeFunction = { event ->
                            val value = (event.target as HTMLInputElement).value
                            setRegionIsoCode(value)
                            regions[value]?.let(setRegionName)
                        }
                        attrs.value = regionIsoCode
                    }
                    dataList {
                        attrs.id = "regionIsoCode"
                        regions.keys.forEach {
                            option(content = it)
                        }
                    }

                    label { +"  Name: " }
                    input(InputType.text) {
                        attrs.list = "regionName"
                        attrs.name = "Region Name"
                        attrs.autoComplete = false
                        attrs.onChangeFunction = { event ->
                            val value = (event.target as HTMLInputElement).value
                            setRegionName(value)
                            regions.entries.find { it.value == value }?.key?.let(setRegionIsoCode)
                        }
                        attrs.value = regionName
                    }
                    dataList {
                        attrs.id = "regionName"
                        regions.values.forEach {
                            option(content = it)
                        }
                    }
                    br {  }
                    styledButton(type = ButtonType.submit) {
                        css{
                            color = Color.white
                            backgroundColor = Color.darkGreen
                            marginTop = 16.pt
                            padding(horizontal = 8.pt, vertical = 4.pt)
                        }
                        +"ADD"
                    }
                }
            },
            props = jsObject {
                onSubmit = { locale ->
                    scope.launch {
                        locale.language.let {
                            if (!languages.containsKey(it.isoCode)) {
                                setLanguages(languages.plus(it.isoCode to it.name))
                            }
                        }
                        locale.region?.let { region ->
                            if (!regions.containsKey(region.isoCode)) {
                                setRegions(regions.plus(region.isoCode to region.name))
                            }
                            val langRegions = languageRegions[locale.language.isoCode].orEmpty().plus(region.isoCode).distinct()
                            setLanguageRegions(languageRegions.plus(locale.language.isoCode to langRegions))
                        }
                        setLocale(locale)
                    }
                }
            }
        )
    }
}

external interface InputProps : RProps {
    var onSubmit: (Locale) -> Unit
}
