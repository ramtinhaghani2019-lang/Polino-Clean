package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodHelpersTest {

    @Test
    fun weekStart_ofTuesday_isSaturday() {
        val tuesday = JalaliDate.jalaliToMillis(1405, 8, 12)
        assertEquals(
            Triple(1405, 8, 9),
            jalaliTripleOf(jalaliWeekStartMillis(tuesday))
        )
    }

    @Test
    fun weekStart_ofSaturday_isItself() {
        val saturday = JalaliDate.jalaliToMillis(1405, 8, 9)
        assertEquals(saturday, jalaliWeekStartMillis(saturday))
    }

    @Test
    fun sameJalaliDay_ignoresTimeOfDay() {
        val morning = JalaliDate.jalaliToMillis(1405, 8, 12)
        val evening = morning + 10L * 60 * 60 * 1000
        assertTrue(isSameJalaliDay(morning, evening))
        assertFalse(isSameJalaliDay(morning, morning + 24L * 60 * 60 * 1000))
    }

    @Test
    fun jalaliYearOf_matches() {
        assertEquals(1405, jalaliYearOf(JalaliDate.jalaliToMillis(1405, 8, 12)))
    }

    @Test
    fun periodFilters_splitCorrectly() {
        val now = JalaliDate.jalaliToMillis(1405, 8, 12)
        val list = listOf(
            MoneyTransaction("d1", "امروز", 100, false, "خوراک", now),
            MoneyTransaction("d2", "شنبه", 200, false, "خوراک", JalaliDate.jalaliToMillis(1405, 8, 9)),
            MoneyTransaction("d3", "هفته پیش", 300, false, "خوراک", JalaliDate.jalaliToMillis(1405, 8, 2)),
            MoneyTransaction("d4", "پارسال", 400, false, "خوراک", JalaliDate.jalaliToMillis(1404, 8, 12))
        )
        assertEquals(
            listOf("d1"),
            transactionsInPeriod(list, ReportPeriod.DAILY, now, "1405-08").map { it.id }
        )
        assertEquals(
            listOf("d1", "d2"),
            transactionsInPeriod(list, ReportPeriod.WEEKLY, now, "1405-08").map { it.id }
        )
        assertEquals(
            listOf("d1", "d2", "d3"),
            transactionsInPeriod(list, ReportPeriod.YEARLY, now, "1405-08").map { it.id }
        )
    }
}
