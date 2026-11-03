package com.polino.app.data

import java.util.Locale

/**
 * خروجی JSON تراکنش‌ها برای پشتیبان‌گیری. فرمت دقیقاً همان فرمت ۶فیلدی
 * قدیمی است تا ورود آن با همان تجزیه‌کننده مهاجرت انجام شود.
 */
internal fun transactionsToJson(transactions: List<MoneyTransaction>): String = buildString {
    append('[')
    transactions.forEachIndexed { index, t ->
        if (index > 0) append(',')
        append(
            "{\"id\":\"${escapeJsonString(t.id)}\"," +
                "\"title\":\"${escapeJsonString(t.title)}\"," +
                "\"amount\":${t.amount}," +
                "\"income\":${t.income}," +
                "\"category\":\"${escapeJsonString(t.category)}\"," +
                "\"dateMillis\":${t.dateMillis}}"
        )
    }
    append(']')
}

internal fun escapeJsonString(value: String): String = buildString {
    for (c in value) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (c < ' ') append("\\u%04x".format(Locale.US, c.code)) else append(c)
            }
        }
    }
}
