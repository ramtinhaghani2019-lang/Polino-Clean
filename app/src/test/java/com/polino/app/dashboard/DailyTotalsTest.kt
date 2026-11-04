package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyTotalsTest {

    private fun tx(title: String, amount: Long, income: Boolean, day: Int): MoneyTransaction =
        MoneyTransaction(
            "id-$title",
            title,
            amount,
            income,
            "سایر",
            JalaliDate.jalaliToMillis(1405, 8, day)
        )

    @Test
    fun sumsSplitByDayAndDirection() {
        val list = listOf(
            tx("a", 100000, false, 13),
            tx("b", 50000, false, 13),
            tx("c", 2000000, true, 13),
            tx("d", 70000, true, 20)
        )
        val days = dailyTotals(list, "1405-08")
        assertEquals(30, days.size)
        assertEquals(DayTotal(13, 2000000L, 150000L), days[12])
        assertEquals(DayTotal(20, 70000L, 0L), days[19])
        assertEquals(DayTotal(1, 0L, 0L), days[0])
    }

    @Test
    fun otherMonthAndDatelessIgnored() {
        val other = MoneyTransaction(
            "id-other", "other", 900000, true, "حقوق",
            JalaliDate.jalaliToMillis(1405, 7, 13)
        )
        val dateless = MoneyTransaction("id-none", "none", 800000, false, "سایر", 0L)
        val days = dailyTotals(listOf(other, dateless), "1405-08")
        assertEquals(30, days.size)
        assertEquals(true, days.all { it.income == 0L && it.expense == 0L })
    }

    @Test
    fun invalidMonthReturnsEmpty() {
        assertEquals(emptyList<DayTotal>(), dailyTotals(emptyList(), "bogus"))
    }
}
