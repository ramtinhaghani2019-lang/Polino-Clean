package com.polino.app.dashboard

import com.polino.app.data.ChequeReminderEntity
import com.polino.app.data.GoalEntity
import com.polino.app.data.InstallmentEntity
import com.polino.app.data.NoteEntity
import com.polino.app.data.PlanEntity
import com.polino.app.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalsHubSubtitleTest {

    // Same midday anchor as ChequeDueStatusTest; only the day-window math matters.
    private val noon = 1_762_765_800_000L
    private val startOfDay = noon - Math.floorMod(noon, 86_400_000L)

    private fun goal(id: String, saved: Long, target: Long = 1000L) =
        GoalEntity(id, "هدف $id", target, "main", savedAmount = saved)

    private fun task(id: String, done: Boolean) = TaskEntity(id, "کار $id", 0L, done)

    private fun plan(id: String, done: Boolean) = PlanEntity(id, "برنامه $id", done = done)

    private fun note(id: String) = NoteEntity(id, "یادداشت $id", "", 0L, 0L)

    private fun cheque(id: String, dueMillis: Long, settled: Boolean = false) =
        ChequeReminderEntity(id, "چک $id", 1000L, dueMillis, settled)

    private fun installment(id: String, paidCount: Int, monthCount: Int = 3) =
        InstallmentEntity(id, "قسط $id", 3000L, monthCount, paidCount = paidCount)

    @Test
    fun goals_emptyKeepsPergarText() {
        assertEquals("تعریف هدف", goalsHubSubtitle(emptyList()))
    }

    @Test
    fun goals_countsActiveGoals() {
        // Two active (saved below target), one completed (saved at target).
        val goals = listOf(goal("a", 100), goal("b", 500), goal("c", 1000))
        assertEquals("۲ هدف فعال", goalsHubSubtitle(goals))
    }

    @Test
    fun goals_singleActiveGoal() {
        assertEquals("۱ هدف فعال", goalsHubSubtitle(listOf(goal("a", 0))))
    }

    @Test
    fun goals_manyActiveGoals() {
        val goals = (1..12).map { goal("g$it", 0) }
        assertEquals("۱۲ هدف فعال", goalsHubSubtitle(goals))
    }

    @Test
    fun goals_allCompletedShowsCompletedCount() {
        val goals = listOf(goal("a", 1000), goal("b", 1500))
        assertEquals("۲ هدف تکمیل شده", goalsHubSubtitle(goals))
    }

    @Test
    fun tasks_emptyAndAllDoneKeepPergarText() {
        assertEquals("انجام شده", tasksHubSubtitle(emptyList()))
        assertEquals("انجام شده", tasksHubSubtitle(listOf(task("a", true), task("b", true))))
    }

    @Test
    fun tasks_countsOpenTasks() {
        val tasks = listOf(task("a", false), task("b", true), task("c", false))
        assertEquals("۲ کار باقی‌مانده", tasksHubSubtitle(tasks))
    }

    @Test
    fun tasks_manyOpenTasks() {
        val tasks = (1..12).map { task("t$it", false) }
        assertEquals("۱۲ کار باقی‌مانده", tasksHubSubtitle(tasks))
    }

    @Test
    fun plans_emptyIsZeroCount() {
        assertEquals("۰ برنامه", plansHubSubtitle(emptyList()))
    }

    @Test
    fun plans_countsOpenPlansOnly() {
        val plans = listOf(plan("a", false), plan("b", true), plan("c", false))
        assertEquals("۲ برنامه", plansHubSubtitle(plans))
    }

    @Test
    fun plans_allDoneIsZeroCount() {
        assertEquals("۰ برنامه", plansHubSubtitle(listOf(plan("a", true))))
    }

    @Test
    fun notes_countsNotes() {
        assertEquals("۰ یادداشت", notesHubSubtitle(emptyList()))
        assertEquals("۱ یادداشت", notesHubSubtitle(listOf(note("a"))))
        assertEquals("۵ یادداشت", notesHubSubtitle((1..5).map { note("n$it") }))
    }

    @Test
    fun reminders_emptyIsZeroCount() {
        assertEquals("۰ سررسید", remindersHubSubtitle(emptyList(), emptyList(), noon))
    }

    @Test
    fun reminders_countsOpenChequesAndUnpaidInstallments() {
        // Open: 1 unsettled cheque + 2 unpaid installments (i1, i3);
        // settled cheque b and fully-paid i2 do not count.
        val cheques = listOf(
            cheque("a", startOfDay + 3 * 86_400_000L),
            cheque("b", startOfDay + 4 * 86_400_000L, settled = true)
        )
        val installments = listOf(
            installment("i1", 1),
            installment("i2", 3),
            installment("i3", 0)
        )
        assertEquals("۳ سررسید", remindersHubSubtitle(cheques, installments, noon))
    }

    @Test
    fun reminders_overdueWithoutDueTodayShowsOpenCount() {
        val cheques = listOf(cheque("a", startOfDay - 86_400_000L))
        assertEquals("۱ سررسید", remindersHubSubtitle(cheques, emptyList(), noon))
    }

    @Test
    fun reminders_dueTodayPrefersTodayPhrasing() {
        val cheques = listOf(
            cheque("a", noon),
            cheque("b", startOfDay + 86_400_000L)
        )
        assertEquals("۱ یادآور برای امروز", remindersHubSubtitle(cheques, emptyList(), noon))
    }

    @Test
    fun reminders_twoDueToday() {
        val cheques = listOf(cheque("a", startOfDay), cheque("b", noon))
        assertEquals("۲ یادآور برای امروز", remindersHubSubtitle(cheques, emptyList(), noon))
    }

    @Test
    fun reminders_settledChequeDueTodayDoesNotCount() {
        val cheques = listOf(cheque("a", noon, settled = true))
        assertEquals("۰ سررسید", remindersHubSubtitle(cheques, emptyList(), noon))
    }
}
