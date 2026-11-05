package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryParentTest {

    private val sample = listOf(
        CategoryEntity("خوراک", false),
        CategoryEntity("رستوران", false, false, "خوراک"),
        CategoryEntity("فست‌فود", false, false, "رستوران"),
        CategoryEntity("حمل", false),
        CategoryEntity("حقوق", true),
        CategoryEntity("سایر", false)
    )

    @Test
    fun typeFlags_roundTripThroughCategoryType() {
        CategoryType.entries.forEach { type ->
            val (isIncome, isInvestment) = typeFlags(type)
            assertEquals(type, categoryType(CategoryEntity("x", isIncome, isInvestment)))
        }
    }

    @Test
    fun descendants_areTransitive() {
        assertEquals(setOf("رستوران", "فست‌فود"), descendantNames(sample, "خوراک"))
        assertEquals(setOf("فست‌فود"), descendantNames(sample, "رستوران"))
        assertEquals(emptySet<String>(), descendantNames(sample, "حمل"))
        assertEquals(emptySet<String>(), descendantNames(sample, "ناموجود"))
    }

    @Test
    fun parentCandidates_excludeSelfNonMainsAndOtherTypes() {
        val names = parentCandidates(sample, "حمل", CategoryType.EXPENSE).map { it.name }
        assertEquals(listOf("خوراک", "سایر"), names)
    }

    @Test
    fun parentCandidates_followSelectedType() {
        assertEquals(listOf("حقوق"), parentCandidates(sample, "حمل", CategoryType.INCOME).map { it.name })
        assertEquals(emptyList<String>(), parentCandidates(sample, "حمل", CategoryType.INVESTMENT).map { it.name })
    }

    @Test
    fun parentCandidates_emptyWhenSelfHasSubcategories() {
        assertTrue(parentCandidates(sample, "خوراک", CategoryType.EXPENSE).isEmpty())
        assertTrue(parentCandidates(sample, "رستوران", CategoryType.EXPENSE).isEmpty())
    }

    @Test
    fun parentCandidates_emptyWhenOnlyMainIsSelf() {
        assertTrue(parentCandidates(sample, "حقوق", CategoryType.INCOME).isEmpty())
    }

    @Test
    fun isValidParent_acceptsMainOfSameTypeAndNull() {
        assertTrue(isValidParent(sample, "حمل", CategoryType.EXPENSE, null))
        assertTrue(isValidParent(sample, "حمل", CategoryType.EXPENSE, "خوراک"))
    }

    @Test
    fun isValidParent_rejectsSelfNonMainWrongTypeAndMissing() {
        assertFalse(isValidParent(sample, "حمل", CategoryType.EXPENSE, "حمل"))
        assertFalse(isValidParent(sample, "حمل", CategoryType.EXPENSE, "رستوران"))
        assertFalse(isValidParent(sample, "حمل", CategoryType.EXPENSE, "حقوق"))
        assertFalse(isValidParent(sample, "حمل", CategoryType.EXPENSE, "ناموجود"))
    }

    @Test
    fun isValidParent_rejectsParentWhenSelfHasSubcategories() {
        assertFalse(isValidParent(sample, "خوراک", CategoryType.EXPENSE, "حمل"))
        assertFalse(isValidParent(sample, "خوراک", CategoryType.EXPENSE, "رستوران"))
        assertTrue(isValidParent(sample, "خوراک", CategoryType.EXPENSE, null))
    }
}
