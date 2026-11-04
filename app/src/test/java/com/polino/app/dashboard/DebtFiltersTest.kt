package com.polino.app.dashboard

import com.polino.app.data.DebtEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtFiltersTest {

    private fun debt(
        person: String,
        amount: Long,
        receivable: Boolean,
        settled: Boolean = false,
        due: Long? = null
    ) = DebtEntity(
        id = "$person-$amount-$receivable",
        personName = person,
        amount = amount,
        isReceivable = receivable,
        settled = settled,
        dueMillis = due
    )

    @Test
    fun tabFilterKeepsOnlyMatchingDirection() {
        val debts = listOf(
            debt("علی", 100_000L, true),
            debt("رضا", 50_000L, false),
            debt("سارا", 200_000L, true)
        )
        assertEquals(
            listOf("علی", "سارا"),
            filterDebts(debts, "", true).map { it.personName }
        )
        assertEquals(
            listOf("رضا"),
            filterDebts(debts, "", false).map { it.personName }
        )
    }

    @Test
    fun nameSearchMatchesSubstring() {
        val debts = listOf(
            debt("علی محمدی", 100_000L, true),
            debt("رضا", 50_000L, true)
        )
        assertEquals(
            listOf("علی محمدی"),
            filterDebts(debts, "محمد", true).map { it.personName }
        )
    }

    @Test
    fun amountSearchMatchesLatinAndPersianDigits() {
        val debts = listOf(
            debt("علی", 250_000L, true),
            debt("رضا", 50_000L, true)
        )
        assertEquals(
            listOf("علی"),
            filterDebts(debts, "250", true).map { it.personName }
        )
        assertEquals(
            listOf("علی"),
            filterDebts(debts, "۲۵۰", true).map { it.personName }
        )
    }

    @Test
    fun settledDebtsStayInTheList() {
        val debts = listOf(debt("علی", 100_000L, true, settled = true))
        assertEquals(1, filterDebts(debts, "", true).size)
    }

    @Test
    fun dueTodayWindow() {
        val now = 1_760_000_000_000L
        val startOfToday = now - Math.floorMod(now, 86_400_000L)
        assertTrue(isDebtDueToday(startOfToday + 1_000L, now))
        assertTrue(isDebtDueToday(startOfToday, now))
        assertFalse(isDebtDueToday(startOfToday - 1_000L, now))
        assertFalse(isDebtDueToday(startOfToday + 86_400_000L, now))
        assertFalse(isDebtDueToday(null, now))
    }
}
