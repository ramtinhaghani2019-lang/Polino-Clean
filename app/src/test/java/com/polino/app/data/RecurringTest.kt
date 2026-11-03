package com.polino.app.data

import com.polino.app.dashboard.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringTest {

    @Test
    fun jalaliToMillis_roundTripsThroughFormat() {
        assertEquals(
            "۱۲ آبان ۱۴۰۵",
            JalaliDate.formatJalali(JalaliDate.jalaliToMillis(1405, 8, 12))
        )
    }

    @Test
    fun jalaliToMillis_supportsLeapEsfandDay30() {
        // ۱۴۰۳ کبیسه است؛ ۳۰ اسفند آن روز معتبری است.
        assertEquals(
            "۳۰ اسفند ۱۴۰۳",
            JalaliDate.formatJalali(JalaliDate.jalaliToMillis(1403, 12, 30))
        )
    }

    @Test
    fun daysInJalaliMonth_matchesKnownCalendar() {
        assertEquals(31, JalaliDate.daysInJalaliMonth(1405, 1))
        assertEquals(30, JalaliDate.daysInJalaliMonth(1405, 7))
        assertEquals(30, JalaliDate.daysInJalaliMonth(1403, 12))
        assertEquals(29, JalaliDate.daysInJalaliMonth(1404, 12))
    }

    @Test
    fun isLeapJalaliYear_matchesKnownYears() {
        assertEquals(true, JalaliDate.isLeapJalaliYear(1403))
        assertEquals(false, JalaliDate.isLeapJalaliYear(1404))
    }

    @Test
    fun dueYearMonths_coversStartThroughCurrent() {
        assertEquals(
            listOf("1405-08", "1405-09", "1405-10"),
            dueYearMonths("1405-08", null, "1405-10")
        )
    }

    @Test
    fun dueYearMonths_resumesAfterLastApplied() {
        assertEquals(
            listOf("1405-10"),
            dueYearMonths("1405-08", "1405-09", "1405-10")
        )
        assertEquals(
            emptyList<String>(),
            dueYearMonths("1405-08", "1405-10", "1405-10")
        )
        assertEquals(
            emptyList<String>(),
            dueYearMonths("1405-11", null, "1405-10")
        )
    }

    @Test
    fun dueYearMonths_crossesYearBoundary() {
        assertEquals(
            listOf("1405-12", "1406-01"),
            dueYearMonths("1405-12", null, "1406-01")
        )
    }
}
