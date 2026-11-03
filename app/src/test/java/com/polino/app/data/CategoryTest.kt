package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun defaultCategories_haveNoDuplicateNames() {
        val names = DEFAULT_CATEGORIES.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun fallbackCategory_isAvailableForBothTypes() {
        val categories = DEFAULT_CATEGORIES
        assertTrue(expenseCategoryNames(categories).contains(FALLBACK_CATEGORY))
        assertTrue(incomeCategoryNames(categories).contains(FALLBACK_CATEGORY))
    }

    @Test
    fun onlyFallbackCategory_isProtectedFromDeletion() {
        assertFalse(canDeleteCategory(FALLBACK_CATEGORY))
        assertTrue(canDeleteCategory("خوراک"))
    }
}
