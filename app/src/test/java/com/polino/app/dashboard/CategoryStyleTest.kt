package com.polino.app.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import com.polino.app.data.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryStyleTest {

    @Test
    fun iconKeys_areUnique() {
        val keys = CATEGORY_ICON_OPTIONS.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun knownKey_resolvesToExpectedIcon() {
        assertEquals(Icons.Filled.ShoppingBag, categoryIconByKey("bag"))
        assertEquals(Icons.Filled.Flight, categoryIconByKey("flight"))
    }

    @Test
    fun unknownKey_returnsNull() {
        assertNull(categoryIconByKey("nope"))
    }

    @Test
    fun entityWithIconKey_usesIt() {
        val entity = CategoryEntity("خیریه", false, iconKey = "bag")
        assertEquals(Icons.Filled.ShoppingBag, categoryIconFor(entity))
    }

    @Test
    fun entityWithoutIconKey_fallsBackToNameIcon() {
        val entity = CategoryEntity("خیریه", false)
        assertEquals(categoryIcon("خیریه"), categoryIconFor(entity))
    }

    @Test
    fun entityWithUnknownIconKey_fallsBackToNameIcon() {
        val entity = CategoryEntity("خیریه", false, iconKey = "nope")
        assertEquals(categoryIcon("خیریه"), categoryIconFor(entity))
    }

    @Test
    fun palette_has13DistinctOpaqueColors() {
        assertEquals(13, CATEGORY_COLOR_PALETTE.size)
        assertEquals(13, CATEGORY_COLOR_PALETTE.toSet().size)
        CATEGORY_COLOR_PALETTE.forEach { argb ->
            assertEquals(0xFF000000L, argb and 0xFF000000L)
        }
    }

    @Test
    fun entityWithColor_usesIt() {
        val entity = CategoryEntity("خیریه", false, colorArgb = 0xFFE53935L)
        assertEquals(Color(0xFFE53935L), categoryColorFor(entity, Color.Blue))
    }

    @Test
    fun entityWithoutColor_usesFallback() {
        val entity = CategoryEntity("خیریه", false)
        assertEquals(Color.Blue, categoryColorFor(entity, Color.Blue))
    }
}
