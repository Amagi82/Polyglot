package ui.resource

import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.Metadata
import project.Resource

@Composable
fun <R : Resource, M : Metadata<M>> ResourceTable(vm: ResourceTypeViewModel<R, M>, displayedLocales: List<LocaleIsoCode>) {
    val metadataById by vm.metadataById.collectAsState()
    if (metadataById.any { it.value.type != vm.type }) return
    Row {
        val state = rememberLazyListState()
        Column(Modifier.weight(1f)) {
            val modifier = Modifier.padding(start = 16.dp, end = 8.dp)
            Row(modifier.fillMaxWidth().padding(top = 12.dp, end = 104.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("id", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                when (vm) {
                    is StringResourceViewModel -> Unit
                    is PluralResourceViewModel -> Spacer(Modifier.width(40.dp))
                    is ArrayResourceViewModel -> Spacer(Modifier.width(64.dp))
                }
                displayedLocales.forEachIndexed { i, it ->
                    Text(Locale[it].displayName(i == 0), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                }
            }
            Divider()

            val localizedResourcesById by vm.localizedResourcesById.collectAsState()
            LazyColumn(modifier = modifier, state = state) {
                items(metadataById.keys.toList(), key = { it.value }) { resId ->
                    ResourceRow(
                        vm = vm,
                        metadata = metadataById[resId]!!,
                        displayedLocales = displayedLocales,
                        resources = localizedResourcesById[resId].orEmpty(),
                        resId = resId
                    )
                    Divider()
                }
            }

            val scope = rememberCoroutineScope()
            val scrollToItem by vm.scrollToItem.collectAsState()
            if (scrollToItem != null) {
                val i = metadataById.keys.indexOf(scrollToItem)
                if (i != -1) {
                    scope.launch { state.animateScrollToItem(i) }
                    vm.scrollToItem.value = null
                }
            }
        }
        VerticalScrollbar(adapter = ScrollbarAdapter(state))
    }
}
