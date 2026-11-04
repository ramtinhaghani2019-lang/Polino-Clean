package com.polino.app.dashboard

/**
 * برنامه‌ای که موعدش گذشته و هنوز انجام نشده است؛ برنامه بدون موعد
 * هرگز سررسیدگذشته نیست و برنامه انجام‌شده هم نیست.
 */
internal fun isPlanOverdue(dueMillis: Long?, nowMillis: Long, done: Boolean): Boolean =
    !done && dueMillis != null && dueMillis < nowMillis
