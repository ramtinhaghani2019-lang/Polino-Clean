package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun goalsOverallFraction_sumsSavedOverTargets() {
        val goals = listOf(
            GoalEntity("g1", "الف", 1000, "main", savedAmount = 500),
            GoalEntity("g2", "ب", 3000, "main", savedAmount = 1500)
        )
        assertEquals(0.5f, goalsOverallFraction(goals), 0.001f)
        assertEquals(0f, goalsOverallFraction(emptyList()), 0.001f)
        assertEquals(
            0f,
            goalsOverallFraction(listOf(GoalEntity("g3", "پ", 0, "main", savedAmount = 100))),
            0.001f
        )
    }

    @Test
    fun isGoalCompleted_reachesTarget() {
        assertTrue(isGoalCompleted(GoalEntity("g1", "الف", 1000, "main", savedAmount = 1000)))
        assertTrue(isGoalCompleted(GoalEntity("g2", "ب", 1000, "main", savedAmount = 1500)))
        assertFalse(isGoalCompleted(GoalEntity("g3", "پ", 1000, "main", savedAmount = 999)))
    }
}
