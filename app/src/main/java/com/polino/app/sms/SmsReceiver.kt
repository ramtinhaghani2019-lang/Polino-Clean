package com.polino.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * پیامک‌های بانکی دریافتی را — فقط وقتی کاربر خوانش پیامک را فعال کرده باشد —
 * به تراکنش تبدیل می‌کند. پیامک بدون مبلغ یا جهت قابل تشخیص نادیده گرفته
 * می‌شود و پیامک تکراری (همان فرستنده، مبلغ و زمان) دوباره ثبت نمی‌شود.
 * متن پیامک ذخیره نمی‌شود؛ یادداشت تراکنشِ ثبت‌شده از پیامک خالی می‌ماند.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = context.getSharedPreferences(SMS_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(SMS_ENABLED_KEY, false)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = TransactionRepository(context)
                val existing = runCatching { repository.getAll() }.getOrDefault(emptyList()).toMutableList()
                val patterns = runCatching { repository.getPatterns() }.getOrDefault(emptyList())
                val smsLinks = runCatching { repository.getAccountSmsLinks() }.getOrDefault(emptyList())
                Telephony.Sms.Intents.getMessagesFromIntent(intent).forEach { sms ->
                    val body = sms.messageBody ?: return@forEach
                    val originator = sms.originatingAddress ?: ""
                    // الگوها اول بررسی می‌شوند: الگوی غیرمالی جلوی ثبت را
                    // می‌گیرد و الگوی مالی جهت را تعیین می‌کند.
                    val pattern = findMatchingPattern(patterns, originator, body)
                    if (!shouldRecordSms(pattern)) return@forEach
                    val parsed = parseBankSms(originator, body)
                    val amount = (if (pattern != null) parseSmsAmount(body) else parsed?.amount)
                        ?: return@forEach
                    val income = resolveSmsIncome(pattern, parsed?.income ?: false)
                    val accountId = resolveSmsAccountId(smsLinks, originator)
                    if (isSmsDuplicate(existing, amount, income, accountId, sms.timestampMillis)) return@forEach
                    val transaction = MoneyTransaction(
                        id = UUID.randomUUID().toString(),
                        title = "تراکنش بانکی",
                        amount = amount,
                        income = income,
                        category = "سایر",
                        dateMillis = sms.timestampMillis,
                        accountId = accountId,
                        note = ""
                    )
                    repository.upsert(transaction)
                    // فهرستِ در حافظه هم به‌روز می‌شود تا بخش بعدیِ همین
                    // پیامک چندپاره یا تحویل دوباره همان پیامک، تکراری
                    // شناخته شود و دوباره ثبت نشود (عین اسکن مجدد).
                    existing.add(transaction)
                }
            } catch (_: Exception) {
                // خطای پیامک نباید برنامه را از کار بیندازد؛ پیامک بعدی دوباره تلاش می‌شود.
            } finally {
                pending.finish()
            }
        }
    }
}
