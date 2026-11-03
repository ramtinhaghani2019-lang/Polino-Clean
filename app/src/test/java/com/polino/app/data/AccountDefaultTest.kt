package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountDefaultTest {

    @Test
    fun legacyTransactions_defaultToMainAccount() {
        val parsed = parseLegacyTransactions(
            "[{\"title\":\"خرید\",\"amount\":50000,\"income\":false}]"
        )
        assertEquals(1, parsed.size)
        assertEquals("main", parsed[0].accountId)
    }

    @Test
    fun backupJson_preservesAccountId() {
        val original = MoneyTransaction(
            "id-1", "حقوق", 1000000, true, "حقوق", 0L, "wallet-2"
        )
        val parsed = parseLegacyTransactions(transactionsToJson(listOf(original)))
        assertEquals(listOf(original), parsed)
    }

    @Test
    fun entityRoundTrip_preservesAccountId() {
        val original = MoneyTransaction(
            "id-1", "حقوق", 1000000, true, "حقوق", 0L, "wallet-2"
        )
        assertEquals(original, original.toEntity().toDomain())
    }
}
