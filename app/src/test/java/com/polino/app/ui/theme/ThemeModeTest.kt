package com.polino.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun dark_alwaysDark() {
        assertTrue(isDarkThemeForMode("dark", false))
        assertTrue(isDarkThemeForMode("dark", true))
    }

    @Test
    fun light_alwaysLight() {
        assertFalse(isDarkThemeForMode("light", false))
        assertFalse(isDarkThemeForMode("light", true))
    }

    @Test
    fun system_followsDevice() {
        assertTrue(isDarkThemeForMode("system", true))
        assertFalse(isDarkThemeForMode("system", false))
    }

    @Test
    fun unknown_fallsBackToSystem() {
        assertTrue(isDarkThemeForMode("bogus", true))
        assertFalse(isDarkThemeForMode("bogus", false))
    }
}
