package com.polino.app.dashboard

import com.polino.app.data.BudgetEntity
import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetWarningTest {

    private fun expense(id: String, title: String, amount: Long, category: String) =
        MoneyTransaction(id, title, amount, false, category, JalaliDate.jalaliToMillis(1405, 8, 12))

    @Test
    fun overspent_onlyStrictlyOverLimit() {
        val transactions = listOf(
            expense("1", "خوراک ۱", 150000, "خوراک"),
            expense("2", "خوراک ۲", 100000, "خوراک"),
            expense("3", "رفت‌وآمد", 50000, "حمل‌ونقل"),
            MoneyTransaction("4", "حقوق", 1000000, true, "حقوق", JalaliDate.jalaliToMillis(1405, 8, 1))
        )
        val budgets = listOf(
            BudgetEntity("1405-08", "خوراک", 200000),
            BudgetEntity("1405-08", "حمل‌ونقل", 50000),
            BudgetEntity("1405-08", "سلامت", 100000)
        )
        val result = overspentCategories(transactionsInMonth(transactions, "1405-08"), budgets)
        // فقط خوراک رد شده (۲۵۰٬۰۰۰ از ۲۰۰٬۰۰۰)؛ حمل‌ونقل دقیقاً به سقف
        // رسیده و ردشده نیست، سلامت هم هزینه‌ای ندارد، درآمد هم نادیده
        // گرفته می‌شود.
        assertEquals(listOf(OverspentCategory("خوراک", 250000, 200000)), result)
    }

    @Test
    fun overspent_sortedByOverageDescending() {
        val transactions = listOf(
            expense("1", "خوراک", 210000, "خوراک"),
            expense("2", "لباس", 300000, "پوشاک")
        )
        val budgets = listOf(
            BudgetEntity("1405-08", "خوراک", 200000),
            BudgetEntity("1405-08", "پوشاک", 100000)
        )
        val result = overspentCategories(transactionsInMonth(transactions, "1405-08"), budgets)
        assertEquals(
            listOf(
                OverspentCategory("پوشاک", 300000, 100000),
                OverspentCategory("خوراک", 210000, 200000)
            ),
            result
        )
    }

    @Test
    fun nearing_onlyBetweenEightyAndLimit_sortedByRatio() {
        val transactions = listOf(
            expense("1", "خوراک", 90000, "خوراک"),
            expense("2", "لباس", 80000, "پوشاک"),
            expense("3", "رفت‌وآمد", 79000, "حمل‌ونقل"),
            expense("4", "درمان", 150000, "سلامت")
        )
        val budgets = listOf(
            BudgetEntity("1405-08", "خوراک", 100000),
            BudgetEntity("1405-08", "پوشاک", 100000),
            BudgetEntity("1405-08", "حمل‌ونقل", 100000),
            BudgetEntity("1405-08", "سلامت", 100000)
        )
        val result = nearingCategories(transactionsInMonth(transactions, "1405-08"), budgets)
        // خوراک ۹۰٪ و پوشاک دقیقاً ۸۰٪ نزدیک‌اند (نزدیک‌ترین اول)؛
        // حمل‌ونقل ۷۹٪ هنوز نزدیک نیست و سلامت رد شده است و اینجا نمی‌آید.
        assertEquals(
            listOf(
                NearingCategory("خوراک", 90000, 100000),
                NearingCategory("پوشاک", 80000, 100000)
            ),
            result
        )
    }

    @Test
    fun overspent_emptyWhenNoBudgetsOrNoSpending() {
        val transactions = listOf(expense("1", "خوراک", 500000, "خوراک"))
        assertEquals(
            emptyList<OverspentCategory>(),
            overspentCategories(transactionsInMonth(transactions, "1405-08"), emptyList())
        )
        assertEquals(
            emptyList<OverspentCategory>(),
            overspentCategories(emptyList(), listOf(BudgetEntity("1405-08", "خوراک", 100000)))
        )
    }
}
