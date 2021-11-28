package ui.resource

import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import locales.Locale
import locales.LocaleIsoCode
import project.ResourceGroup
import project.Resource
import project.ResourceId
import project.type

@Composable
fun <R : Resource> ResourceTable(
    vm: ResourceTypeViewModel<R>,
    excludedGroups: Set<ResourceGroup>,
    displayedLocales: List<LocaleIsoCode>,
    isMultiSelectEnabled: Boolean
) {
    val state = rememberLazyListState()

    val resourceGroups by vm.resourceGroups.collectAsState()
    val filteredGroups = resourceGroups.filter { it.key !in excludedGroups }
    val keys = filteredGroups.flatMap { it.value.toSortedSet() }
    val localizedResourcesById by vm.localizedResourcesById.collectAsState()

    val selectedRows by vm.selectedRows.collectAsState()
    var firstClickIndex by remember(keys) { mutableStateOf<Int?>(null) }

    LazyColumn(Modifier.fillMaxSize(), state = state) {
        stickyHeader {
            ResourceTableHeader(displayedLocales)
        }
        items(keys, key = { it.value }) { resId ->
            val resources = localizedResourcesById[resId].orEmpty()
            if (resources.any { it.value::class.type != vm.type }) return@items
            ResourceRow(
                vm = vm,
                displayedLocales = displayedLocales,
                resources = resources,
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
                        isShiftPressed && selectedRows.isEmpty() -> setOf(resId).also { firstClickIndex = clickIndex }
                        isShiftPressed && firstIndex != null && clickIndex < firstIndex -> keys.slice(clickIndex..firstIndex).toSet()
                        isShiftPressed && firstIndex != null && clickIndex > firstIndex -> keys.slice(firstIndex..clickIndex).toSet()
                        isShiftPressed && firstIndex == clickIndex -> selectedRows
                        isSelected -> setOf<ResourceId>().also { firstClickIndex = null }
                        else -> setOf(resId).also { firstClickIndex = clickIndex }
                    }
                }
            )
            Divider(Modifier.padding(horizontal = 8.dp))
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        VerticalScrollbar(adapter = ScrollbarAdapter(state))
    }
}

@Composable
private fun ResourceTableHeader(displayedLocales: List<LocaleIsoCode>) {
    Surface {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 112.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Group", Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold)
            Text("ID", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            displayedLocales.forEachIndexed { i, it ->
                Text(Locale[it].displayName(i == 0), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Divider()
}
