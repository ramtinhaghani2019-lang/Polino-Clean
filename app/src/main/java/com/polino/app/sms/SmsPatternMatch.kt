package com.polino.app.sms

import com.polino.app.data.SmsPatternEntity
import com.polino.app.data.smsPatternKeywords

/**
 * اولین الگویی که بر پیامک می‌نشیند: فرستنده الگو باید (بدون حساسیت به
 * بزرگی و کوچکی حروف) زیررشته‌ای از فرستنده پیامک باشد، و یا کلمات کلیدی
 * خالی باشند یا دست‌کم یکی از آن‌ها در متن پیامک آمده باشد.
 */
internal fun findMatchingPattern(
    patterns: List<SmsPatternEntity>,
    sender: String,
    body: String
): SmsPatternEntity? {
    val senderNorm = sender.trim().lowercase()
    if (senderNorm.isEmpty()) return null
    return patterns.firstOrNull { pattern ->
        val patternSender = pattern.sender.trim().lowercase()
        patternSender.isNotEmpty() && senderNorm.contains(patternSender) &&
            smsPatternKeywords(pattern.keywords).let { keywords ->
                keywords.isEmpty() || keywords.any { body.contains(it, ignoreCase = true) }
            }
    }
}

/** الگوی غیرمالی جلوی ثبت خودکار را می‌گیرد؛ بدون الگو همیشه ثبت می‌شود. */
internal fun shouldRecordSms(pattern: SmsPatternEntity?): Boolean =
    pattern == null || pattern.isFinancial

/** جهت الگو بر تشخیص پارس اولویت دارد؛ بدون الگو جهت پارس برمی‌گردد. */
internal fun resolveSmsIncome(pattern: SmsPatternEntity?, parsedIncome: Boolean): Boolean =
    pattern?.isIncome ?: parsedIncome
