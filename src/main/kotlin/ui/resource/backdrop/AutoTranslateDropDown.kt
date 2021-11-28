package ui.resource.backdrop

import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import locales.LocaleIsoCode
import project.Plural
import project.Str
import project.StringArray
import translation.CloudTranslateApi
import ui.resource.ResourceViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AutoTranslateDropDown(
    vm: ResourceViewModel,
    apiKey: String?,
    defaultLocale: LocaleIsoCode,
    isoCode: LocaleIsoCode,
    translatableLanguages: List<LocaleIsoCode>
) {
    if (apiKey.isNullOrEmpty() || isoCode !in translatableLanguages) return
    val scope = rememberCoroutineScope()
    DropdownMenuItem(onClick = {
        scope.launch(Dispatchers.IO) {
            for ((resId, localeMap) in vm.strings.localizedResourcesById.value) {
                if (localeMap[isoCode] != null) continue
                val from = localeMap[defaultLocale]?.text ?: continue
                val result = CloudTranslateApi.translate(apiKey = apiKey, from = defaultLocale, to = isoCode, text = listOf(from))
                result.onSuccess { response ->
                    response.data.translations.firstOrNull()?.translatedText?.let {
                        vm.strings.updateResource(resId, isoCode, Str(it))
                    }
                }
            }
            for ((resId, localeMap) in vm.plurals.localizedResourcesById.value) {
                if (localeMap[isoCode] != null) continue
                val from = localeMap[defaultLocale]?.items ?: continue
                val result = CloudTranslateApi.translate(apiKey = apiKey, from = defaultLocale, to = isoCode, text = from.map { it.value })
                result.onSuccess { response ->
                    var i = 0
                    vm.plurals.updateResource(resId, isoCode, Plural(from.mapValues { (_, text) ->
                        (response.data.translations.getOrNull(i)?.translatedText ?: text).also { i++ }
                    }))
                }
            }
            for ((resId, localeMap) in vm.arrays.localizedResourcesById.value) {
                val from = localeMap[defaultLocale]?.items ?: continue
                if (localeMap[isoCode]?.items?.size == from.size) continue
                val result = CloudTranslateApi.translate(apiKey = apiKey, from = defaultLocale, to = isoCode, text = from)
                result.onSuccess { response ->
                    vm.arrays.updateResource(
                        resId,
                        isoCode,
                        StringArray(response.data.translations.map { it.translatedText })
                    )
                }
            }
        }
    }) {
        Icon(
            painterResource(R.drawable.gTranslate),
            contentDescription = "Auto-translate",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("Auto-translate")
    }
}
