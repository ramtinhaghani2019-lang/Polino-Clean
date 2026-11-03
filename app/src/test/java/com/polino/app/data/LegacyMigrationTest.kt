package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyMigrationTest {

    @Test
    fun legacyEntryWithoutNewFields_getsDefaults() {
        val raw = """[{"title":"خرید","amount":50000,"income":false}]"""
        val result = parseLegacyTransactions(raw)
        assertEquals(1, result.size)
        assertEquals("خرید", result[0].title)
        assertEquals(50000L, result[0].amount)
        assertEquals(false, result[0].income)
        assertEquals("سایر", result[0].category)
        assertEquals(0L, result[0].dateMillis)
        assertTrue(result[0].id.isNotBlank())
    }

    @Test
    fun newFormatEntry_parsesAllFields() {
        val raw = """[{"id":"abc","title":"حقوق","amount":1000000,"income":true,"category":"حقوق","dateMillis":1762300000000}]"""
        val result = parseLegacyTransactions(raw)
        assertEquals(1, result.size)
        assertEquals("abc", result[0].id)
        assertEquals("حقوق", result[0].title)
        assertEquals(1000000L, result[0].amount)
        assertEquals(true, result[0].income)
        assertEquals("حقوق", result[0].category)
        assertEquals(1762300000000L, result[0].dateMillis)
    }

    @Test
    fun corruptOrEmptyJson_returnsEmpty() {
        assertEquals(emptyList<MoneyTransaction>(), parseLegacyTransactions("not json"))
        assertEquals(emptyList<MoneyTransaction>(), parseLegacyTransactions("[]"))
    }
}
