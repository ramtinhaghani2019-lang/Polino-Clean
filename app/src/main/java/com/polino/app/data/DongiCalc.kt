package com.polino.app.data

/**
 * تقسیم مساوی مبلغ کل بین نفرات؛ باقیمانده (کمتر از تعداد نفرات) یکی‌یکی
 * به نفرات اول می‌رسد تا جمع سهم‌ها دقیقاً برابر مبلغ کل بماند.
 * ورودی نامعتبر (مبلغ منفی یا کمتر از یک نفر) فهرست خالی می‌دهد.
 */
fun dongiShares(total: Long, people: Int): List<Long> {
    if (total < 0 || people < 1) return emptyList()
    val base = total / people
    val remainder = (total % people).toInt()
    return List(people) { index -> base + if (index < remainder) 1 else 0 }
}
