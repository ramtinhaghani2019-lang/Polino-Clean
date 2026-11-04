package com.polino.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinLockTest {

    @Test
    fun hash_isDeterministicForSamePinAndSalt() {
        assertEquals(hashPin("1234", "salt-a"), hashPin("1234", "salt-a"))
    }

    @Test
    fun hash_differsAcrossSaltsAndPins() {
        assertNotEquals(hashPin("1234", "salt-a"), hashPin("1234", "salt-b"))
        assertNotEquals(hashPin("1234", "salt-a"), hashPin("5678", "salt-a"))
    }

    @Test
    fun hash_isSixtyFourHexChars() {
        val hash = hashPin("123456", "salt-a")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun verify_acceptsCorrectPinAndRejectsOthers() {
        val salt = "salt-a"
        val hash = hashPin("1234", salt)
        assertTrue(verifyPin("1234", salt, hash))
        assertFalse(verifyPin("1235", salt, hash))
        assertFalse(verifyPin("1234", "salt-b", hash))
        assertFalse(verifyPin("", salt, hash))
        assertFalse(verifyPin("1234", salt, hash.dropLast(1) + "0"))
        assertFalse(verifyPin("1234", "", hash))
        assertFalse(verifyPin("1234", salt, ""))
    }

    @Test
    fun verify_acceptsPersianDigitsForSamePin() {
        val salt = "salt-a"
        val hash = hashPin("1234", salt)
        assertTrue(verifyPin("۱۲۳۴", salt, hash))
    }

    @Test
    fun isValidPin_acceptsFourToSixDigitsOnly() {
        assertTrue(isValidPin("1234"))
        assertTrue(isValidPin("123456"))
        assertTrue(isValidPin("۱۲۳۴"))
        assertFalse(isValidPin("123"))
        assertFalse(isValidPin("1234567"))
        assertFalse(isValidPin("12a4"))
        assertFalse(isValidPin(""))
    }

    @Test
    fun generateSalt_isNonBlankAndUnique() {
        val first = generatePinSalt()
        val second = generatePinSalt()
        assertTrue(first.isNotBlank())
        assertNotEquals(first, second)
    }

    @Test
    fun normalizePinDigits_convertsPersianAndArabicDigits() {
        assertEquals("1234", normalizePinDigits("۱۲۳۴"))
        assertEquals("1234", normalizePinDigits("١٢٣٤"))
        assertEquals("12a4", normalizePinDigits("12a4"))
    }
}
