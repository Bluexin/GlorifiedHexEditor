package be.bluexin.ghe.view.xmledit

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import be.bluexin.ghe.io.DataFile
import be.bluexin.ghe.io.DataHolder
import be.bluexin.ghe.view.common.AppIcons
import be.bluexin.ghe.view.hexedit.fieldtree.tooltip
import be.bluexin.ghe.view.logger
import be.bluexin.layoutloader.*
import compose.icons.evaicons.outline.Checkmark
import compose.icons.evaicons.outline.Save
import compose.icons.evaicons.outline.Sync
import kotlin.math.pow

private const val useLazyRow = false

@Composable
fun TableScreen(dataLayout: DataLayout?, data: DataFile?) {
    when {
        dataLayout == null -> Text("No layout found")
        data == null -> Text("Loading data")
        data.data.isEmpty() -> Text("No data found")
        else -> {
            Row(Modifier.padding(8.dp)) {
                val vScroll = rememberLazyListState()
                val hScroll = rememberScrollState()
                val hScrollLazy = rememberLazyListState()

                VerticalScrollbar(
                    rememberScrollbarAdapter(vScroll),
                    Modifier.padding(top = 16.dp, end = 8.dp, bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.SpaceBetween) {
//                    logger.info { "Composing Column" }

                    HorizontalScrollbar(
                        if (useLazyRow) rememberScrollbarAdapter(hScrollLazy) else rememberScrollbarAdapter(hScroll),
                        Modifier.padding(bottom = 8.dp, end = 8.dp, start = 8.dp)
                    )

                    if (useLazyRow) LazyRow(state = hScrollLazy) {
                        items(dataLayout.fields) {
                            SimpleReadField(text = it.name)
                        }
                    } else Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        dataLayout.fields.forEach {
                            SimpleReadField(text = it.name)
                        }
                    }

                    val elements = data.data
                    LazyColumn(state = vScroll) {
                        items(elements) { element ->
                            TableRow(element, dataLayout, hScroll, hScrollLazy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    element: DataHolder,
    dataLayout: DataLayout,
    hScroll: ScrollState,
    hScrollLazy: LazyListState,
) {
//    logger.info { "Composing ${element.position}" }
    if (useLazyRow) LazyRow(state = hScrollLazy) {
        items(dataLayout.fields) {
            TableCell(it, element)
        }
    } else Row(modifier = Modifier.horizontalScroll(hScroll)) {
        dataLayout.fields.forEach {
            TableCell(it, element)
        }
    }
}

@Composable
private fun TableCell(field: Field, element: DataHolder) {
    when (field) {
        is Size -> SimpleMappingField(field, mutableStateOf(element.text))
        else -> SimpleReadField(text = "TODO ${field::class.simpleName?.replace("Field", "")}")
    }
}

private val cellWidth = 200.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleReadField(
    text: String
) = TooltipArea(tooltip = {
    tooltip(text)
}) {
    OutlinedTextField(
        value = text,
        onValueChange = { },
        shape = MaterialTheme.shapes.large,
        enabled = false,
        singleLine = true,
        modifier = Modifier.width(cellWidth)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleMappingField(
    field: Size,
    text: MutableState<String>,
) {
    val offset = field.characterOffset
    val size = field.characterSize
    val maxValue = (2.0.pow(size * 4) - 1).toULong()

    val maxChar = field.maxCharacter

    val textSubString = remember(field, text.value) {
        text.value.substring(field)
    }

    fun saveTextFieldValue(value: UInt) {
        text.value = field.write(value, text.value)
    }

    fun getTextFieldValue(): String = try {
        textSubString.readBE()
    } catch (e: Exception) {
//        logger.error(e) { "Could not read $textSubString" }
        "error: $textSubString ($field)"
    }

    val textFieldValueState = remember(field, textSubString) {
        mutableStateOf(TextFieldValue(text = getTextFieldValue()))
    }
    var textFieldValue by textFieldValueState

    val changed by remember(field, textSubString, textFieldValue) {
        mutableStateOf(textFieldValue.text != getTextFieldValue())
    }

    TooltipArea(tooltip = {
        tooltip(buildString {
            appendLine(getTextFieldValue())
            appendLine(field.name)
            field.description?.let { appendLine(it) }
            append("Range : [0, ${maxValue}]")
        })
    }) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it.copy(it.text.filter(Char::isDigit))
            },
//            label = { Text(label) },
            placeholder = { field.description?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            keyboardActions = KeyboardActions { logger.info { "action" } },
            leadingIcon = {
                if (changed) Icon(AppIcons.Save, "save", modifier = Modifier.clickable {
                    saveTextFieldValue(
                        if (textFieldValue.text.isEmpty()) 0u
                        else textFieldValue.text.filter(Char::isDigit).toUInt()
                    )
                }) else Icon(AppIcons.Checkmark, "up-to-date")
            },
            trailingIcon = {
                if (changed) Icon(AppIcons.Sync, "restore", modifier = Modifier.clickable {
                    textFieldValue = textFieldValue.copy(getTextFieldValue())
                })
            },
            shape = MaterialTheme.shapes.large,
            isError = (textFieldValue.text.toULongOrNull() ?: 0uL) > maxValue,
            enabled = maxChar <= text.value.length && offset > 0,
            modifier = Modifier.width(cellWidth)
        )
    }
}
