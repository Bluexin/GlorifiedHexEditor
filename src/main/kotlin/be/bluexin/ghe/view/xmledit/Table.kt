package be.bluexin.ghe.view.xmledit

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import be.bluexin.ghe.json.DataLayout
import be.bluexin.ghe.view.logger

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableScreen(dataLayout: DataLayout?) {
    if (dataLayout == null) Text("No layout found")
    else {
        Row(Modifier.padding(8.dp)) {
            val vScroll = rememberLazyListState()
            val hScroll = rememberScrollState()

            VerticalScrollbar(
                rememberScrollbarAdapter(vScroll),
                Modifier.padding(top = 16.dp, end = 8.dp, bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.SpaceBetween) {
                HorizontalScrollbar(
                    rememberScrollbarAdapter(hScroll),
                    Modifier.padding(bottom = 8.dp, end = 8.dp, start = 8.dp)
                )

                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                    dataLayout.fields.forEach {
                        TableCell(text = it.name, enabled = false)
                    }
                }

                LazyColumn(state = vScroll) {
                    items(100) { index ->
                        Row(modifier = Modifier.horizontalScroll(hScroll).animateItemPlacement()) {
                            dataLayout.fields.forEach {
                                TableCell(text = it.name, enabled = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    enabled: Boolean = true
) {
    val textFieldValueState = remember(text) {
        mutableStateOf(TextFieldValue(text = text))
    }
    OutlinedTextField(
        textFieldValueState.value,
        onValueChange = {
            logger.info { "New value: ${it.text}" }
            textFieldValueState.value = it
        },
        shape = MaterialTheme.shapes.large,
        enabled = enabled
    )
}
