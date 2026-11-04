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

    @Test
    fun dongiSettlements_balanced_returnsEmpty() {
        val members = listOf(
            DongiMember("علی", 40000),
            DongiMember("رضا", 40000),
            DongiMember("سارا", 40000)
        )
        assertEquals(emptyList<DongiSettlement>(), dongiSettlements(members))
    }

    @Test
    fun dongiSettlements_twoPerson() {
        val members = listOf(DongiMember("علی", 100000), DongiMember("رضا", 0))
        assertEquals(
            listOf(DongiSettlement("رضا", "علی", 50000)),
            dongiSettlements(members)
        )
    }

    @Test
    fun dongiSettlements_multiPerson() {
        val members = listOf(
            DongiMember("علی", 90000),
            DongiMember("رضا", 30000),
            DongiMember("سارا", 0)
        )
        assertEquals(
            listOf(
                DongiSettlement("رضا", "علی", 10000),
                DongiSettlement("سارا", "علی", 40000)
            ),
            dongiSettlements(members)
        )
    }

    @Test
    fun dongiSettlements_remainderGoesToFirst() {
        val members = listOf(
            DongiMember("علی", 100001),
            DongiMember("رضا", 0),
            DongiMember("سارا", 0)
        )
        assertEquals(
            listOf(
                DongiSettlement("رضا", "علی", 33334),
                DongiSettlement("سارا", "علی", 33333)
            ),
            dongiSettlements(members)
        )
    }

    @Test
    fun dongiSettlements_settledMemberExcluded() {
        val members = listOf(
            DongiMember("علی", 100000),
            DongiMember("رضا", 0),
            DongiMember("سارا", 0, settled = true)
        )
        assertEquals(
            listOf(DongiSettlement("رضا", "علی", 33333)),
            dongiSettlements(members)
        )
    }

    @Test
    fun dongiSettlements_emptyMembers_returnsEmpty() {
        assertEquals(emptyList<DongiSettlement>(), dongiSettlements(emptyList()))
    }
}
