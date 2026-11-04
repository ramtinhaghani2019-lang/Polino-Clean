package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction

/** یک پیشنهاد ثبت سریع: دسته + مبلغ آخرین تراکنش آن دسته. */
data class EntrySuggestion(
    val category: String,
    val amount: Long
)

/**
 * پیشنهادهای ثبت را از تراکنش‌های خود کاربر می‌سازد: فقط تراکنش‌های هم‌جهت
 * (هزینه/درآمد) با صفحه فعلی، تازه‌ترین اول، هر دسته فقط یک بار (تازه‌ترین
 * مبلغش)، حداکثر [limit] مورد. هیچ مقداری ساخته نمی‌شود.
 */
internal fun deriveEntrySuggestions(
    transactions: List<MoneyTransaction>,
    income: Boolean,
    limit: Int = 8
): List<EntrySuggestion> {
    if (limit <= 0) return emptyList()
    val seen = mutableSetOf<String>()
    val result = mutableListOf<EntrySuggestion>()
    transactions
        .filter { it.income == income }
        .sortedByDescending { it.dateMillis }
        .forEach { tx ->
            if (tx.category !in seen) {
                seen.add(tx.category)
                result.add(EntrySuggestion(tx.category, tx.amount))
                if (result.size >= limit) return result
            }
        }
    return result
}
