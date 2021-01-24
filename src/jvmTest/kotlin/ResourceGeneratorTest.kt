import org.junit.Test

internal class ResourceGeneratorTest {

    @Test
    fun generateFiles() {
        val resources = createSampleResources()
        ResourceGenerator.generateFiles(resources, defaultLanguage = "en")
    }

    private fun createSampleResources(): List<Resource> {
        val es = "es"
        val fr = "fr"
        val de = "de"
        return listOf(
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
    }
}
