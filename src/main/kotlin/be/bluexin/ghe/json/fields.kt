package be.bluexin.ghe.json

import kotlin.math.pow

val Size.characterOffset get() = offset * 2

val Size.maxValue get() = (2.0.pow(size * 4) - 1).toULong()
val Size.characterSize get() = size * 2
val Size.maxCharacter get() = characterOffset + characterSize
fun Size.write(value: UInt, text: String): String =
    if (value <= maxValue && maxCharacter <= text.length) {
        text.replaceRange(
            offset, maxCharacter,
            value.toString(16).uppercase().padStart(size, '0').chunked(2).reversed()
                .joinToString(separator = "")
        )
    } else text

fun String.substring(field: Size) =
    if (field.maxCharacter <= length) substring(field.characterOffset, field.maxCharacter)
    else ""

fun String.readBE() = ifEmpty { "0" }
    .chunked(2).reversed()
    .joinToString(separator = "").toULong(16)
    .toString()
