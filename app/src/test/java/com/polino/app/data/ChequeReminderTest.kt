package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChequeReminderTest {

    @Test
    fun overdue_onlyBeforeToday() {
        val day = 86_400_000L
        val now = 5 * day + 12_000L
        assertEquals(true, isChequeReminderOverdue(4 * day, now, false))
        assertEquals(false, isChequeReminderOverdue(5 * day, now, false))
        assertEquals(false, isChequeReminderOverdue(6 * day, now, false))
        assertEquals(false, isChequeReminderOverdue(4 * day, now, true))
    }

    @Test
    fun openTotal_countsUnsettledOnly() {
        val items = listOf(
            ChequeReminderEntity("c1", "چک صیادی", 100_000, 0L, settled = false),
            ChequeReminderEntity("c2", "قبض برق", 50_000, 0L, settled = true),
            ChequeReminderEntity("c3", "چک دوم", 25_000, 0L, settled = false)
        )
        assertEquals(125_000L, openChequeRemindersTotal(items))
        assertEquals(0L, openChequeRemindersTotal(emptyList()))
    }
}
