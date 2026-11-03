package com.polino.app.data

import java.util.UUID

/**
 * تجزیه JSON قدیمی تراکنش‌ها بدون وابستگی خارجی.
 *
 * چرا دستی؟ org.json اندروید در تست واحد JVM فقط یک استاب است و فراخوانی‌اش
 * استثنا می‌دهد؛ پس تابع تجزیه باید بدون هیچ کلاس اندرویدی کار کند تا هم در
 * اپ و هم در تست، دقیقاً یک پیاده‌سازی اجرا شود. ورودی فقط همان فرمتی است که
 * نسخه قدیمی با JSONObject می‌ساخت: آرایه‌ای از آبجکت‌های تخت با فیلدهای
 * مشخص. ترتیب فیلدها مهم نیست؛ فیلدهای غایب مقدار پیش‌فرض می‌گیرند؛ رکوردی
 * که فیلد اصلی‌اش (عنوان، مبلغ، نوع) خراب باشد حذف می‌شود و بقیه حفظ می‌شوند.
 */
internal fun parseLegacyTransactions(raw: String): List<MoneyTransaction> {
    return try {
        LegacyJsonParser(raw).parseArray()
    } catch (_: Exception) {
        emptyList()
    }
}

private class LegacyJsonParser(private val text: String) {
    private var i = 0
    private val n = text.length

    fun parseArray(): List<MoneyTransaction> {
        skipWs()
        if (consume() != '[') throw IllegalArgumentException("not an array")
        val out = ArrayList<MoneyTransaction>()
        skipWs()
        if (peek() == ']') {
            i++
            return out
        }
        while (true) {
            val item = parseObject()
            if (item != null) out.add(item)
            skipWs()
            when (consume()) {
                ',' -> continue
                ']' -> return out
                else -> throw IllegalArgumentException("expected , or ]")
            }
        }
    }

    private fun parseObject(): MoneyTransaction? {
        skipWs()
        if (consume() != '{') throw IllegalArgumentException("not an object")
        var id: String? = null
        var title: String? = null
        var amount: Long? = null
        var income: Boolean? = null
        var category: String? = null
        var dateMillis: Long? = null
        var accountId: String? = null
        var note: String? = null
        skipWs()
        if (peek() == '}') {
            i++
            return null
        }
        while (true) {
            val key = parseString() ?: throw IllegalArgumentException("bad key")
            skipWs()
            if (consume() != ':') throw IllegalArgumentException("expected :")
            when (key) {
                "id" -> id = parseValueAsString()
                "title" -> title = parseValueAsString()
                "amount" -> amount = parseValueAsLong()
                "income" -> income = parseValueAsBoolean()
                "category" -> category = parseValueAsString()
                "dateMillis" -> dateMillis = parseValueAsLong()
                "accountId" -> accountId = parseValueAsString()
                "note" -> note = parseValueAsString()
                else -> skipValue()
            }
            skipWs()
            when (consume()) {
                ',' -> continue
                '}' -> break
                else -> throw IllegalArgumentException("expected , or }")
            }
        }
        if (title == null || amount == null || income == null) return null
        return MoneyTransaction(
            id = id?.ifBlank { UUID.randomUUID().toString() } ?: UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            income = income,
            category = category?.ifBlank { "سایر" } ?: "سایر",
            dateMillis = dateMillis ?: 0L,
            accountId = accountId?.ifBlank { "main" } ?: "main",
            note = note ?: ""
        )
    }

    private fun parseValueAsString(): String? {
        val s = parseString()
        if (s == null) skipValue()
        return s
    }

    private fun parseValueAsLong(): Long? {
        skipWs()
        val start = i
        while (i < n && (text[i].isDigit() || text[i] == '-' || text[i] == '+')) i++
        if (start == i) {
            skipValue()
            return null
        }
        return text.substring(start, i).toLongOrNull()
    }

    private fun parseValueAsBoolean(): Boolean? {
        skipWs()
        return when {
            text.startsWith("true", i) -> {
                i += 4
                true
            }
            text.startsWith("false", i) -> {
                i += 5
                false
            }
            else -> {
                skipValue()
                null
            }
        }
    }

    private fun parseString(): String? {
        skipWs()
        if (peek() != '"') return null
        i++
        val sb = StringBuilder()
        while (i < n) {
            val c = text[i]
            when {
                c == '"' -> {
                    i++
                    return sb.toString()
                }
                c == '\\' && i + 1 < n -> {
                    when (val e = text[i + 1]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (i + 5 < n) {
                                text.substring(i + 2, i + 6).toIntOrNull(16)?.let {
                                    sb.append(it.toChar())
                                }
                                i += 4
                            }
                        }
                        else -> sb.append(e)
                    }
                    i += 2
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return null
    }

    private fun skipValue() {
        skipWs()
        when (peek()) {
            '"' -> parseString()
            '{', '[' -> {
                val open = consume()!!
                val close = if (open == '{') '}' else ']'
                var depth = 1
                while (i < n && depth > 0) {
                    val c = text[i]
                    if (c == '"') {
                        parseString()
                        continue
                    }
                    if (c == open) depth++
                    if (c == close) depth--
                    i++
                }
            }
            else -> {
                while (i < n && text[i] != ',' && text[i] != '}' && text[i] != ']') i++
            }
        }
    }

    private fun skipWs() {
        while (i < n && text[i].isWhitespace()) i++
    }

    private fun peek(): Char? = if (i < n) text[i] else null

    private fun consume(): Char? = if (i < n) text[i++] else null
}
