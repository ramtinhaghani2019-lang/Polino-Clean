package com.polino.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarWeekTest {

    @Test
    fun friday_detectedOnlyOnFriday() {
        // لنگر از JalaliDateTest: ۱۲ آبان ۱۴۰۵ = سه‌شنبه ۳ نوامبر ۲۰۲۶، پس ۱۵ آبان = جمعه ۶ نوامبر.
        assertTrue(isJalaliFriday(1405, 8, 15))
        assertFalse(isJalaliFriday(1405, 8, 14))
        assertFalse(isJalaliFriday(1405, 8, 16))
    }

    @Test
    fun weekdayIndex_saturdayZeroFridaySix() {
        assertEquals(0, jalaliWeekdayIndex(1405, 8, 16))
        assertEquals(3, jalaliWeekdayIndex(1405, 8, 12))
        assertEquals(6, jalaliWeekdayIndex(1405, 8, 15))
    }

    @Test
    fun weekStart_saturdayOfContainingWeek() {
        assertEquals(Triple(1405, 8, 9), jalaliWeekStart(1405, 8, 13))
        assertEquals(Triple(1405, 8, 9), jalaliWeekStart(1405, 8, 15))
        assertEquals(Triple(1405, 8, 16), jalaliWeekStart(1405, 8, 16))
    }

    @Test
    fun weekDays_sevenDaysSaturdayToFriday() {
        val days = jalaliWeekDays(1405, 8, 13)
        assertEquals(7, days.size)
        assertEquals(Triple(1405, 8, 9), days.first())
        assertEquals(Triple(1405, 8, 15), days.last())
        assertEquals((0..6).toList(), days.map { jalaliWeekdayIndex(it.first, it.second, it.third) })
    }

    @Test
    fun plusDays_crossesMonthAndYear() {
        assertEquals(Triple(1405, 9, 1), jalaliPlusDays(1405, 8, 30, 1))
        assertEquals(Triple(1405, 8, 30), jalaliPlusDays(1405, 9, 1, -1))
        assertEquals(Triple(1405, 1, 1), jalaliPlusDays(1404, 12, 29, 1))
    }

    @Test
    fun yearMonthString_zeroPadsMonth() {
        assertEquals("1405-08", jalaliYearMonthString(1405, 8))
        assertEquals("1405-01", jalaliYearMonthString(1405, 1))
        assertEquals("1405-12", jalaliYearMonthString(1405, 12))
    }
}
