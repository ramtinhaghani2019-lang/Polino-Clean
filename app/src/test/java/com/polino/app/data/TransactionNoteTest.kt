package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionNoteTest {

    @Test
    fun noteField_isReadWhenPresent() {
        val raw = """[{"title":"خرید","amount":50000,"income":false,"note":"قرض به علی"}]"""
        val result = parseLegacyTransactions(raw)
        assertEquals(1, result.size)
        assertEquals("قرض به علی", result[0].note)
    }

    @Test
    fun noteField_defaultsToEmptyWhenAbsent() {
        val raw = """[{"title":"خرید","amount":50000,"income":false}]"""
        val result = parseLegacyTransactions(raw)
        assertEquals(1, result.size)
        assertEquals("", result[0].note)
        assertEquals("", MoneyTransaction("x", "خرید", 50000, false, "سایر", 0L).note)
    }
}
