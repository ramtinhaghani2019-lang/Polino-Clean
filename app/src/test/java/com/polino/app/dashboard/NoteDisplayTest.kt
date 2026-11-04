package com.polino.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteDisplayTest {

    @Test
    fun emptyBodyStaysEmpty() {
        assertEquals("", noteSnippet(""))
    }

    @Test
    fun shortBodyUnchanged() {
        assertEquals("قسط ماشین", noteSnippet("قسط ماشین"))
    }

    @Test
    fun multilineCollapsedToSingleLine() {
        assertEquals("خط اول خط دوم", noteSnippet("خط اول\nخط دوم"))
    }

    @Test
    fun surroundingWhitespaceTrimmed() {
        assertEquals("متن", noteSnippet("  متن  "))
    }

    @Test
    fun longBodyTruncatedWithEllipsis() {
        val body = "الف".repeat(130)
        // 120 chars kept + "…" = 121 chars total.
        assertEquals("الف".repeat(120) + "…", noteSnippet(body))
    }

    @Test
    fun exactlyLimitBodyUnchanged() {
        val body = "ب".repeat(120)
        assertEquals(body, noteSnippet(body))
    }
}
