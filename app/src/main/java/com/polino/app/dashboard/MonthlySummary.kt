package com.polino.app.dashboard

import com.polino.app.data.BudgetEntity
import com.polino.app.data.MoneyTransaction

internal data class MonthTotals(val income: Long, val expense: Long)

/** تراکنش‌های یک ماه شمسی؛ ورودی‌های بدون تاریخ (قدیمی) کنار گذاشته می‌شوند. */
internal fun transactionsInMonth(
    transactions: List<MoneyTransaction>,
    yearMonth: String
): List<MoneyTransaction> =
    transactions.filter {
        it.dateMillis != 0L && JalaliDate.jalaliYearMonthOf(it.dateMillis) == yearMonth
    }

internal fun monthTotals(monthTransactions: List<MoneyTransaction>): MonthTotals {
    val income = monthTransactions.filter { it.income }.sumOf { it.amount }
    val expense = monthTransactions.filterNot { it.income }.sumOf { it.amount }
    return MonthTotals(income, expense)
}

/** هزینه هر دسته در ماه، از بیشترین به کمترین. */
internal fun spentByCategory(monthTransactions: List<MoneyTransaction>): List<Pair<String, Long>> =
    monthTransactions.filterNot { it.income }
        .groupBy { it.category }
        .map { (category, items) -> category to items.sumOf { it.amount } }
        .sortedByDescending { it.second }

/** دسته‌ای که هزینه ماه جاری‌اش از بودجه‌اش گذشته است. */
internal data class OverspentCategory(val category: String, val spent: Long, val limit: Long)

/**
 * دسته‌های ردشده از بودجه ماه جاری، از بیشترین اضافه‌خرج به کمترین.
 * درآمد نادیده گرفته می‌شود و دسته‌ای که دقیقاً به سقف رسیده ردشده
 * حساب نمی‌شود؛ بودجه دسته بدون هزینه هم هشدار نمی‌سازد.
 */
internal fun overspentCategories(
    monthTransactions: List<MoneyTransaction>,
    budgets: List<BudgetEntity>
): List<OverspentCategory> {
    val spent = spentByCategory(monthTransactions).toMap()
    return budgets.mapNotNull { budget ->
        val spentAmount = spent[budget.category] ?: 0L
        if (spentAmount > budget.limitAmount) {
            OverspentCategory(budget.category, spentAmount, budget.limitAmount)
        } else {
            null
        }
    }.sortedByDescending { it.spent - it.limit }
}

internal data class MonthTrend(val yearMonth: String, val income: Long, val expense: Long)

/** مجموع درآمد و هزینه هر ماه از فهرست داده‌شده، به همان ترتیب ورودی. */
internal fun monthlyTrend(
    transactions: List<MoneyTransaction>,
    yearMonths: List<String>
): List<MonthTrend> = yearMonths.map { yearMonth ->
    val totals = monthTotals(transactionsInMonth(transactions, yearMonth))
    MonthTrend(yearMonth, totals.income, totals.expense)
}
