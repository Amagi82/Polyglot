/*
 * Copyright (C) 2018 Jim Pekarek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import models.*
import utils.*


fun main(args: Array<String>) {
    val resources = createSampleResources()
    ResourceGenerator.generateFiles(resources, English, Spanish, French, German)
}


private fun createSampleResources() = listOf(
        string(id = "cat",
                en = "cat",
                es = "gato",
                fr = "chat",
                de = "Katze"),
        string(id = "dog",
                platform = Platform.ANDROID,
                en = "dog",
                es = "perro",
                fr = "chien",
                de = "Hund"),
        string(id = "bird",
                platform = Platform.IOS,
                en = "bird",
                es = "ave",
                fr = "oiseau",
                de = "Vogel"),
        plural(id = "number_of_cats",
                plural = quantities(
                        one = Str(
                                English to "%d cat",
                                Spanish to "%d gato",
                                French to "%d chat",
                                German to "%d Katze"),
                        other = Str(
                                English to "%d cats",
                                Spanish to "%d gatos",
                                French to "%d chats",
                                German to "%d Katzen"))),
        plural(id = "number_of_dogs",
                platform = Platform.ANDROID,
                plural = quantities(
                        one = Str(
                                English to "%d dog",
                                Spanish to "%d perro",
                                French to "%d chien",
                                German to "%d Hund"),
                        other = Str(
                                English to "%d dogs",
                                Spanish to "%d perros",
                                French to "%d chiens",
                                German to "%d Hunde"))),
        plural(id = "number_of_birds",
                platform = Platform.IOS,
                plural = quantities(
                        one = Str(
                                English to "%d bird",
                                Spanish to "%d ave",
                                French to "%d oiseau",
                                German to "%d Vogel"),
                        other = Str(
                                English to "%d birds",
                                Spanish to "%d aves",
                                French to "%d oiseaux",
                                German to "%d Vögel"))),
        stringArray(id = "cat_species",
                items = items(
                        Str(
                                English to "lion",
                                Spanish to "león",
                                French to "lion",
                                German to "Löwe"),
                        Str(
                                English to "tiger",
                                Spanish to "tigre",
                                French to "tigre",
                                German to "Tiger"),
                        Str(
                                English to "cheetah",
                                Spanish to "leopardo cazador",
                                French to "guépard",
                                German to "Gepard"))),
        stringArray(id = "dog_species",
                platform = Platform.ANDROID,
                items = items(
                        Str(
                                English to "wolf",
                                Spanish to "lobo",
                                French to "loup",
                                German to "Wolf"),
                        Str(
                                English to "fox",
                                Spanish to "zorro",
                                French to "renard",
                                German to "Fuchs"),
                        Str(
                                English to "coyote",
                                Spanish to "coyote",
                                French to "coyote",
                                German to "Kojote"))),
        stringArray(id = "bird_species",
                platform = Platform.IOS,
                items = items(
                        Str(
                                English to "hawk",
                                Spanish to "halcón",
                                French to "faucon",
                                German to "Falke"),
                        Str(
                                English to "raven",
                                Spanish to "cuervo",
                                French to "corbeau",
                                German to "Rabe"),
                        Str(
                                English to "parrot",
                                Spanish to "loro",
                                French to "perroquet",
                                German to "Papagei"))))