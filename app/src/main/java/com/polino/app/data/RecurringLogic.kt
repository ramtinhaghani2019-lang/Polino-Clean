package com.polino.app.data

import java.util.Locale

/** ماه شمسی بعد از ماه داده‌شده؛ «۱۴۰۵-۱۲» → «۱۴۰۶-۰۱». */
internal fun nextJalaliYearMonth(yearMonth: String): String {
    val parts = yearMonth.split("-")
    if (parts.size != 2) return yearMonth
    var jy = parts[0].toIntOrNull() ?: return yearMonth
    var jm = parts[1].toIntOrNull() ?: return yearMonth
    jm++
    if (jm > 12) {
        jm = 1
        jy++
    }
    return "%04d-%02d".format(Locale.US, jy, jm)
}

/**
 * ماه‌هایی که برای یک قاعده تکرار باید ساخته شوند: از ماه شروع
 * (یا ماه بعد از آخرین ماه اعمال‌شده) تا ماه جاری. مقایسه رشته‌ای
 * برای قالب صفرپرشده «سال-ماه» درست کار می‌کند.
 */
internal fun dueYearMonths(
    startYearMonth: String,
    lastAppliedYearMonth: String?,
    currentYearMonth: String
): List<String> {
    val months = ArrayList<String>()
    var cursor = lastAppliedYearMonth?.let { nextJalaliYearMonth(it) } ?: startYearMonth
    while (cursor <= currentYearMonth) {
        months.add(cursor)
        if (cursor == currentYearMonth) break
        cursor = nextJalaliYearMonth(cursor)
    }
    return months
}
