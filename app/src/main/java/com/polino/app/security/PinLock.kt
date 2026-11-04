package com.polino.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** کلیدهای ذخیره پین در polino_prefs — فقط هش نمکی نگه داشته می‌شود، هرگز خود پین. */
const val PIN_HASH_KEY = "pin_hash"
const val PIN_SALT_KEY = "pin_salt"

private const val PIN_HASH_ITERATIONS = 1000

/** ارقام فارسی و عربی را به لاتین برمی‌گرداند؛ بقیه کاراکترها دست‌نخورده می‌مانند. */
fun normalizePinDigits(pin: String): String = pin.map { ch ->
    when (ch) {
        in '۰'..'۹' -> '0' + (ch - '۰')
        in '٠'..'٩' -> '0' + (ch - '٠')
        else -> ch
    }
}.joinToString("")

/** پین معتبر: ۴ تا ۶ رقم. */
fun isValidPin(pin: String): Boolean {
    val normalized = normalizePinDigits(pin)
    return normalized.length in 4..6 && normalized.all { it.isDigit() }
}

/** نمک تصادفی ۱۶ بایتی با Base64 — در تست JVM هم کار می‌کند (java.util.Base64). */
fun generatePinSalt(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return Base64.getEncoder().encodeToString(bytes)
}

/**
 * هش پین با نمک: SHA-256 تکرارشونده روی «نمک:پین»، خروجی هگز.
 * تابع خالص است؛ ورودی‌ها مصنوعی و قابل تست‌اند، هیچ داده کاربری خوانده نمی‌شود.
 */
fun hashPin(pin: String, salt: String): String {
    var digest = (salt + ":" + normalizePinDigits(pin)).toByteArray(Charsets.UTF_8)
    val md = MessageDigest.getInstance("SHA-256")
    repeat(PIN_HASH_ITERATIONS) { digest = md.digest(digest) }
    return digest.joinToString("") { "%02x".format(it) }
}

/** بررسی پین با مقایسه ثابت‌زمانه؛ نمک یا هش خالی همیشه نادرست است. */
fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
    if (salt.isBlank() || expectedHash.isBlank()) return false
    val actual = hashPin(pin, salt).toByteArray(Charsets.UTF_8)
    return MessageDigest.isEqual(actual, expectedHash.toByteArray(Charsets.UTF_8))
}
