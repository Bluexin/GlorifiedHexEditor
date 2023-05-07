package be.bluexin.ghe.view.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import be.bluexin.ghe.view.common.AppIcons
import be.bluexin.layoutloader.json.Settings
import compose.icons.evaicons.outline.Menu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FrameWindowScope.topBar(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    selectedTab: MutableState<AppTabs>
) {
    TopAppBar(
        title = { Text(text = "Glorified Hex Editor") },
        elevation = 8.dp,
        navigationIcon = {
            IconButton(onClick = {
                scope.launch { scaffoldState.drawerState.open() }
            }) {
                Icon(AppIcons.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            TabRow(selectedTab.value.ordinal) {
                AppTabs.values().forEach { Tab(it, selectedTab) }
            }
        }
    )
}

@Composable
fun FrameWindowScope.menuContent(
    settings: Settings?,
    darkMode: MutableState<Boolean>,
    onLoad: (File) -> Unit
) = Column(
    modifier = Modifier
        .padding(start = 8.dp)
) {
    LoadButton(settings?.metadata, onLoad)
    DarkLightSwitch(darkMode.value) { darkMode.value = it }
}

@Composable
private fun Tab(
    tab: AppTabs,
    selectedTab: MutableState<AppTabs>
) = Tab(
    selected = selectedTab.value == tab,
    onClick = { selectedTab.value = tab },
    text = { Text(tab.displayName) }
)
