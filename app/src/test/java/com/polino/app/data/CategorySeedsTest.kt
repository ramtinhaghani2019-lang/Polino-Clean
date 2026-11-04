package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySeedsTest {

    @Test
    fun expenseSeeds_matchPergarNineMainsInOrder() {
        val expenseMains = mainCategories(DEFAULT_CATEGORIES, CategoryType.EXPENSE)
            .map { it.name }
            .filter { it != FALLBACK_CATEGORY }
        assertEquals(
            listOf(
                "درمان و سلامت",
                "تفریح و ورزش",
                "خریدهای روزانه",
                "تعهدات و اقساط",
                "خوراک و پوشاک",
                "حمل و نقل",
                "قبوض و ارتباطات",
                "خیریه",
                "اقساط"
            ),
            expenseMains
        )
    }

    @Test
    fun incomeSeeds_matchPergarThreeMainsInOrder() {
        val incomeMains = mainCategories(DEFAULT_CATEGORIES, CategoryType.INCOME).map { it.name }
        assertEquals(
            listOf("حقوق و دستمزد", "پاداش و کارانه", "فروش و درآمد جانبی"),
            incomeMains
        )
    }

    @Test
    fun investmentSeeds_unchangedEightMains() {
        val investmentMains = mainCategories(DEFAULT_CATEGORIES, CategoryType.INVESTMENT).map { it.name }
        assertEquals(INVESTMENT_CATEGORIES, investmentMains)
    }

    @Test
    fun fallbackCategory_stillPresentAndProtected() {
        val all = DEFAULT_CATEGORIES.map { it.name }
        assertTrue(all.contains(FALLBACK_CATEGORY))
        assertTrue(mainCategories(DEFAULT_CATEGORIES, CategoryType.EXPENSE).any { it.name == FALLBACK_CATEGORY })
    }

    @Test
    fun tabFiltering_groupsSyntheticCategoriesByType() {
        val categories = listOf(
            CategoryEntity("درمان و سلامت", false),
            CategoryEntity("بیمارستان", false, false, "درمان و سلامت"),
            CategoryEntity("حقوق و دستمزد", true),
            CategoryEntity("طلا و جواهر", false, true)
        )
        assertEquals(
            listOf("درمان و سلامت"),
            mainCategories(categories, CategoryType.EXPENSE).map { it.name }
        )
        assertEquals(
            listOf("حقوق و دستمزد"),
            mainCategories(categories, CategoryType.INCOME).map { it.name }
        )
        assertEquals(
            listOf("طلا و جواهر"),
            mainCategories(categories, CategoryType.INVESTMENT).map { it.name }
        )
        assertEquals(
            listOf("بیمارستان"),
            subcategoriesOf(categories, "درمان و سلامت").map { it.name }
        )
    }
}
