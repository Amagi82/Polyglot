package ui.resource.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import locales.Locale
import ui.core.IconButton
import ui.resource.ResourceViewModel

@Composable
fun FiltersMenu(vm: ResourceViewModel) {
    Column(Modifier.width(300.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val project by vm.project.collectAsState()
        val localizedResources by vm.localizedResources.collectAsState()
        val excludedLocales by vm.excludedLocales.collectAsState()

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Filters", style = MaterialTheme.typography.h6)
            IconButton(Icons.Default.Close, contentDescription = "Close filters menu") { vm.menuState.value = MenuState.CLOSED }
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
