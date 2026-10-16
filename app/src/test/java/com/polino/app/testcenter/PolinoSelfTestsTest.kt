package com.polino.app.testcenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PolinoSelfTestsTest {
    @Test
    fun tomanToRial_isCorrect() {
        assertEquals(100L, PolinoSelfTests.tomanToRial(10))
    }

    @Test
    fun balance_isCorrect() {
        assertEquals(
            900_000L,
            PolinoSelfTests.calculateBalance(
                incomesRial = listOf(1_000_000L, 200_000L),
                expensesRial = listOf(250_000L, 50_000L)
            )
        )
    }

    @Test
    fun negativeAmount_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PolinoSelfTests.validateMoneyAmount(-1)
        }
    }
}
