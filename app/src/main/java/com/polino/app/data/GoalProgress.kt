package com.polino.app.data

/** پیشرفت پس‌انداز نسبت به هدف؛ بین صفر و یک، و هدف نامعتبر صفر است. */
internal fun goalProgressFraction(saved: Long, target: Long): Float {
    if (target <= 0) return 0f
    val savedClamped = saved.coerceAtLeast(0)
    return (savedClamped.toFloat() / target.toFloat()).coerceAtMost(1f)
}

/** پیشرفت کل همه هدف‌ها: مجموع پس‌اندازشده بر مجموع هدف‌ها. */
internal fun goalsOverallFraction(goals: List<GoalEntity>): Float {
    val totalTarget = goals.sumOf { it.target }
    if (totalTarget <= 0) return 0f
    val totalSaved = goals.sumOf { it.savedAmount.coerceAtLeast(0) }
    return (totalSaved.toFloat() / totalTarget.toFloat()).coerceAtMost(1f)
}

/** هدف وقتی تکمیل است که پس‌اندازشده به سقف برسد. */
internal fun isGoalCompleted(goal: GoalEntity): Boolean =
    goalProgressFraction(goal.savedAmount, goal.target) >= 1f
