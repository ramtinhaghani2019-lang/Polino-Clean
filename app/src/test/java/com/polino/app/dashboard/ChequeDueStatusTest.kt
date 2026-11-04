package com.polino.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ChequeDueStatusTest {

    // An arbitrary midday timestamp; only the floorMod day-window math matters.
    private val noon = 1_762_765_800_000L
    private val startOfDay = noon - Math.floorMod(noon, 86_400_000L)

    @Test
    fun settledIsSettledRegardlessOfDate() {
        assertEquals(ChequeDueStatus.SETTLED, chequeDueStatus(startOfDay - 10_000L, noon, true))
        assertEquals(ChequeDueStatus.SETTLED, chequeDueStatus(startOfDay + 10_000L, noon, true))
    }

    @Test
    fun pastDueIsOverdue() {
        assertEquals(ChequeDueStatus.OVERDUE, chequeDueStatus(startOfDay - 1L, noon, false))
        assertEquals(ChequeDueStatus.OVERDUE, chequeDueStatus(startOfDay - 86_400_000L, noon, false))
    }

    @Test
    fun todayWindowIsDueToday() {
        assertEquals(ChequeDueStatus.DUE_TODAY, chequeDueStatus(startOfDay, noon, false))
        assertEquals(ChequeDueStatus.DUE_TODAY, chequeDueStatus(noon, noon, false))
        assertEquals(
            ChequeDueStatus.DUE_TODAY,
            chequeDueStatus(startOfDay + 86_400_000L - 1L, noon, false)
        )
    }

    @Test
    fun futureIsUpcoming() {
        assertEquals(
            ChequeDueStatus.UPCOMING,
            chequeDueStatus(startOfDay + 86_400_000L, noon, false)
        )
    }
}
