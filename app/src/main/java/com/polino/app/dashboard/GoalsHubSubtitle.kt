package com.polino.app.dashboard

import com.polino.app.data.ChequeReminderEntity
import com.polino.app.data.GoalEntity
import com.polino.app.data.InstallmentEntity
import com.polino.app.data.NoteEntity
import com.polino.app.data.PlanEntity
import com.polino.app.data.TaskEntity
import com.polino.app.data.isGoalCompleted

/**
 * زیرنویس‌های پویای کاشی‌های هاب اهداف (مرحله ۱۰۷، تصمیم کاربر ۲۰۲۶-۱۱-۰۵،
 * گزینه «پویا»): وقتی داده هست، زیرنویس از داده واقعی ساخته می‌شود؛ وقتی
 * نیست، همان متن خالی موجود بدون تغییر می‌ماند. همه ورودی‌ها فقط داده‌اند
 * و خروجی یک خط فارسی با ارقام فارسی است، پس در تست JVM هم اجرا می‌شوند.
 */

/** کاشی اهداف: خالی «تعریف هدف»؛ با داده، شمار هدف‌های فعال یا تکمیل‌شده. */
internal fun goalsHubSubtitle(goals: List<GoalEntity>): String {
    if (goals.isEmpty()) return "تعریف هدف"
    val inProgress = goals.count { !isGoalCompleted(it) }
    return if (inProgress > 0) {
        "${persianNumber(inProgress)} هدف فعال"
    } else {
        "${persianNumber(goals.size)} هدف تکمیل شده"
    }
}

/** کاشی کارها: بدون کار باز «انجام شده»؛ با داده، شمار کارهای باقی‌مانده. */
internal fun tasksHubSubtitle(tasks: List<TaskEntity>): String {
    val open = tasks.count { !it.done }
    return if (open == 0) "انجام شده" else "${persianNumber(open)} کار باقی‌مانده"
}

/** کاشی برنامه‌ها: شمار برنامه‌های باز؛ صفر هم همان عبارت شمار است. */
internal fun plansHubSubtitle(plans: List<PlanEntity>): String =
    "${persianNumber(plans.count { !it.done })} برنامه"

/** کاشی یادداشت‌ها: شمار کل یادداشت‌ها. */
internal fun notesHubSubtitle(notes: List<NoteEntity>): String =
    "${persianNumber(notes.size)} یادداشت"

/**
 * ردیف یادآورها: شمار یادآورهای باز (چک/قبض تسویه‌نشده + اقساط
 * پرداخت‌نشده). اگر یادآوری سررسید امروز باشد، همان شمارِ امروز نشان
 * داده می‌شود. سررسیدِ امروز فقط از چک/قبض خوانده می‌شود، چون مدل قسط
 * تاریخ سررسید بعدی ندارد.
 */
internal fun remindersHubSubtitle(
    cheques: List<ChequeReminderEntity>,
    installments: List<InstallmentEntity>,
    nowMillis: Long
): String {
    val dueToday = cheques.count {
        chequeDueStatus(it.dueMillis, nowMillis, it.settled) == ChequeDueStatus.DUE_TODAY
    }
    if (dueToday > 0) return "${persianNumber(dueToday)} یادآور برای امروز"
    val open = hubReminderCount(installments.count { it.paidCount < it.monthCount }, cheques)
    return "${persianNumber(open)} سررسید"
}
