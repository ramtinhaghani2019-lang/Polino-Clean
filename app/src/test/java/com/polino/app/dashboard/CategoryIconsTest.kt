package com.polino.app.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Work
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryIconsTest {

    @Test
    fun knownCategories_mapToExpectedIcons() {
        assertEquals(Icons.Filled.Restaurant, categoryIcon("خوراک"))
        assertEquals(Icons.Filled.Work, categoryIcon("حقوق"))
        assertEquals(Icons.Filled.CardGiftcard, categoryIcon("هدیه"))
    }

    @Test
    fun unknownCategory_fallsBackToGenericIcon() {
        assertEquals(Icons.Filled.Category, categoryIcon("چیزی که نیست"))
    }
}
