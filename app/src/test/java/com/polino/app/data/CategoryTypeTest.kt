package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTypeTest {

    @Test
    fun investmentFlag_winsOverIncomeFlag() {
        val entity = CategoryEntity("صندوق", true, true)
        assertEquals(CategoryType.INVESTMENT, categoryType(entity))
    }

    @Test
    fun incomeAndExpense_resolvedFromIsIncome() {
        assertEquals(CategoryType.INCOME, categoryType(CategoryEntity("حقوق", true)))
        assertEquals(CategoryType.EXPENSE, categoryType(CategoryEntity("خوراک", false)))
        assertEquals(CategoryType.EXPENSE, categoryType(CategoryEntity(FALLBACK_CATEGORY, false)))
    }

    @Test
    fun mainsAndSubs_groupedByParent() {
        val categories = listOf(
            CategoryEntity("خوراک", false),
            CategoryEntity("رستوران", false, false, "خوراک"),
            CategoryEntity("سوپرمارکت", false, false, "خوراک"),
            CategoryEntity("حقوق", true)
        )
        val mains = mainCategories(categories, CategoryType.EXPENSE)
        assertEquals(listOf("خوراک"), mains.map { it.name })
        val subs = subcategoriesOf(categories, "خوراک")
        assertEquals(listOf("رستوران", "سوپرمارکت"), subs.map { it.name })
        assertTrue(subcategoriesOf(categories, "حقوق").isEmpty())
    }

    @Test
    fun entryNameLists_excludeInvestmentButKeepFallback() {
        val categories = DEFAULT_CATEGORIES
        val expense = expenseCategoryNames(categories)
        val income = incomeCategoryNames(categories)
        INVESTMENT_CATEGORIES.forEach { name ->
            assertFalse(expense.contains(name))
            assertFalse(income.contains(name))
        }
        assertTrue(expense.contains(FALLBACK_CATEGORY))
        assertTrue(income.contains(FALLBACK_CATEGORY))
    }

    @Test
    fun investmentSeeds_areEightUniqueInvestmentMains() {
        assertEquals(8, INVESTMENT_CATEGORIES.size)
        assertEquals(8, INVESTMENT_CATEGORIES.toSet().size)
        INVESTMENT_CATEGORIES.forEach { name ->
            val entity = DEFAULT_CATEGORIES.first { it.name == name }
            assertEquals(CategoryType.INVESTMENT, categoryType(entity))
            assertEquals(null, entity.parentId)
        }
    }
}
