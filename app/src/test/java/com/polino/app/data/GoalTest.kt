package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalTest {

    @Test
    fun goalProgressFraction_isClampedBetweenZeroAndOne() {
        assertEquals(0f, goalProgressFraction(0, 1000), 0.001f)
        assertEquals(0.5f, goalProgressFraction(500, 1000), 0.001f)
        assertEquals(1f, goalProgressFraction(1000, 1000), 0.001f)
        assertEquals(1f, goalProgressFraction(1500, 1000), 0.001f)
        assertEquals(0f, goalProgressFraction(-200, 1000), 0.001f)
        assertEquals(0f, goalProgressFraction(500, 0), 0.001f)
    }
}
