package com.polino.app.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDuplicateCleanupTest {

    private fun bankTx(id: String, amount: Long, income: Boolean, dateMillis: Long): MoneyTransaction =
        MoneyTransaction(
            id = id,
            title = "تراکنش بانکی",
            amount = amount,
            income = income,
            category = "سایر",
            dateMillis = dateMillis
        )

    private fun atDay(day: Int, hour: Int, minute: Int): Long =
        LocalDate.of(2026, 11, day).atTime(hour, minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun cleanup_keepsEarliestAndRemovesTheRest() {
        val txs = listOf(
            bankTx("a", 128000L, false, atDay(6, 10, 5)),
            bankTx("b", 128000L, false, atDay(6, 10, 2)),
            bankTx("c", 128000L, false, atDay(6, 10, 9)),
            bankTx("d", 128000L, false, atDay(6, 10, 7))
        )
        assertEquals(listOf("a", "c", "d"), findDuplicateSmsTransactionIds(txs).sorted())
    }

    @Test
    fun cleanup_sameTimestampKeepsSmallestId() {
        val txs = listOf(
            bankTx("z", 128000L, false, atDay(6, 10, 2)),
            bankTx("a", 128000L, false, atDay(6, 10, 2))
        )
        assertEquals(listOf("z"), findDuplicateSmsTransactionIds(txs))
    }

    @Test
    fun cleanup_differentAmountsAreUntouched() {
        val txs = listOf(
            bankTx("a", 128000L, false, atDay(6, 10, 2)),
            bankTx("b", 129000L, false, atDay(6, 10, 2))
        )
        assertTrue(findDuplicateSmsTransactionIds(txs).isEmpty())
    }

    @Test
    fun cleanup_differentDaysAreUntouched() {
        val txs = listOf(
            bankTx("a", 128000L, false, atDay(6, 10, 2)),
            bankTx("b", 128000L, false, atDay(7, 10, 2))
        )
        assertTrue(findDuplicateSmsTransactionIds(txs).isEmpty())
    }

    @Test
    fun cleanup_differentDirectionIsUntouched() {
        val txs = listOf(
            bankTx("a", 128000L, false, atDay(6, 10, 2)),
            bankTx("b", 128000L, true, atDay(6, 10, 2))
        )
        assertTrue(findDuplicateSmsTransactionIds(txs).isEmpty())
    }

    @Test
    fun cleanup_manualTransactionsAreUntouched() {
        val txs = listOf(
            bankTx("a", 128000L, false, atDay(6, 10, 2)),
            MoneyTransaction(
                id = "m",
                title = "خرید",
                amount = 128000L,
                income = false,
                category = "سایر",
                dateMillis = atDay(6, 10, 2)
            )
        )
        assertTrue(findDuplicateSmsTransactionIds(txs).isEmpty())
    }

    @Test
    fun cleanup_emptyAndSingleAreUntouched() {
        assertTrue(findDuplicateSmsTransactionIds(emptyList()).isEmpty())
        assertTrue(
            findDuplicateSmsTransactionIds(
                listOf(bankTx("a", 128000L, false, atDay(6, 10, 2)))
            ).isEmpty()
        )
    }

    @Test
    fun cleanup_twoGroupsCleanedIndependently() {
        val txs = listOf(
            bankTx("a1", 128000L, false, atDay(6, 9, 0)),
            bankTx("a2", 128000L, false, atDay(6, 9, 30)),
            bankTx("b1", 50000L, true, atDay(6, 11, 0)),
            bankTx("b2", 50000L, true, atDay(6, 11, 15))
        )
        assertEquals(listOf("a2", "b2"), findDuplicateSmsTransactionIds(txs).sorted())
    }
}
