package com.polino.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class JalaliDateTest {

    @Test
    fun knownDates_convertCorrectly() {
        // تاریخ‌های مرجع شامل مرز سال کبیسه و غیرکبیسه
        assertEquals(Triple(1405, 8, 12), JalaliDate.gregorianToJalali(2026, 11, 3))
        assertEquals(Triple(1405, 1, 1), JalaliDate.gregorianToJalali(2026, 3, 21))
        assertEquals(Triple(1404, 12, 29), JalaliDate.gregorianToJalali(2026, 3, 20))
        assertEquals(Triple(1404, 1, 1), JalaliDate.gregorianToJalali(2025, 3, 21))
        assertEquals(Triple(1403, 12, 30), JalaliDate.gregorianToJalali(2025, 3, 20))
        assertEquals(Triple(1403, 1, 1), JalaliDate.gregorianToJalali(2024, 3, 20))
        assertEquals(Triple(1402, 12, 29), JalaliDate.gregorianToJalali(2024, 3, 19))
        assertEquals(Triple(1402, 1, 1), JalaliDate.gregorianToJalali(2023, 3, 21))
        assertEquals(Triple(1378, 10, 11), JalaliDate.gregorianToJalali(2000, 1, 1))
        assertEquals(Triple(1404, 10, 25), JalaliDate.gregorianToJalali(2026, 1, 15))
        assertEquals(Triple(1404, 10, 1), JalaliDate.gregorianToJalali(2025, 12, 22))
        assertEquals(Triple(1405, 8, 1), JalaliDate.gregorianToJalali(2026, 10, 23))
    }

    @Test
    fun legacyEntryWithoutDate_showsNoDate() {
        assertEquals("بدون تاریخ", JalaliDate.formatJalali(0L))
    }

    @Test
    fun previousMonth_rollsOverYear() {
        assertEquals("1405-07", JalaliDate.previousJalaliYearMonth("1405-08"))
        assertEquals("1404-12", JalaliDate.previousJalaliYearMonth("1405-01"))
        assertEquals("1405-08", JalaliDate.previousJalaliYearMonth("1405-09"))
    }

    @Test
    fun lastMonths_endingAtGivenMonth() {
        assertEquals(
            listOf("1405-06", "1405-07", "1405-08"),
            JalaliDate.lastJalaliYearMonths(3, "1405-08")
        )
    }

    @Test
    fun lastMonths_crossesYearBoundary() {
        assertEquals(
            listOf("1404-12", "1405-01"),
            JalaliDate.lastJalaliYearMonths(2, "1405-01")
        )
    }
}
