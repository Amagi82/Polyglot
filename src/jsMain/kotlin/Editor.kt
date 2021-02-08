import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.css.properties.border
import kotlinx.css.properties.borderBottom
import kotlinx.css.properties.borderLeft
import kotlinx.css.properties.borderTop
import kotlinx.html.TBODY
import kotlinx.html.TD
import react.*
import react.dom.*
import styled.*

private val scope = MainScope()

val Editor = functionalComponent<RProps> { _ ->
    val locales = mutableListOf(
        Locale(Language(isoCode = "en", name = "English"), null),
        Locale(Language(isoCode = "en", name = "English"), Region(isoCode = "GB", name = "British")),
        Locale(Language(isoCode = "es", name = "Spanish"), null),
        Locale(Language(isoCode = "fr", name = "French"), null),
        Locale(Language(isoCode = "de", name = "German"), null)
    ).apply { sort() }
    val es = "es"
    val fr = "fr"
    val de = "de"
    val resources = mutableListOf(
        string(
            id = "color",
            default = "color",
            "en_GB" to "colour",
            es to "color",
            fr to "couleur",
            de to "farbe"
        ),
        string(
            id = "cat",
            default = "cat",
            fr to "chat",
            de to "Katze"
        ),
        string(
            id = "dog",
            default = "dog",
            es to "perro",
            fr to "chien",
            de to "Hund",
            platforms = Platform.AndroidOnly,
        ),
        string(
            id = "bird",
            default = "bird",
            es to "ave",
            fr to "oiseau",
            de to "Vogel",
            platforms = Platform.iosOnly,
        ),
        string(
            id = "lizard",
            default = "%s lizard",
            es to "%s lagartija",
            fr to "%s lézard",
            de to "%s Eidechse",
        ),
        plural(
            id = "number_of_cats",
            one = Localizations(
                default = "%d cat",
                es to "%d gato",
                fr to "%d chat",
                de to "%d Katze"
            ),
            other = Localizations(
                default = "%d cats",
                es to "%d gatos",
                fr to "%d chats",
                de to "%d Katzen"
            )
        ),
        plural(
            id = "number_of_dogs",
            platforms = Platform.AndroidOnly,
            one = Localizations(
                default = "%d dog",
                es to "%d perro",
                fr to "%d chien",
                de to "%d Hund"
            ),
            other = Localizations(
                default = "%d dogs",
                es to "%d perros",
                fr to "%d chiens",
                de to "%d Hunde"
            )
        ),
        plural(
            id = "number_of_birds",
            one = Localizations(
                default = "%d bird",
                es to "%d ave",
                fr to "%d oiseau",
                de to "%d Vogel"
            ),
            other = Localizations(
                default = "%d birds",
                es to "%d aves",
                fr to "%d oiseaux",
                de to "%d Vögel"
            ),
            platforms = Platform.iosOnly
        ),
        stringArray(
            id = "cat_species",
            Localizations(
                default = "lion",
                es to "león",
                fr to "lion",
                de to "Löwe"
            ),
            Localizations(
                default = "tiger",
                es to "tigre",
                fr to "tigre",
                de to "Tiger"
            ),
            Localizations(
                default = "cheetah",
                es to "leopardo cazador",
                fr to "guépard",
                de to "Gepard"
            )
        ),
        stringArray(
            id = "dog_species",
            Localizations(
                default = "wolf",
                es to "lobo",
                fr to "loup",
                de to "Wolf"
            ),
            Localizations(
                default = "fox",
                es to "zorro",
                fr to "renard",
                de to "Fuchs"
            ),
            Localizations(
                default = "coyote",
                es to "coyote",
                fr to "coyote",
                de to "Kojote"
            ),
            platforms = Platform.AndroidOnly
        ),
        stringArray(
            id = "bird_species",
            Localizations(
                default = "hawk",
                es to "halcón",
                fr to "faucon",
                de to "Falke"
            ),
            Localizations(
                default = "raven",
                es to "cuervo",
                fr to "corbeau",
                de to "Rabe"
            ),
            Localizations(
                default = "parrot",
                es to "loro",
                fr to "perroquet",
                de to "Papagei"
            ),
            platforms = Platform.iosOnly
        )
    )
//    val (resources, setResources) = useState(emptyList<Resource>())
//    val (locales, setLocales) = useState(emptyList<Locale>())
//
//    useEffect(dependencies = listOf()) {
//        scope.launch {
//            setResources(getResources())
//            setLocales(getLocales())
//        }
//    }

    styledDiv {
        css {
            margin(16.pt)
        }

        h1 {
            +"Polyglot Editor"
        }

        styledTable {
            css {
                width = LinearDimension.fillAvailable
//                border(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            tbody {
                tr {
                    th {}
                    th { +"Name" }
                    th { +"id" }
                    th { +"description" }
                    th { +"tags" }
                    th { img(src = "$endpoint/camera.png") {} }
                    th { img(src = "$endpoint/chevron-down.png") {} }
                }
                resources.forEach { res ->
                    println(res)
                    cellTitleRow(res)
                    locales.forEach { locale ->
                        when (res) {
                            is Resource.Str -> cellRow(res, locale)
                            is Resource.Plural -> cellRows(res, locale)
                            is Resource.StringArray -> cellRows(res, locale)
                        }
                    }
                }
            }
        }
    }
}

