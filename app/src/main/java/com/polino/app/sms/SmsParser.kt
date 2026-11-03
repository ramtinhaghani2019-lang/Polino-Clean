package com.polino.app.sms

/** کلید ذخیره وضعیت خوانش پیامک؛ گیرنده و صفحه مدیریت هر دو همین کلید را می‌خوانند. */
const val SMS_PREFS_NAME = "polino_prefs"
const val SMS_ENABLED_KEY = "sms_reading_enabled"

data class ParsedSms(
    val amount: Long,
    val income: Boolean,
    val sender: String
)

private fun normalizeSmsDigits(input: String): String = input.map { char ->
    when (char) {
        in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
        '٬', '،' -> ','
        else -> char
    }
}.joinToString("")

private val AMOUNT_REGEX = Regex("""([\d,]+)\s*(تومان|ریال)""")

private val EXPENSE_WORDS = listOf("برداشت", "خرید", "پرداخت", "کسر", "انتقال")
private val INCOME_WORDS = listOf("واریز", "دریافت")

/**
 * پیامک بانکی را به مبلغ (تومان) و جهت تبدیل می‌کند؛ اگر مبلغ یا جهت
 * قابل تشخیص نباشد null برمی‌گرداند تا چیزی حدسی ثبت نشود.
 *
 * - مبلغ: اولین عدد همراه با «تومان»/«ریال» که بعد از «مانده»/«موجودی» نیامده باشد.
 * - ریال به تومان تبدیل می‌شود (تقسیم بر ۱۰).
 * - جهت: کلمات هزینه اول بررسی می‌شوند، بعد کلمات درآمد.
 */
fun parseBankSms(sender: String, body: String): ParsedSms? {
    val normalized = normalizeSmsDigits(body)
    val balanceIndex = listOf("مانده", "موجودی")
        .mapNotNull { word -> normalized.indexOf(word).takeIf { it >= 0 } }
        .minOrNull()
    // فقط مبلغی که بلافاصله بعد از «مانده»/«موجودی» آمده، موجودی است و
    // کنار گذاشته می‌شود؛ مبلغ تراکنشِ بعد از آن همچنان خوانده می‌شود.
    val amountMatch = AMOUNT_REGEX.findAll(normalized)
        .firstOrNull { match ->
            balanceIndex == null ||
                match.range.first < balanceIndex ||
                match.range.first >= balanceIndex + 40
        } ?: return null
    val rawAmount = amountMatch.groupValues[1].replace(",", "").toLongOrNull()
        ?: return null
    if (rawAmount <= 0L) return null
    val amount = if (amountMatch.groupValues[2] == "ریال") rawAmount / 10 else rawAmount
    if (amount <= 0L) return null
    val income = when {
        EXPENSE_WORDS.any { normalized.contains(it) } -> false
        INCOME_WORDS.any { normalized.contains(it) } -> true
        else -> return null
    }
    return ParsedSms(amount = amount, income = income, sender = sender.trim())
}
