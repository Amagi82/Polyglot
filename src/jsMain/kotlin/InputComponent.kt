import react.*
import react.dom.*
import kotlinx.html.js.*
import kotlinx.html.InputType
import org.w3c.dom.events.Event
import org.w3c.dom.HTMLInputElement

//external interface InputProps : RProps {
//    var onSubmit: (Locale) -> Unit
//}

//val InputComponent = functionalComponent<InputProps> { props ->
//    val (languageIsoCode, setLanguageIsoCode) = useState("")
//    val (languageName, setLanguageName) = useState("")
//    val (regionIsoCode, setRegionIsoCode) = useState("")
//    val (regionName, setRegionName) = useState("")
//
//    val submitHandler: (Event) -> Unit = {
//        it.preventDefault()
//        val locale = Locale(
//            language = Language(isoCode = languageIsoCode, name = languageName),
//            region = if(regionIsoCode.isEmpty()) null else Region(isoCode = regionIsoCode, name = regionName)
//        )
//        setLanguageIsoCode("")
//        setLanguageName("")
//        setRegionIsoCode("")
//        setRegionName("")
//        props.onSubmit(locale)
//    }
//
////    val changeHandler: (Event) -> Unit = {
////        val value = (it.target as HTMLInputElement).value
////        setText(value)
////    }
//
//    form {
//        attrs.onSubmitFunction = submitHandler
//
//        +"Language ISO Code"
//        input(InputType.text) {
//            attrs.onChangeFunction = {
//                val value = (it.target as HTMLInputElement).value
//                setLanguageIsoCode(value)
//            }
//            attrs.value = languageIsoCode
//        }
//        +"Language Name"
//        input(InputType.text) {
//            attrs.onChangeFunction = {
//                val value = (it.target as HTMLInputElement).value
//                setLanguageName(value)
//            }
//            attrs.value = languageName
//        }
//        +"Region ISO Code (optional)"
//        input(InputType.text) {
//            attrs.onChangeFunction = {
//                val value = (it.target as HTMLInputElement).value
//                setRegionIsoCode(value)
//            }
//            attrs.value = regionIsoCode
//        }
//        +"Region Name (optional)"
//        input(InputType.text) {
//            attrs.onChangeFunction = {
//                val value = (it.target as HTMLInputElement).value
//                setRegionName(value)
//            }
//            attrs.value = regionName
//        }
//    }
//}