private fun RDOMBuilder<TBODY>.cellTitleRow(res: Resource) {
    styledTr {
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            res.platforms?.forEach {
                img(src = "$endpoint/${it.iconUrl}") {}
            }
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            +res.name
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            +res.id
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            +res.description
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            +res.tags.joinToString()
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            img(src = res.imageUrl) {
                attrs {
                    width = "24pt"
                    height = "24pt"
                }
            }
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
                textAlign = TextAlign.right
            }
            img(src = "$endpoint/dots-vertical.png") {}
        }
    }
}

private fun RDOMBuilder<TBODY>.cellRow(res: Resource.Str, locale: Locale) {
    tr {
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            +localeName(locale)
        }
        styledTd {
            attrs.colSpan = "5"
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            +res.localizations.get(locale.isoCode).orEmpty()
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
                textAlign = TextAlign.right
            }
            img(src = "$endpoint/dots-vertical.png") {}
        }
    }
}

private fun RDOMBuilder<TBODY>.cellRows(res: Resource.Plural, locale: Locale) {
    Quantities.values().forEach { quantity ->
        cellRow(res, quantity, locale)
    }
}

private fun RDOMBuilder<TBODY>.cellRow(res: Resource.Plural, quantity: Quantities, locale: Locale) {
    tr {
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            if (quantity == Quantities.ZERO) +localeName(locale)
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            +quantity.label
        }
        styledTd {
            attrs.colSpan = "4"
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
            }
            +res.quantity(quantity)?.get(locale.isoCode).orEmpty()
        }
        styledTd {
            css {
                padding(4.pt)
                borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
                textAlign = TextAlign.right
            }
            img(src = "$endpoint/dots-vertical.png") {}
        }
    }
}


private fun RDOMBuilder<TBODY>.cellRows(res: Resource.StringArray, locale: Locale) {
    val items = res.items.mapNotNull { it.get(locale.isoCode) }.let { if (it.isEmpty()) listOf("") else it }
    items.forEach { item ->
        tr {
            styledTd {
                css {
                    padding(4.pt)
                    borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
                }
                if (item == items.first()) +localeName(locale)
            }
            styledTd {
                attrs.colSpan = "5"
                css {
                    padding(4.pt)
                    borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
                }
                +item
            }
            styledTd {
                css {
                    padding(4.pt)
                    borderBottom(1.pt, BorderStyle.solid, Color.darkGreen)
                    textAlign = TextAlign.right
                }
                img(src = "$endpoint/dots-vertical.png") {}
            }
        }
    }
}

private fun localeName(locale: Locale) = if (locale.region == null) locale.language.name else "    ${locale.region.name}"


fun string(
    id: String,
    default: String,
    vararg translations: Pair<LocaleIsoCode, String>,
    platforms: List<Platform>? = null,
) = Resource.Str(
    id = id,
    name = id.capitalize(),
    platforms = platforms,
    localizations = Localizations(default, *translations)
)

fun plural(
    id: String,
    zero: Localizations? = null,
    one: Localizations?,
    two: Localizations? = null,
    few: Localizations? = null,
    many: Localizations? = null,
    other: Localizations,
    platforms: List<Platform>? = null
) = Resource.Plural(id = id, name = id.capitalize(), platforms = platforms, zero = zero, one = one, two = two, few = few, many = many, other = other)

fun stringArray(
    id: String,
    vararg items: Localizations,
    platforms: List<Platform>? = null
) = Resource.StringArray(id = id, name = id.capitalize(), platforms = platforms, items = items.toList())

