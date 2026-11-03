package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {

    @Test
    fun headerAndRows_escapeSpecialCharacters() {
        val list = listOf(
            MoneyTransaction("id-1", "خرید، با \"تخفیف\"", 50000, false, "خوراک", 0L)
        )
        val csv = transactionsToCsv(list)
        assertTrue(csv.startsWith('\uFEFF' + "عنوان,مبلغ,نوع,دسته,تاریخ شمسی\n"))
        assertTrue(csv.contains("\"خرید، با \"\"تخفیف\"\"\",50000,هزینه,خوراک,بدون تاریخ"))
    }

    @Test
    fun emptyList_producesHeaderOnly() {
        assertEquals(
            '\uFEFF' + "عنوان,مبلغ,نوع,دسته,تاریخ شمسی\n",
            transactionsToCsv(emptyList())
        )
    }
}
