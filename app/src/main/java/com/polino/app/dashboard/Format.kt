package com.polino.app.dashboard

import java.text.NumberFormat
import java.util.Locale

internal fun money(value: Long): String =
    NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(value) + " تومان"

internal fun normalizeDigits(input: String): String = input.map { char ->
    when (char) {
        in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
        else -> char
    }
}.joinToString("")
