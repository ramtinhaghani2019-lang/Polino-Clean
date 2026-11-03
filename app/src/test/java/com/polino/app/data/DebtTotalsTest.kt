package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DebtTotalsTest {

    @Test
    fun unsettledTotals_ignoresSettled() {
        val debts = listOf(
            DebtEntity("1", "علی", 500000, true, false),
            DebtEntity("2", "رضا", 200000, true, true),
            DebtEntity("3", "سارا", 300000, false, false)
        )
        val (receivable, payable) = unsettledTotals(debts)
        assertEquals(500000L, receivable)
        assertEquals(300000L, payable)
    }

    @Test
    fun unsettledTotals_empty_returnsZeros() {
        assertEquals(Pair(0L, 0L), unsettledTotals(emptyList()))
    }
}
