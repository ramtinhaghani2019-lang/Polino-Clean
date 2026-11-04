package com.polino.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DongiSummaryTest {

    @Test
    fun personCountText_usesPersianDigits() {
        assertEquals("۲ نفر", dongiPersonCountText(2))
        assertEquals("۰ نفر", dongiPersonCountText(0))
    }

    @Test
    fun settlementCountText_countsPayments() {
        assertEquals("۰ پرداخت", dongiSettlementCountText(0))
        assertEquals("۳ پرداخت", dongiSettlementCountText(3))
    }

    @Test
    fun shareSummary_equalShares_singleValue() {
        assertEquals(money(30000L), dongiShareSummary(listOf(30000L, 30000L, 30000L)))
    }

    @Test
    fun shareSummary_remainderRange() {
        assertEquals(
            "${money(33333L)} تا ${money(33334L)}",
            dongiShareSummary(listOf(33334L, 33333L, 33333L))
        )
    }

    @Test
    fun shareSummary_emptyShares_emptyText() {
        assertEquals("", dongiShareSummary(emptyList()))
    }

    @Test
    fun balanceLabel_directionsAndSettled() {
        assertEquals("باید بگیرد: ${money(5000L)}", dongiBalanceLabel(false, 5000L))
        assertEquals("باید بدهد: ${money(7000L)}", dongiBalanceLabel(false, -7000L))
        assertEquals("متوازن", dongiBalanceLabel(false, 0L))
        assertEquals("", dongiBalanceLabel(true, 5000L))
        assertEquals("", dongiBalanceLabel(true, -5000L))
    }
}
