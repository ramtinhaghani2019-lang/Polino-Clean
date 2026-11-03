package com.polino.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceAmountTest {

    @Test
    fun thousandMultiplier_applied() {
        assertEquals(500000L, parseSpokenAmount("500 هزار تومان"))
    }

    @Test
    fun millionMultiplier_applied() {
        assertEquals(2000000L, parseSpokenAmount("2 میلیون تومان"))
    }

    @Test
    fun plainToman_kept() {
        assertEquals(350000L, parseSpokenAmount("350000 تومان"))
    }

    @Test
    fun persianDigitsAndRial_converted() {
        assertEquals(500000L, parseSpokenAmount("۵٬۰۰۰٬۰۰۰ ریال"))
    }

    @Test
    fun noDigits_returnsNull() {
        assertNull(parseSpokenAmount("سلام"))
    }

    @Test
    fun wordOnlyNumber_returnsNull() {
        assertNull(parseSpokenAmount("پانصد هزار تومان"))
    }
}
