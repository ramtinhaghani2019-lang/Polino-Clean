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
                val existing = runCatching { repository.getAll() }.getOrDefault(emptyList())
                Telephony.Sms.Intents.getMessagesFromIntent(intent).forEach { sms ->
                    val body = sms.messageBody ?: return@forEach
                    val parsed = parseBankSms(sms.originatingAddress ?: "", body)
                        ?: return@forEach
                    val duplicate = existing.any {
                        it.title == "تراکنش بانکی" &&
                            it.amount == parsed.amount &&
                            it.dateMillis == sms.timestampMillis
                    }
                    if (duplicate) return@forEach
                    repository.upsert(
                        MoneyTransaction(
                            id = UUID.randomUUID().toString(),
                            title = "تراکنش بانکی",
                            amount = parsed.amount,
                            income = parsed.income,
                            category = "سایر",
                            dateMillis = sms.timestampMillis,
                            accountId = "main",
                            note = body
                        )
                    )
                }
            } catch (_: Exception) {
                // خطای پیامک نباید برنامه را از کار بیندازد؛ پیامک بعدی دوباره تلاش می‌شود.
            } finally {
                pending.finish()
            }
        }
    }
}
