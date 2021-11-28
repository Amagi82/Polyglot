package ui.resource.backdrop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ui.core.Chip
import ui.resource.ResourceViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GroupSettings(vm: ResourceViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val groups by vm.groups.collectAsState(setOf())
        val excludedGroups by vm.excludedGroups.collectAsState()

        Text("Groups", style = MaterialTheme.typography.h6)

        groups.forEach { group ->
            val isExcluded = group in excludedGroups
            Chip(
                text = { Text(group.name.ifEmpty { "(none)" }) },
                onClick = { vm.excludedGroups.value = if (isExcluded) excludedGroups.minus(group) else excludedGroups.plus(group) },
                isClickEnabled = true,
                leadingIcon = {
                    if (isExcluded) Icon(painterResource(R.drawable.visibilityOff), contentDescription = "Hidden")
                    else Icon(painterResource(R.drawable.visibility), contentDescription = "Visible")
                })
        }
    }
}
