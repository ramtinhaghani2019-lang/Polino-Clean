package com.polino.app.dashboard

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * تبدیل تاریخ میلادی به شمسی بدون وابستگی خارجی.
 *
 * الگوریتم تقویم جلالی (محاسبه روز ژولیوسی) که با ۱۲ تاریخ مرجع
 * شامل مرزهای سال کبیسه (۱۴۰۳ کبیسه، ۱۴۰۴ غیرکبیسه) راستی‌آزمایی شده است.
 * جایگزین android.icu.util.PersianCalendar شد چون آن کلاس در SDK کامپایل
 * این پروژه در دسترس نیست و بیلد مرحله ۲ را با «Unresolved reference» رد کرد.
 */
internal object JalaliDate {

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private val MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    /** خروجی: Triple(سال شمسی، ماه ۱ تا ۱۲، روز). */
    internal fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> =
        julianToJalali(gregorianToJulian(gy, gm, gd))

    internal fun formatJalali(millis: Long): String {
        if (millis == 0L) return "بدون تاریخ"
        val date = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val (jy, jm, jd) = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
        return "$jd ${MONTHS[jm - 1]} $jy"
    }

    /** ماه شمسی یک لحظه، با قالب «۱۴۰۵-۰۸»؛ کلید بودجه‌ها و گزارش ماهانه. */
    internal fun jalaliYearMonthOf(millis: Long): String {
        val date = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val (jy, jm, _) = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
        return "%04d-%02d".format(Locale.US, jy, jm)
    }

    internal fun currentJalaliYearMonth(): String {
        val now = LocalDate.now()
        val (jy, jm, _) = gregorianToJalali(now.year, now.monthValue, now.dayOfMonth)
        return "%04d-%02d".format(Locale.US, jy, jm)
    }

    /** «۱۴۰۵-۰۸» را به «آبان ۱۴۰۵» تبدیل می‌کند. */
    internal fun jalaliMonthDisplay(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return yearMonth
        val jy = parts[0].toIntOrNull() ?: return yearMonth
        val jm = parts[1].toIntOrNull() ?: return yearMonth
        if (jm !in 1..12) return yearMonth
        return "${MONTHS[jm - 1]} $jy"
    }

    /** ماه شمسی قبل از ماه داده‌شده؛ «۱۴۰۵-۰۱» → «۱۴۰۴-۱۲». */
    internal fun previousJalaliYearMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return yearMonth
        var jy = parts[0].toIntOrNull() ?: return yearMonth
        var jm = parts[1].toIntOrNull() ?: return yearMonth
        jm--
        if (jm < 1) {
            jm = 12
            jy--
        }
        return "%04d-%02d".format(Locale.US, jy, jm)
    }

    /** فهرست n ماه اخیر شمسی، از قدیمی به جدید، شامل ماه جاری. */
    internal fun lastJalaliYearMonths(count: Int): List<String> {
        val months = ArrayList<String>()
        var current = currentJalaliYearMonth()
        repeat(count) {
            months.add(0, current)
            current = previousJalaliYearMonth(current)
        }
        return months
    }

    /** خروجی: Triple(کبیسه بودن ۰ یا ۱، سال میلادی متناظر، روز مارس آغاز فروردین). */
    private fun jalCal(jy: Int): Triple<Int, Int, Int> {
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        val gy = jy + 621
        var i = 1
        while (i < BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += jump / 33 * 8 + (jump % 33) / 4
            jp = jm
            i++
        }
        var n = jy - jp
        leapJ += n / 33 * 8 + (n % 33 + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ++
        val leapG = gy / 4 - (gy / 100 + 1) * 3 / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + (jump + 4) / 33 * 33
        var leap = (n + 1) % 33 - 1
        leap %= 4
        if (leap == -1) leap = 4
        return Triple(leap, gy, march)
    }

    private fun gregorianToJulian(gy: Int, gm: Int, gd: Int): Int {
        var d = (gy + (gm - 8) / 6 + 100100) * 1461 / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d = d - (gy + 100100 + (gm - 8) / 6) / 100 * 3 / 4 + 752
        return d
    }

    private fun julianToGregorian(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += (4 * jdn + 183187720) / 146097 * 3 / 4 * 4 - 3908
        val i = (j % 1461) / 4 * 5 + 308
        val gd = (i % 153) / 5 + 1
        val gm = (i / 153) % 12 + 1
        val gy = j / 1461 - 100100 + (8 - gm) / 6
        return Triple(gy, gm, gd)
    }

    private fun julianToJalali(jdn: Int): Triple<Int, Int, Int> {
        val gy = julianToGregorian(jdn).first
        var jy = gy - 621
        val (leap, _, march) = jalCal(jy)
        val jdn1f = gregorianToJulian(gy, 3, march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return Triple(jy, 1 + k / 31, k % 31 + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (leap == 1) k += 1
        }
        return Triple(jy, 7 + k / 30, k % 30 + 1)
    }
}
