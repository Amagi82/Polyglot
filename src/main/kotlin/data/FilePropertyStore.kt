package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

abstract class FilePropertyStore(private val file: File) : PropertyStore(file.apply(File::createNewFile).inputStream()) {
    protected fun store(comment: String = "", onFailure: (Exception) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (isEmpty()) file.delete()
                else props.store(file.outputStream(), comment)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    protected open fun save() {
        store { println("Error saving: $it") }
    }

    protected fun <T> mutableStateFlowOf(propName: String, getter: (String?) -> T, setter: (T) -> String = { "$it" }) =
        MutableStateFlow(getter(get(propName))).also { stateFlow ->
            GlobalScope.launch {
                stateFlow.collectLatest {
                    put(propName, setter(it))
                    save()
                }
            }
        }

    protected fun <T> mutableStateFlowOf(value: T, update: (T) -> Unit) = MutableStateFlow(value).also { stateFlow ->
        GlobalScope.launch {
            stateFlow.collectLatest {
                update(it)
                save()
            }
        }
    }
}
