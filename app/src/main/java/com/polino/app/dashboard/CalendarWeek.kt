package com.polino.app.dashboard

import java.time.DayOfWeek

/**
 * محاسبات هفته شمسی برای تقویم: هفته از شنبه آغاز می‌شود و جمعه روز پایانی آن است.
 * این توابع خالص‌اند (فقط java.time) تا در تست‌های واحد JVM هم بدون استاب اندروید اجرا شوند.
 */

/** شاخص روز هفته: شنبه ۰ تا جمعه ۶. */
internal fun jalaliWeekdayIndex(jy: Int, jm: Int, jd: Int): Int =
    (JalaliDate.jalaliToLocalDate(jy, jm, jd).dayOfWeek.value + 1) % 7

/** آیا این روز شمسی جمعه است؟ */
internal fun isJalaliFriday(jy: Int, jm: Int, jd: Int): Boolean =
    JalaliDate.jalaliToLocalDate(jy, jm, jd).dayOfWeek == DayOfWeek.FRIDAY

/** جابه‌جایی روز شمسی به اندازه delta روز (مثبت یا منفی؛ گذر از مرز ماه و سال مجاز است). */
internal fun jalaliPlusDays(jy: Int, jm: Int, jd: Int, delta: Int): Triple<Int, Int, Int> {
    val local = JalaliDate.jalaliToLocalDate(jy, jm, jd).plusDays(delta.toLong())
    return JalaliDate.gregorianToJalali(local.year, local.monthValue, local.dayOfMonth)
}

/** شنبه آغاز هفته‌ای که روز داده‌شده در آن قرار دارد. */
internal fun jalaliWeekStart(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> =
    jalaliPlusDays(jy, jm, jd, -jalaliWeekdayIndex(jy, jm, jd))

/** هفت روز هفته (شنبه تا جمعه) شامل روز داده‌شده. */
internal fun jalaliWeekDays(jy: Int, jm: Int, jd: Int): List<Triple<Int, Int, Int>> {
    val start = jalaliWeekStart(jy, jm, jd)
    return (0..6).map { jalaliPlusDays(start.first, start.second, start.third, it) }
}

/** قالب «سال-ماه» سازگار با currentJalaliYearMonth (مثل «۱۴۰۵-۰۸» به شکل 1405-08). */
internal fun jalaliYearMonthString(jy: Int, jm: Int): String =
    "$jy-${jm.toString().padStart(2, '0')}"
