package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Test

class EntrySuggestionsTest {

    private fun tx(category: String, amount: Long, income: Boolean, date: Long) =
        MoneyTransaction(
            id = "$category-$date",
            title = category,
            amount = amount,
            income = income,
            category = category,
            dateMillis = date
        )

    @Test
    fun suggestions_newestFirstDedupedByCategory() {
        val txs = listOf(
            tx("خوراک", 100L, false, 1L),
            tx("مسکن", 200L, false, 2L),
            tx("خوراک", 150L, false, 3L)
        )
        assertEquals(
            listOf(
                EntrySuggestion("خوراک", 150L),
                EntrySuggestion("مسکن", 200L)
            ),
            deriveEntrySuggestions(txs, income = false)
        )
    }

    @Test
    fun suggestions_filterByDirection() {
        val txs = listOf(
            tx("خوراک", 100L, false, 1L),
            tx("حقوق", 500L, true, 2L)
        )
        assertEquals(
            listOf(EntrySuggestion("حقوق", 500L)),
            deriveEntrySuggestions(txs, income = true)
        )
        assertEquals(
            listOf(EntrySuggestion("خوراک", 100L)),
            deriveEntrySuggestions(txs, income = false)
        )
    }

    @Test
    fun suggestions_limitRespected() {
        val txs = (1..10).map { tx("دسته$it", it.toLong(), false, it.toLong()) }
        val result = deriveEntrySuggestions(txs, income = false, limit = 3)
        assertEquals(3, result.size)
        assertEquals("دسته10", result[0].category)
    }

    @Test
    fun suggestions_emptyAndZeroLimit() {
        assertEquals(emptyList<EntrySuggestion>(), deriveEntrySuggestions(emptyList(), false))
        assertEquals(
            emptyList<EntrySuggestion>(),
            deriveEntrySuggestions(listOf(tx("خوراک", 1L, false, 1L)), false, limit = 0)
        )
    }
}
