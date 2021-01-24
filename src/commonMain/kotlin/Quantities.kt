/**
 * Different languages have different grammatical rules for quantity.
 * e.g. English, 1 book, 2 books
 *
 * Not all languages use all rules, and some languages, like Chinese, only use "other"
 *
 * More information:
 * https://developer.android.com/guide/topics/resources/string-resource#Plurals
 */
enum class Quantities(val label: String, val isRequired: Boolean = false) {
    ZERO("zero"),
    ONE("one"),
    TWO("two"),
    FEW("few"),
    MANY("many"),
    OTHER("other", isRequired = true)
}
