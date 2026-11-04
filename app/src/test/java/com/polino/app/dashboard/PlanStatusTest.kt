package com.polino.app.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanStatusTest {

    @Test
    fun overdueWhenPastAndNotDone() {
        assertTrue(isPlanOverdue(dueMillis = 1_000L, nowMillis = 2_000L, done = false))
    }

    @Test
    fun notOverdueWhenFuture() {
        assertFalse(isPlanOverdue(dueMillis = 3_000L, nowMillis = 2_000L, done = false))
    }

    @Test
    fun notOverdueWhenDone() {
        assertFalse(isPlanOverdue(dueMillis = 1_000L, nowMillis = 2_000L, done = true))
    }

    @Test
    fun notOverdueWhenNoDueDate() {
        assertFalse(isPlanOverdue(dueMillis = null, nowMillis = 2_000L, done = false))
    }

    @Test
    fun notOverdueWhenDueExactlyNow() {
        assertFalse(isPlanOverdue(dueMillis = 2_000L, nowMillis = 2_000L, done = false))
    }
}
