package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class MonthlySummaryTest {

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDateTime.of(year, month, day, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun tx(
        title: String,
        amount: Long,
        income: Boolean,
        category: String,
        dateMillis: Long
    ): MoneyTransaction =
        MoneyTransaction("id-$title", title, amount, income, category, dateMillis)

    @Test
    fun currentMonthFiltersAndTotalsCorrect() {
        val list = listOf(
            tx("a", 100000, false, "خوراک", millis(2026, 11, 3)),
            tx("b", 50000, false, "مسکن", millis(2026, 10, 23)),
            tx("c", 2000000, true, "حقوق", millis(2026, 11, 10)),
            tx("d", 30000, false, "خوراک", millis(2026, 10, 22)),
            tx("e", 70000, false, "سایر", 0L)
        )
        val month = transactionsInMonth(list, "1405-08")
        assertEquals(3, month.size)
        val totals = monthTotals(month)
        assertEquals(2000000L, totals.income)
        assertEquals(150000L, totals.expense)
        val spent = spentByCategory(month)
        assertEquals(listOf("خوراک" to 100000L, "مسکن" to 50000L), spent)
    }
}
