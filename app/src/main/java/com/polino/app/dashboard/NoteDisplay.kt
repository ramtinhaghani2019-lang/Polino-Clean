package com.polino.app.dashboard

/**
 * پیش‌نمایش یک‌خطی متن یادداشت برای کارت فهرست: خط‌های تازه به
 * فاصله تبدیل می‌شوند، فاصله‌های اضافی ابتدا و انتها حذف می‌شوند و
 * اگر از حد بیشتر شد، با «…» بریده می‌شود.
 */
internal fun noteSnippet(body: String, limit: Int = 120): String {
    val singleLine = body.replace('\n', ' ').replace('\r', ' ').trim()
    if (singleLine.length <= limit) return singleLine
    return singleLine.take(limit).trimEnd() + "…"
}
