package com.polino.app.dashboard

import com.polino.app.data.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetMonthBarTest {

    private fun category(name: String, income: Boolean = false, investment: Boolean = false, parent: String? = null) =
        CategoryEntity(name, income, investment, parent)

    @Test
    fun allBudgetCategoryNames_mixesAllMainTypesInStoredOrder() {
        val categories = listOf(
            category("خوراک و پوشاک"),
            category("حقوق و دستمزد", income = true),
            category("طلا و جواهر", investment = true),
            category("حمل و نقل"),
            category("پاداش و کارانه", income = true)
        )
        assertEquals(
            listOf("خوراک و پوشاک", "حقوق و دستمزد", "طلا و جواهر", "حمل و نقل", "پاداش و کارانه"),
            allBudgetCategoryNames(categories)
        )
    }

    @Test
    fun allBudgetCategoryNames_excludesSubcategories() {
        val categories = listOf(
            category("خوراک و پوشاک"),
            category("رستوران", parent = "خوراک و پوشاک"),
            category("سایر")
        )
        assertEquals(
            listOf("خوراک و پوشاک", "سایر"),
            allBudgetCategoryNames(categories)
        )
    }

    @Test
    fun allBudgetCategoryNames_emptyInput() {
        assertEquals(emptyList<String>(), allBudgetCategoryNames(emptyList()))
    }

    @Test
    fun isBudgetMonthNextDisabled_currentMonth() {
        assertTrue(isBudgetMonthNextDisabled("1405-08", "1405-08"))
    }

    @Test
    fun isBudgetMonthNextDisabled_otherMonth() {
        assertFalse(isBudgetMonthNextDisabled("1405-07", "1405-08"))
        assertFalse(isBudgetMonthNextDisabled("1405-09", "1405-08"))
    }
}
