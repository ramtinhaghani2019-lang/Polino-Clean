package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentMathTest {

    @Test
    fun monthlyAmount_dividesEvenly() {
        assertEquals(100000L, installmentMonthlyAmount(1200000L, 12))
    }

    @Test
    fun monthlyAmount_zeroMonths_returnsZero() {
        assertEquals(0L, installmentMonthlyAmount(1200000L, 0))
    }

    @Test
    fun progressFraction_clamped() {
        assertEquals(0.5f, installmentProgressFraction(6, 12), 0.001f)
        assertEquals(1f, installmentProgressFraction(15, 12), 0.001f)
        assertEquals(0f, installmentProgressFraction(0, 0), 0.001f)
    }
}
