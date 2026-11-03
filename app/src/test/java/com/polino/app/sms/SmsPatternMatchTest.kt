package com.polino.app.sms

import com.polino.app.data.SmsPatternEntity
import com.polino.app.data.smsPatternKeywords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsPatternMatchTest {

    private fun pattern(
        sender: String = "BANK-MELLAT",
        keywords: String = "برداشت، خرید",
        isFinancial: Boolean = true,
        isIncome: Boolean = false
    ) = SmsPatternEntity("p1", sender, keywords, isFinancial, isIncome)

    @Test
    fun senderAndKeywordMatch_applies() {
        val found = findMatchingPattern(
            listOf(pattern()),
            "BANK-MELLAT",
            "برداشت مبلغ 100000 تومان انجام شد"
        )
        assertEquals("p1", found?.id)
    }

    @Test
    fun senderMismatch_ignored() {
        assertNull(
            findMatchingPattern(
                listOf(pattern()),
                "OTHER-BANK",
                "برداشت مبلغ 100000 تومان انجام شد"
            )
        )
    }

    @Test
    fun keywordAbsent_noMatch() {
        assertNull(
            findMatchingPattern(
                listOf(pattern()),
                "BANK-MELLAT",
                "واریز مبلغ 100000 تومان انجام شد"
            )
        )
    }

    @Test
    fun emptyKeywords_matchOnSenderAlone() {
        val found = findMatchingPattern(
            listOf(pattern(keywords = "")),
            "BANK-MELLAT",
            "هر متنی"
        )
        assertEquals("p1", found?.id)
    }

    @Test
    fun keywords_splitOnCommaAndTrimmed() {
        assertEquals(listOf("برداشت", "خرید"), smsPatternKeywords("برداشت، خرید"))
        assertEquals(listOf("برداشت", "خرید"), smsPatternKeywords("برداشت, خرید "))
        assertEquals(emptyList<String>(), smsPatternKeywords(" ، "))
    }

    @Test
    fun nonFinancialPattern_skipsRecording() {
        assertFalse(shouldRecordSms(pattern(isFinancial = false)))
        assertTrue(shouldRecordSms(pattern(isFinancial = true)))
        assertTrue(shouldRecordSms(null))
    }

    @Test
    fun directionOverride_beatsParserDefault() {
        assertTrue(resolveSmsIncome(pattern(isIncome = true), false))
        assertFalse(resolveSmsIncome(pattern(isIncome = false), true))
        assertTrue(resolveSmsIncome(null, true))
    }

    @Test
    fun amountOnly_parsesWithoutDirection() {
        assertEquals(100000L, parseSmsAmount("مبلغ ۱۰۰٬۰۰۰ تومان ثبت شد"))
    }

    @Test
    fun amountOnly_rialConverts() {
        assertEquals(50000L, parseSmsAmount("مبلغ ۵۰۰٬۰۰۰ ریال ثبت شد"))
    }
}
