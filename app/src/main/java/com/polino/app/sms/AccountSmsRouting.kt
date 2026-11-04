package com.polino.app.sms

import com.polino.app.data.AccountSmsSenderEntity

/**
 * حسابی که پیامک دریافتی باید به آن برود: اولین اتصالی که فرستنده‌اش
 * (بدون حساسیت به بزرگی و کوچکی حروف) زیررشته‌ای از فرستنده پیامک باشد
 * — همان قاعده تطابق فرستنده در الگوهای پیامک. اتصال خالی نادیده گرفته
 * می‌شود و وقتی هیچ اتصالی نمی‌نشیند، حساب پیش‌فرض «اصلی» برمی‌گردد.
 */
internal fun resolveSmsAccountId(
    links: List<AccountSmsSenderEntity>,
    originator: String,
    fallbackAccountId: String = "main"
): String {
    val originatorNorm = originator.trim().lowercase()
    if (originatorNorm.isEmpty()) return fallbackAccountId
    return links.firstOrNull { link ->
        val senderNorm = link.sender.trim().lowercase()
        senderNorm.isNotEmpty() && originatorNorm.contains(senderNorm)
    }?.accountId ?: fallbackAccountId
}
