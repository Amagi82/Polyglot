package ui.resource

import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import locales.Locale
import locales.LocaleIsoCode
import project.Resource

@Composable
fun <R : Resource> ResourceTable(vm: ResourceTypeViewModel<R>, displayedLocales: List<LocaleIsoCode>, defaultLocale: LocaleIsoCode) {
    Row {
        val state = rememberLazyListState()
        Column(Modifier.padding(start = 16.dp, end = 8.dp).weight(1f)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, end = 104.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("id", Modifier.weight(1f))
                when (vm) {
                    is StringResourceViewModel -> Unit
                    is PluralResourceViewModel -> Spacer(Modifier.width(40.dp))
                    is ArrayResourceViewModel -> Spacer(Modifier.width(64.dp))
                }
                displayedLocales.forEach {
                    Text(Locale[it].displayName(it == defaultLocale), Modifier.weight(1f))
                }
            }
            Divider()

            val resources by vm.displayedResources.collectAsState(listOf())
            LazyColumn(state = state) {
                items(resources, key = { it.value }) { resId ->
                    ResourceRow(vm = vm, displayedLocales = displayedLocales, resId = resId)
                    Divider()
                }
            }
        }
        VerticalScrollbar(adapter = ScrollbarAdapter(state))
    }
}
