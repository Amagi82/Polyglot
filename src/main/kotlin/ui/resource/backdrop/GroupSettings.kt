package ui.resource.backdrop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ui.core.Chip
import ui.resource.ResourceViewModel

@Composable
fun GroupSettings(vm: ResourceViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val groups by vm.groups.collectAsState(setOf())
        val excludedGroups by vm.excludedGroups.collectAsState()

        Text("Groups", style = MaterialTheme.typography.h6)

        val text = if (excludedGroups.isEmpty()) "Hide all" else "Show all"
        Chip(
            text = { Text(text) },
            onClick = { vm.excludedGroups.value = if (excludedGroups.isEmpty()) groups else setOf() },
            isClickEnabled = true,
            trailingIcon = { Icon(painterResource(R.drawable.visibility), contentDescription = text) })

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
