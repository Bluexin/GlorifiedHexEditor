package be.bluexin.ghe.view.hexedit

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.bluexin.ghe.view.common.AppShapes
import be.bluexin.ghe.view.hexedit.fieldtree.FieldTree
import be.bluexin.ghe.view.hexedit.fieldtree.FieldTreeView
import be.bluexin.layoutloader.json.DataLayout

@Composable
fun HexEditor(layouts: Map<String, DataLayout>) {
    var selectedLayoutName by remember { mutableStateOf<String?>(null) }
    val selectedLayout by remember(selectedLayoutName, layouts) {
        derivedStateOf {
            selectedLayoutName?.let { layouts[it] }
        }
    }
    var expanded by remember { mutableStateOf(false) }
    val textValue = remember { mutableStateOf("0123456789ABCDEF".repeat(50)) }
    val selectedRange = remember { mutableStateOf<IntRange?>(null) }

    Column(
        Modifier
            .padding(8.dp)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            HexView(
                textValue, Modifier
                    .weight(.8f),
                selectedRange
            )

            Box(Modifier.weight(.2f, false)) {
                Button(
                    onClick = { expanded = true },
                    shape = AppShapes.roundEnd,
                    modifier = Modifier.height(55.dp)
                ) {
                    if (selectedLayout == null) Text("No layout selected")
                    else Text(selectedLayout!!.fileFilter)
                }
                DropdownMenu(expanded, { expanded = false }) {
                    layouts.forEach { (filter, layout) ->
                        DropdownMenuItem({
                            selectedLayoutName = layout.name
                            expanded = false
                        }) { Text(filter) }
                    }
                }
            }
        }

        selectedLayout?.let { FieldTreeView(FieldTree(it), textValue, selectedRange) }
    }
}
