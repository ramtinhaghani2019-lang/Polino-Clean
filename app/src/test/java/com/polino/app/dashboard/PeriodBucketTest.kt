package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PeriodBucketTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun tx(
        title: String,
        amount: Long,
        income: Boolean,
        dateMillis: Long
    ): MoneyTransaction =
        MoneyTransaction("id-$title", title, amount, income, "سایر", dateMillis)

    @Test
    fun hourBuckets_groupByFourHourRangesAndIgnoreOtherDays() {
        val day = millis(2026, 11, 4)
        val list = listOf(
            tx("morning", 100000, true, millis(2026, 11, 4, 8, 30)),
            tx("night", 50000, false, millis(2026, 11, 4, 23, 15)),
            tx("other-day", 999000, true, millis(2026, 11, 5, 9, 0))
        )
        val buckets = bucketByHourOfDay(list, day)
        assertEquals(6, buckets.size)
        assertEquals(PeriodBucket("۸ تا ۱۲", 100000L, 0L), buckets[2])
        assertEquals(PeriodBucket("۲۰ تا ۲۴", 0L, 50000L), buckets[5])
        assertEquals(PeriodBucket("۰ تا ۴", 0L, 0L), buckets[0])
    }

    @Test
    fun weekdayBuckets_saturdayFirstAndIgnoreOutsideWeek() {
        val ref = millis(2026, 11, 4)
        val weekStart = jalaliWeekStartMillis(ref)
        val day = 24L * 60 * 60 * 1000
        val list = listOf(
            tx("sat", 200000, true, weekStart),
            tx("wed", 70000, false, weekStart + 4 * day),
            tx("next-week", 999000, true, weekStart + 7 * day)
        )
        val buckets = bucketByWeekday(list, weekStart)
        assertEquals(7, buckets.size)
        assertEquals(PeriodBucket("شنبه", 200000L, 0L), buckets[0])
        assertEquals(PeriodBucket("چهارشنبه", 0L, 70000L), buckets[4])
        assertEquals(PeriodBucket("جمعه", 0L, 0L), buckets[6])
    }

    @Test
    fun dayOfMonthBuckets_coverWholeMonthAndIgnoreOtherMonths() {
        val ref = millis(2026, 11, 4)
        val (jy, jm, _) = jalaliTripleOf(ref)
        val yearMonth = "%04d-%02d".format(java.util.Locale.US, jy, jm)
        val list = listOf(
            tx("first", 300000, true, JalaliDate.jalaliToMillis(jy, jm, 1)),
            tx("mid", 80000, false, JalaliDate.jalaliToMillis(jy, jm, 15)),
            tx("other-month", 999000, true, JalaliDate.jalaliToMillis(jy, jm + 1, 1))
        )
        val buckets = bucketByDayOfMonth(list, yearMonth)
        assertEquals(JalaliDate.daysInJalaliMonth(jy, jm), buckets.size)
        assertEquals(PeriodBucket("۱", 300000L, 0L), buckets[0])
        assertEquals(PeriodBucket("۱۵", 0L, 80000L), buckets[14])
    }

    @Test
    fun jalaliMonthBuckets_coverWholeYearAndIgnoreOtherYears() {
        val ref = millis(2026, 11, 4)
        val (jy, jm, _) = jalaliTripleOf(ref)
        val nextMonth = if (jm < 12) jm + 1 else 1
        val nextYear = if (jm < 12) jy else jy + 1
        val list = listOf(
            tx("this", 400000, true, JalaliDate.jalaliToMillis(jy, jm, 10)),
            tx("next", 90000, false, JalaliDate.jalaliToMillis(nextYear, nextMonth, 5)),
            tx("other-year", 999000, true, JalaliDate.jalaliToMillis(jy + 1, jm, 10))
        )
        val buckets = bucketByJalaliMonth(list, jy)
        assertEquals(12, buckets.size)
        val thisName = JalaliDate.jalaliMonthDisplay(
            "%04d-%02d".format(java.util.Locale.US, jy, jm)
        ).substringBefore(" ")
        assertEquals(PeriodBucket(thisName, 400000L, 0L), buckets[jm - 1])
    }

    @Test
    fun shiftJalaliYear_movesYearKeepingMonthAndDay() {
        val ref = millis(2026, 11, 4)
        val (jy, jm, jd) = jalaliTripleOf(ref)
        val shifted = shiftJalaliYear(ref, 1)
        assertEquals(Triple(jy + 1, jm, jd), jalaliTripleOf(shifted))
    }

    @Test
    fun shiftJalaliYear_clampsEsfand30To29InNonLeapYear() {
        val leapYear = (1400..1410).first { JalaliDate.daysInJalaliMonth(it, 12) == 30 }
        val ref = JalaliDate.jalaliToMillis(leapYear, 12, 30)
        val shifted = shiftJalaliYear(ref, 1)
        val (jy, jm, jd) = jalaliTripleOf(shifted)
        assertEquals(leapYear + 1, jy)
        assertEquals(12, jm)
        assertEquals(JalaliDate.daysInJalaliMonth(leapYear + 1, 12), jd)
    }
}
