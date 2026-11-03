package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {

    @Test
    fun headerAndRows_escapeSpecialCharacters() {
        val list = listOf(
            MoneyTransaction("id-1", "خرید، با \"تخفیف\"", 50000, false, "خوراک", 0L),
            MoneyTransaction("id-2", "کرایه", 100000, false, "حمل‌ونقل", 0L, "main", "قرض، به علی")
        )
        val csv = transactionsToCsv(list)
        assertTrue(csv.startsWith('\uFEFF' + "عنوان,مبلغ,نوع,دسته,تاریخ شمسی,یادداشت\n"))
        assertTrue(csv.contains("\"خرید، با \"\"تخفیف\"\"\",50000,هزینه,خوراک,بدون تاریخ,"))
        assertTrue(csv.contains("کرایه,100000,هزینه,حمل‌ونقل,بدون تاریخ,\"قرض، به علی\""))
    }

    @Test
    fun emptyList_producesHeaderOnly() {
        assertEquals(
            '\uFEFF' + "عنوان,مبلغ,نوع,دسته,تاریخ شمسی,یادداشت\n",
            transactionsToCsv(emptyList())
        )
    }
}
