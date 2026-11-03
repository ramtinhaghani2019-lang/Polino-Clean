package com.polino.app.voice

private fun normalizeVoiceDigits(input: String): String = input.map { char ->
    when (char) {
        in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
        '٬', '،' -> ','
        else -> char
    }
}.joinToString("")

private val SPOKEN_AMOUNT_REGEX = Regex("""([\d,]+)\s*(میلیون|هزار|تومان|ریال)?""")

/**
 * اولین مبلغِ گفته‌شده را از متن تشخیص گفتار بیرون می‌کشد (تومان).
 * نسخه اول فقط عددهای رقمی را می‌فهمد («۵۰۰ هزار تومان» → ۵۰۰۰۰۰)؛
 * عددهای کاملاً کلمه‌ای («پانصد هزار») null برمی‌گردانند تا چیزی حدسی پر نشود.
 */
fun parseSpokenAmount(text: String): Long? {
    val normalized = normalizeVoiceDigits(text)
    val match = SPOKEN_AMOUNT_REGEX.find(normalized) ?: return null
    val raw = match.groupValues[1].replace(",", "").toLongOrNull() ?: return null
    if (raw <= 0L || raw > 9_000_000_000L) return null
    val amount = when (match.groupValues[2]) {
        "میلیون" -> raw * 1_000_000L
        "هزار" -> raw * 1_000L
        "ریال" -> raw / 10L
        else -> raw
    }
    return amount.takeIf { it > 0L }
}
