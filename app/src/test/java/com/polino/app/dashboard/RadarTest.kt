package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarTest {

    @Test
    fun increase_detected() {
        val current = listOf(
            MoneyTransaction("c1", "t", 500000, false, "خوراک", 1L),
            MoneyTransaction("c2", "t", 100000, false, "حمل‌ونقل", 1L)
        )
        val previous = listOf(
            MoneyTransaction("p1", "t", 300000, false, "خوراک", 1L),
            MoneyTransaction("p2", "t", 100000, false, "حمل‌ونقل", 1L)
        )
        val insights = spendingInsights(current, previous)
        assertEquals(1, insights.size)
        assertEquals(RadarKind.UP, insights[0].kind)
        assertTrue(insights[0].text.contains("خوراک"))
    }

    @Test
    fun newCategory_detected() {
        val current = listOf(MoneyTransaction("c1", "t", 200000, false, "تفریح", 1L))
        val insights = spendingInsights(current, emptyList())
        assertEquals(1, insights.size)
        assertEquals(RadarKind.NEW, insights[0].kind)
    }

    @Test
    fun totalDecrease_detected() {
        val current = listOf(MoneyTransaction("c1", "t", 100000, false, "خوراک", 1L))
        val previous = listOf(
            MoneyTransaction("p1", "t", 300000, false, "خوراک", 1L),
            MoneyTransaction("p2", "t", 200000, false, "تفریح", 1L)
        )
        val insights = spendingInsights(current, previous)
        assertTrue(insights.any { it.kind == RadarKind.DOWN })
    }

    @Test
    fun emptyCurrent_returnsEmpty() {
        assertTrue(spendingInsights(emptyList(), emptyList()).isEmpty())
    }
}
