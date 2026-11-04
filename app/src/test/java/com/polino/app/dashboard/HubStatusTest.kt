package com.polino.app.dashboard

import com.polino.app.data.ChequeReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HubStatusTest {

    private fun cheque(id: String, settled: Boolean) = ChequeReminderEntity(
        id = id,
        title = "چک $id",
        amount = 1000L,
        dueMillis = 0L,
        settled = settled,
        note = ""
    )

    @Test
    fun openChequeCount_countsOnlyUnsettled() {
        val cheques = listOf(cheque("a", false), cheque("b", true), cheque("c", false))
        assertEquals(2, openChequeCount(cheques))
    }

    @Test
    fun openChequeCount_emptyIsZero() {
        assertEquals(0, openChequeCount(emptyList()))
    }

    @Test
    fun hubReminderCount_sumsInstallmentsAndOpenCheques() {
        val cheques = listOf(cheque("a", false), cheque("b", true))
        assertEquals(4, hubReminderCount(3, cheques))
    }

    @Test
    fun hubReminderCount_allSettledCountsOnlyInstallments() {
        val cheques = listOf(cheque("a", true), cheque("b", true))
        assertEquals(2, hubReminderCount(2, cheques))
    }
}
