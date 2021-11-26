package ui.resource

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.GroupId
import project.Metadata
import project.Resource
import project.ResourceId

@Composable
fun <R : Resource, M : Metadata<M>> ResourceTable(
    vm: ResourceTypeViewModel<R, M>,
    excludedGroups: Set<GroupId>,
    displayedLocales: List<LocaleIsoCode>,
    isMultiSelectEnabled: Boolean
) {
    Row {
        val state = rememberLazyListState()
        Column(Modifier.weight(1f)) {
            val modifier = Modifier.padding(start = 16.dp, end = 8.dp)

            val spacer by animateDpAsState(
                targetValue = when (vm) {
                    is StringResourceViewModel -> 0.dp
                    is PluralResourceViewModel -> 40.dp
                    is ArrayResourceViewModel -> 64.dp
                }
            )
            ResourceTableHeader(modifier, spacer, displayedLocales)

            val metadataById by vm.metadataById.collectAsState()
            val filteredMetadata = metadataById.filter { it.value.type == vm.type && it.value.group !in excludedGroups }
            val keys = filteredMetadata.keys.toList()
            val localizedResourcesById by vm.localizedResourcesById.collectAsState()

            val selectedRows by vm.selectedRows.collectAsState()
            var firstClickIndex by remember(metadataById) { mutableStateOf<Int?>(null) }

            LazyColumn(modifier = modifier, state = state) {
                items(keys, key = { it.value }) { resId ->
                    ResourceRow(
                        vm = vm,
                        metadata = filteredMetadata[resId]!!,
                        displayedLocales = displayedLocales,
                        resources = localizedResourcesById[resId].orEmpty(),
                        resId = resId,
                        isMultiSelectEnabled = isMultiSelectEnabled,
                        isSelected = resId in selectedRows,
                        onClick = { isCtrlPressed, isShiftPressed ->
                            val isSelected = resId in selectedRows
                            val clickIndex = keys.indexOf(resId)
                            val firstIndex = firstClickIndex
                            vm.selectedRows.value = when {
                                isCtrlPressed && isSelected -> selectedRows.minus(resId).also { if (it.isEmpty()) firstClickIndex = null }
                                isCtrlPressed -> selectedRows.plus(resId).also { if (firstClickIndex == null) firstClickIndex = clickIndex }
                                isShiftPressed && selectedRows.isEmpty() -> listOf(resId).also { firstClickIndex = clickIndex }
                                isShiftPressed && firstIndex != null && clickIndex < firstIndex -> keys.slice(clickIndex..firstIndex)
                                isShiftPressed && firstIndex != null && clickIndex > firstIndex -> keys.slice(firstIndex..clickIndex)
                                isShiftPressed && firstIndex == clickIndex -> selectedRows
                                isSelected -> listOf<ResourceId>().also { firstClickIndex = null }
                                else -> listOf(resId).also { firstClickIndex = clickIndex }
                            }
                        }
                    )
                    Divider()
                }
            }

            val scope = rememberCoroutineScope()
            val scrollToItem by vm.scrollToItem.collectAsState()
            if (scrollToItem != null) {
                val i = keys.indexOf(scrollToItem)
                if (i != -1) {
                    scope.launch { state.animateScrollToItem(i) }
                    vm.scrollToItem.value = null
                }
            }
        }
        VerticalScrollbar(adapter = ScrollbarAdapter(state))
    }
}

@Composable
private fun ResourceTableHeader(
    modifier: Modifier,
    spacer: Dp,
    displayedLocales: List<LocaleIsoCode>
) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, end = 104.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("group", Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold)
        Text("id", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(spacer))
        displayedLocales.forEachIndexed { i, it ->
            Text(Locale[it].displayName(i == 0), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        }
    }
    Divider()
}
