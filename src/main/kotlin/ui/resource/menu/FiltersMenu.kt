package ui.resource.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import locales.Locale
import project.Resource
import ui.resource.ResourceViewModel

@Composable
fun FiltersMenu(vm: ResourceViewModel) {
    Column(Modifier.width(300.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val project by vm.project.collectAsState()
        val localizedResources by vm.localizedResources.collectAsState()
        val excludedLocales by vm.excludedLocales.collectAsState()
        val excludedResourceTypes by vm.excludedResourceTypes.collectAsState()

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Filters", style = MaterialTheme.typography.h6)
            IconButton(onClick = { vm.menuState.value = MenuState.CLOSED }) {
                Icon(Icons.Default.Close, contentDescription = "Close filters menu")
            }
        }

        Text("Resource types", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.subtitle1)
        Resource.Type.values().forEach { resType ->
            var isChecked by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.height(32.dp)
                    .background(
                        color = if (isChecked) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 4.dp)
                    .clickable {
                        isChecked = !isChecked
                        vm.excludedResourceTypes.value =
                            if (isChecked) excludedResourceTypes.minus(resType) else excludedResourceTypes.plus(resType)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isChecked) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp).size(18.dp),
                        tint = MaterialTheme.colors.onSecondary
                    )
                }
                Text(
                    resType.name,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colors.onSecondary,
                    style = MaterialTheme.typography.body2
                )
            }
        }

        Text("Locales", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.subtitle1)
        localizedResources.keys.forEach { localeIsoCode ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Switch(checked = localeIsoCode !in excludedLocales,
                    onCheckedChange = { isChecked ->
                        vm.excludedLocales.value = if (isChecked) excludedLocales.minus(localeIsoCode) else excludedLocales.plus(localeIsoCode)
                    })
                Text(Locale[localeIsoCode].displayName(localeIsoCode == project.defaultLocale))
            }
        }
    }
}
