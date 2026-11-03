package com.polino.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskOverdueTest {

    @Test
    fun overdue_whenPastAndNotDone() {
        assertTrue(isTaskOverdue(1000L, 2000L, false))
    }

    @Test
    fun notOverdue_whenFuture() {
        assertFalse(isTaskOverdue(3000L, 2000L, false))
    }

    @Test
    fun notOverdue_whenDone() {
        assertFalse(isTaskOverdue(1000L, 2000L, true))
    }
}
