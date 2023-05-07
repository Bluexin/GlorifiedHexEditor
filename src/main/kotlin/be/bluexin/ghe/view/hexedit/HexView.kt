package be.bluexin.ghe.view.hexedit

import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import be.bluexin.ghe.view.common.AppShapes
import kotlin.math.min

private const val BYTE_SIZE = 2
private const val WORD_SIZE = 8
private const val PARAGRAPH_SIZE = 64

@Composable
fun HexView(textValue: MutableState<String>, modifier: Modifier = Modifier, selected: MutableState<IntRange?>) {
    var text by remember { mutableStateOf(TextFieldValue(textValue.value)) }
    val selectedRange = remember(selected.value) { selected.value }
    val color = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)

    OutlinedTextField(
        label = {
            val from = (selectedRange?.first ?: text.selection.min) / 2
            val to = (selectedRange?.last ?: text.selection.max) / 2
            Text("Selected byte index : $from to $to")
        },
        value = text,
        onValueChange = {
            text = it.copy(
                text = it.text.uppercase().filter { char ->
                    char in '0'..'9' || char in 'A'..'F'
                }, selection = TextRange(
                    ((it.selection.start + 1) / 2) * 2,
                    if (it.selection.end < text.selection.start) ((it.selection.end) / 2) * 2 else ((it.selection.end + 1) / 2) * 2
                )
            )
            textValue.value = text.text
        },
        singleLine = false,
        placeholder = { Text("Enter hex string") },
        modifier = modifier.onFocusChanged {
            if (it.hasFocus) selected.value = null
        },
        maxLines = 6,
        shape = AppShapes.roundStart,
        visualTransformation = {
            TransformedText(
                AnnotatedString(it.text, spanStyles = buildList {
                    spanStyle(BYTE_SIZE, it.text.length, .25.em)
                    spanStyle(WORD_SIZE, it.text.length, .5.em)
                    this += AnnotatedString.Range(SpanStyle(fontFamily = FontFamily.Monospace), 0, it.text.length)
                    if (selectedRange != null) this += AnnotatedString.Range(
                        SpanStyle(color = color), selectedRange.first, selectedRange.last
                    )
                }, paragraphStyles = List(it.text.length / PARAGRAPH_SIZE + 1) { idx ->
                    AnnotatedString.Range(
                        ParagraphStyle(), idx * PARAGRAPH_SIZE, min(it.text.length, (idx + 1) * PARAGRAPH_SIZE)
                    )
                }),
                OffsetMapping.Identity
            )
        },
    )
}

private fun MutableList<AnnotatedString.Range<SpanStyle>>.spanStyle(length: Int, size: Int, spacing: TextUnit) {
    repeat(size / length) { idx ->
        this += AnnotatedString.Range(
            SpanStyle(letterSpacing = spacing), (idx + 1) * length - 1, min(size, (idx + 1) * length)
        )
    }
}
