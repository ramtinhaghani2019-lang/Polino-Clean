package com.polino.app.dashboard

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
