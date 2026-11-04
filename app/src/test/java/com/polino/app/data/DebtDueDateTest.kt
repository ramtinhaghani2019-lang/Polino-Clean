package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DebtDueDateTest {

    @Test
    fun overdue_onlyBeforeTodayAndOnlyWithDueDate() {
        val day = 86_400_000L
        val now = 5 * day + 12_000L
        assertEquals(true, isDebtOverdue(4 * day, now, false))
        assertEquals(false, isDebtOverdue(5 * day, now, false))
        assertEquals(false, isDebtOverdue(6 * day, now, false))
        assertEquals(false, isDebtOverdue(4 * day, now, true))
        assertEquals(false, isDebtOverdue(null, now, false))
    }
}
