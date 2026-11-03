package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DongiCalcTest {

    @Test
    fun dongiShares_evenSplit() {
        assertEquals(listOf(30000L, 30000L, 30000L), dongiShares(90000, 3))
    }

    @Test
    fun dongiShares_remainderGoesToFirst() {
        val shares = dongiShares(100000, 3)
        assertEquals(listOf(33334L, 33333L, 33333L), shares)
        assertEquals(100000L, shares.sum())
    }

    @Test
    fun dongiShares_singleCoinBetweenTwo() {
        assertEquals(listOf(1L, 0L), dongiShares(1, 2))
    }

    @Test
    fun dongiShares_zeroTotal() {
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L), dongiShares(0, 5))
    }

    @Test
    fun dongiShares_invalidInput_returnsEmpty() {
        assertEquals(emptyList<Long>(), dongiShares(-1000, 3))
        assertEquals(emptyList<Long>(), dongiShares(1000, 0))
    }
}
