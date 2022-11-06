package be.bluexin.ghe.view.hexedit.fieldtree

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import be.bluexin.ghe.json.*
import be.bluexin.ghe.view.common.AppIcons
import be.bluexin.ghe.view.common.AppShapes
import be.bluexin.ghe.view.logger
import compose.icons.evaicons.outline.*
import kotlin.math.pow
import androidx.compose.ui.geometry.Size as GeomSize

@Composable
fun FieldTreeViewTabView() = Surface {
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Files",
            color = LocalContentColor.current.copy(alpha = 0.60f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun FieldTreeView(model: FieldTree, text: MutableState<String>, selected: MutableState<IntRange?>) = Surface(
    modifier = Modifier.fillMaxSize()
) {
    val items by remember(model.layout.loadingTimeStamp) {
        derivedStateOf { model.items }
    }
    Box {
        val scrollState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 18.dp),
            state = scrollState
        ) {
            items(items.size) {
                FieldTreeItemView(items[it], text, selected)
            }
        }

        VerticalScrollbar(
            rememberScrollbarAdapter(scrollState),
            Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun FieldTreeItemView(model: ExpandableField, text: MutableState<String>, selected: MutableState<IntRange?>) {
    Row(
        modifier = Modifier
            .padding(start = 24.dp * model.level)
            .fillMaxWidth()
            .let {
                if (model.canExpand) it.clickable(onClick = model::toggleExpanded)
                else it
            }
    ) {

        if (model.canExpand) {
            val active = remember { mutableStateOf(false) }
            if (model.isExpanded) Icon(AppIcons.ArrowDown, "collapse")
            else Icon(AppIcons.ArrowRight, "expand")
            Text(
                text = model.field.name,
                color = if (active.value) LocalContentColor.current.copy(alpha = 0.60f) else LocalContentColor.current,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clipToBounds(),
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        } else {
            val field = model.field

            val (offset, label) = when (field) {
                is Repeated -> field.offset(model.idx) * 2 to field.name(model.idx + 1)
                else -> field.offset * 2 to field.name
            }

            when (field) {
                is Structure -> field.structureRef.fields.forEach {
                    SingleField(
                        model, text, "$label-${it.name}", offset + it.offset * 2, it.size * 2,
                        selected, modifier = Modifier.padding(end = 8.dp)
                    )
                }
                is Lookup -> {
                    LookupModal(
                        field.lookupRef,
                        SingleField(
                            model, text, label, offset, field.size * 2, selected,
                            maxValue = field.lookupRef.values.size.toULong() - 1u,
                            shape = AppShapes.roundStart
                        )
                    )
                }
                is Size -> SingleField(model, text, label, offset, field.size * 2, selected)
                else -> error("Unknown $field")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SingleField(
    model: ExpandableField,
    text: MutableState<String>,
    label: String,
    offset: Int,
    size: Int,
    selected: MutableState<IntRange?>,
    maxValue: ULong = (2.0.pow(size * 4) - 1).toULong(),
    shape: Shape = MaterialTheme.shapes.small,
    modifier: Modifier = Modifier
): MutableState<TextFieldValue> {
    val maxChar = offset + size

    val textSubString = remember(model, text.value) {
        if (maxChar <= text.value.length) text.value.substring(offset, maxChar) else ""
    }

    fun saveTextFieldValue(value: UInt): Boolean =
        if (value <= maxValue && maxChar <= text.value.length) {
            text.value = text.value.replaceRange(
                offset, maxChar,
                value.toString(16).uppercase().padStart(size, '0').chunked(2).reversed()
                    .joinToString(separator = "")
            )
            true
        } else false

    fun getTextFieldValue(): String = textSubString.ifEmpty { "0" }
        .chunked(2).reversed()
        .joinToString(separator = "").toULong(16)
        .toString()

    val textFieldValueState = remember(model, textSubString) {
        mutableStateOf(TextFieldValue(text = getTextFieldValue()))
    }
    var textFieldValue by textFieldValueState

    val changed by remember(model, textSubString, textFieldValue) {
        mutableStateOf(textFieldValue.text != getTextFieldValue())
    }

    TooltipArea(tooltip = {
        tooltip(buildString {
            model.field.description?.let { appendLine(it) }
            append("Range : [0, ${maxValue}]")
        })
    }) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it.copy(it.text.filter(Char::isDigit))
            },
            label = { Text(label) },
            placeholder = { model.field.description?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = modifier.onFocusChanged {
                if (it.hasFocus) selected.value = if (maxChar <= text.value.length) offset..maxChar else null
            },
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
            shape = shape,
            isError = (textFieldValue.text.toULongOrNull() ?: 0uL) > maxValue,
            enabled = maxChar <= text.value.length
        )
    }

    return textFieldValueState
}

@Composable
private fun LookupModal(lookupRef: LayoutLookup, currentValue: MutableState<TextFieldValue>) {
    var isDialogOpen by remember { mutableStateOf(false) }
    Button(
        onClick = { isDialogOpen = true },
        shape = AppShapes.roundEnd,
        modifier = Modifier.height(64.dp).padding(top = 8.dp)
    ) {
        val current = currentValue.value.text.toIntOrNull()?.let { lookupRef.values.getOrNull(it) }
        Text(current ?: "Unknown value selected")
    }
    val dialogState = rememberDialogState(WindowPosition.PlatformDefault)
    Dialog(onCloseRequest = { isDialogOpen = false }, state = dialogState, visible = isDialogOpen) {
        Surface {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                var searchValue by remember { mutableStateOf("") }
                OutlinedTextField(
                    searchValue,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { searchValue = it },
                    singleLine = true,
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(AppIcons.Search, "search") },
                    trailingIcon = {
                        Icon(AppIcons.Backspace, "clear", modifier = Modifier.clickable { searchValue = "" })
                    }
                )

                val filtered by remember(searchValue, lookupRef) {
                    derivedStateOf { lookupRef.values.withIndex().filter { it.value.contains(searchValue) } }
                }

                if (filtered.isEmpty()) {
                    Text("No results")
                } else {
                    Box(Modifier.padding(top = 8.dp)) {
                        val scrollState = rememberLazyListState()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 18.dp),
                            state = scrollState
                        ) {
                            items(filtered.size) {
                                val iv = filtered[it]
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        currentValue.value = currentValue.value.copy(iv.index.toString())
                                        isDialogOpen = false
                                    }
                                ) {
                                    Text(iv.value)
                                }
                            }
                        }

                        VerticalScrollbar(
                            rememberScrollbarAdapter(scrollState),
                            Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun tooltip(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(PaddingValues(start = 12.dp))
                    .background(
                        color = MaterialTheme.colors.secondary,
                        shape = TriangleEdge
                    )
                    .width(12.dp)
                    .height(12.dp)
            ) {}
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colors.secondary,
                        shape = RoundedCornerShape(size = 3.dp)
                    )
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.onSecondary
                )
            }

        }
    }
}

object TriangleEdge : Shape {
    override fun createOutline(
        size: GeomSize,
        layoutDirection: LayoutDirection,
        density: Density
    ) = Outline.Generic(path = Path().apply {
        moveTo(x = size.width / 2, y = 0f)
        lineTo(x = size.width, y = size.height)
        lineTo(x = 0f, y = size.height)
    })
}