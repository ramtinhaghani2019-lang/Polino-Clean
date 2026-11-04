package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountBalanceTest {

    private fun tx(id: String, accountId: String, amount: Long, income: Boolean) =
        MoneyTransaction(
            id = id,
            title = "t",
            amount = amount,
            income = income,
            category = "c",
            dateMillis = 0L,
            accountId = accountId
        )

    @Test
    fun balanceSumsIncomeMinusExpenseForOneAccount() {
        val txs = listOf(
            tx("1", "a", 1000L, true),
            tx("2", "a", 400L, false),
            tx("3", "b", 9999L, true)
        )
        assertEquals(600L, accountBalance(txs, "a"))
    }

    @Test
    fun balanceIgnoresOtherAccounts() {
        val txs = listOf(
            tx("1", "b", 500L, true),
            tx("2", "b", 200L, false)
        )
        assertEquals(300L, accountBalance(txs, "b"))
        assertEquals(0L, accountBalance(txs, "a"))
    }

    @Test
    fun balanceOfEmptyListIsZero() {
        assertEquals(0L, accountBalance(emptyList(), "main"))
    }

    @Test
    fun balanceCanGoNegative() {
        val txs = listOf(tx("1", "a", 100L, false))
        assertEquals(-100L, accountBalance(txs, "a"))
    }
}
