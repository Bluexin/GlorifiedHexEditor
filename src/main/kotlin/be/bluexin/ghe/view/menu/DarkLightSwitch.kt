package be.bluexin.ghe.view.menu

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import be.bluexin.ghe.view.common.AppIcons
import compose.icons.evaicons.outline.Moon
import compose.icons.evaicons.outline.Sun

@Composable
fun DarkLightSwitch(darkMode: Boolean, onCheckedChange: (Boolean) -> Unit) {
    IconToggleButton(darkMode, onCheckedChange = onCheckedChange) {
        Row {
            Icon(AppIcons.Sun, "Switch to Light Mode")
            Switch(darkMode, null)
            Icon(AppIcons.Moon, "Switch to Dark Mode")
        }
    }
}