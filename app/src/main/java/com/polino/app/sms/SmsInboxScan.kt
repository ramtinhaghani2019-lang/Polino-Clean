package com.polino.app.sms

import android.content.Context
import android.net.Uri
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * یک ردیف از صندوق پیامک دستگاه؛ فقط برای اسکن و شمارش فرستنده‌ها
 * خوانده می‌شود. متن پیامک فقط در لحظه خواندن برای استخراج مبلغ و جهت
 * استفاده می‌شود و هرگز به‌عنوان یادداشت تراکنش ذخیره نمی‌شود.
 */
data class InboxSmsRow(
    val sender: String,
    val body: String,
    val timestampMillis: Long
)

/** فرستنده شناسایی‌شده در صندوق پیامک + تعداد پیامک‌هایش. */
data class DetectedSender(
    val sender: String,
    val count: Int
)

/**
 * فرستنده‌های صندوق را می‌شمارد: فرستنده خالی نادیده گرفته می‌شود،
 * بقیه بر اساس فرستنده (پس از trim) گروه‌بندی می‌شوند و خروجی اول بر
 * اساس تعداد نزولی و بعد نام فرستنده مرتب می‌شود.
 */
fun detectSenders(rows: List<InboxSmsRow>): List<DetectedSender> {
    return rows
        .map { it.sender.trim() }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .map { (sender, count) -> DetectedSender(sender, count) }
        .sortedWith(compareByDescending<DetectedSender> { it.count }.thenBy { it.sender })
}

/**
 * پنجره تکراری پیامک (۵ دقیقه): دو پیامک با همان مبلغ، جهت و حساب
 * مقصد که زمانشان در این پنجره باشد همان تراکنش‌اند. پنجره به‌جای
 * برابری دقیق میلی‌ثانیه است چون زمان پیامک در دو مسیر یکی نیست:
 * دریافت زنده زمان PDU را می‌بیند و اسکن صندوق ستون date را؛
 * اختلافشان معمولاً چند ثانیه تا یکی‌دو دقیقه است. بخش‌های یک پیامک
 * چندپاره یا تحویل دوباره همان پیامک هم زمان یکسان یا نزدیک دارند.
 * دو تراکنش واقعاً جدا با همان مبلغ که بیش از ۵ دقیقه از هم فاصله
 * دارند تکراری شمرده نمی‌شوند.
 */
internal const val SMS_DUPLICATE_WINDOW_MILLIS = 5 * 60 * 1000L

/**
 * همان قاعده تکراریِ دریافت زنده (SmsReceiver): پیامکی تکراری است که
 * تراکنشی با همان عنوان «تراکنش بانکی»، همان مبلغ، همان جهت، همان
 * حساب مقصد و زمانی در پنجره تکراری در داده‌ها باشد. اسکن مجدد و
 * دریافت زنده هر دو همین تابع را صدا می‌زنند تا دو قاعده متفاوت
 * برای تکراری به وجود نیاید.
 */
internal fun isSmsDuplicate(
    existing: List<MoneyTransaction>,
    amount: Long,
    income: Boolean,
    accountId: String,
    timestampMillis: Long
): Boolean {
    return existing.any {
        it.title == "تراکنش بانکی" &&
            it.amount == amount &&
            it.income == income &&
            it.accountId == accountId &&
            kotlin.math.abs(it.dateMillis - timestampMillis) <= SMS_DUPLICATE_WINDOW_MILLIS
    }
}

/**
 * صندوق پیامک دستگاه را می‌خواند. باید با دسترسی READ_SMS و خارج از
 * نخ اصلی صدا زده شود؛ در صورت هر خطایی (مثلاً نبود برنامه پیامک)
 * فهرست خالی برمی‌گرداند تا اسکن برنامه را از کار نیندازد.
 */
suspend fun scanInboxSms(context: Context): List<InboxSmsRow> {
    return withContext(Dispatchers.IO) {
        runCatching {
            val rows = mutableListOf<InboxSmsRow>()
            context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null,
                null,
                null
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex("address")
                val bodyIndex = cursor.getColumnIndex("body")
                val dateIndex = cursor.getColumnIndex("date")
                while (cursor.moveToNext()) {
                    val sender = if (addressIndex >= 0) cursor.getString(addressIndex) ?: "" else ""
                    val body = if (bodyIndex >= 0) cursor.getString(bodyIndex) ?: "" else ""
                    val timestamp = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L
                    rows.add(InboxSmsRow(sender, body, timestamp))
                }
            }
            rows
        }.getOrDefault(emptyList())
    }
}

/**
 * صندوق پیامک را دوباره اسکن می‌کند و فقط پیامک‌هایی را ثبت می‌کند که
 * قاعده تکراری آن‌ها را تازه بداند؛ دقیقاً همان مسیر دریافت زنده:
 * الگوها اول بررسی می‌شوند، مبلغ و جهت از همان پارسِر خوانده می‌شود
 * و حساب مقصد از همان اتصال‌های حساب↔فرستنده حل می‌شود. تعداد
 * تراکنش‌های تازه‌ثبت‌شده برمی‌گردد.
 */
internal suspend fun rescanInboxSms(context: Context, repository: TransactionRepository): Int {
    val rows = scanInboxSms(context)
    if (rows.isEmpty()) return 0
    val existing = runCatching { repository.getAll() }.getOrDefault(emptyList()).toMutableList()
    val patterns = runCatching { repository.getPatterns() }.getOrDefault(emptyList())
    val smsLinks = runCatching { repository.getAccountSmsLinks() }.getOrDefault(emptyList())
    var inserted = 0
    rows.forEach { row ->
        val pattern = findMatchingPattern(patterns, row.sender, row.body)
        if (!shouldRecordSms(pattern)) return@forEach
        val parsed = parseBankSms(row.sender, row.body)
        val amount = (if (pattern != null) parseSmsAmount(row.body) else parsed?.amount)
            ?: return@forEach
        val income = resolveSmsIncome(pattern, parsed?.income ?: false)
        val accountId = resolveSmsAccountId(smsLinks, row.sender)
        if (isSmsDuplicate(existing, amount, income, accountId, row.timestampMillis)) return@forEach
        val transaction = MoneyTransaction(
            id = UUID.randomUUID().toString(),
            title = "تراکنش بانکی",
            amount = amount,
            income = income,
            category = "سایر",
            dateMillis = row.timestampMillis,
            accountId = accountId,
            note = ""
        )
        repository.upsert(transaction)
        existing.add(transaction)
        inserted++
    }
    return inserted
}
