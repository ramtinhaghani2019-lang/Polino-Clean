package com.polino.app.dashboard

/**
 * وضعیت سررسید یک یادآور چک/قبض نسبت به امروز؛ سررسیدِ امروز هنوز
 * سررسیدگذشته نیست، چون سررسید تاریخ است نه ساعت (همان قرارداد
 * [com.polino.app.data.isChequeReminderOverdue]).
 */
internal enum class ChequeDueStatus {
    SETTLED,
    OVERDUE,
    DUE_TODAY,
    UPCOMING
}

/** وضعیت سررسید یادآور؛ ورودی‌ها فقط عددند تا در تست JVM هم اجرا شود. */
internal fun chequeDueStatus(dueMillis: Long, nowMillis: Long, settled: Boolean): ChequeDueStatus {
    if (settled) return ChequeDueStatus.SETTLED
    val startOfToday = nowMillis - Math.floorMod(nowMillis, 86_400_000L)
    return when {
        dueMillis < startOfToday -> ChequeDueStatus.OVERDUE
        dueMillis < startOfToday + 86_400_000L -> ChequeDueStatus.DUE_TODAY
        else -> ChequeDueStatus.UPCOMING
    }
}
