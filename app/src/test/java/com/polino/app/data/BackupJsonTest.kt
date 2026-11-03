package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonTest {

    @Test
    fun roundTrip_preservesAllFields() {
        val original = listOf(
            MoneyTransaction("id-1", "گفت \"سلام\"\nخط دوم", 123456, false, "خوراک", 1762300000000L),
            MoneyTransaction("id-2", "حقوق \\ پاداش", 2000000, true, "حقوق", 0L)
        )
        val parsed = parseLegacyTransactions(transactionsToJson(original))
        assertEquals(original, parsed)
    }

    @Test
    fun jsonOutput_escapesSpecialCharacters() {
        val json = transactionsToJson(
            listOf(MoneyTransaction("x", "a\"b", 100, false, "سایر", 0L))
        )
        assertTrue(json.contains("a\\\"b"))
    }
}
