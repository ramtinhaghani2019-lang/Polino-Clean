package com.polino.app.data

/** پیشرفت پس‌انداز نسبت به هدف؛ بین صفر و یک، و هدف نامعتبر صفر است. */
internal fun goalProgressFraction(balance: Long, target: Long): Float {
    if (target <= 0) return 0f
    val saved = balance.coerceAtLeast(0)
    return (saved.toFloat() / target.toFloat()).coerceAtMost(1f)
}
