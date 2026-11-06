package com.polino.app.data

import com.polino.app.sms.findMatchingPattern
import com.polino.app.sms.parseSmsAmount
import com.polino.app.sms.resolveSmsIncome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSmsPatternsTest {

    /** ترتیب خروجی DAO: فرستنده، بعد برداشت پیش از واریز. */
    private fun daoOrdered() = DEFAULT_SMS_PATTERNS.sortedWith(
        compareBy({ it.sender }, { it.isIncome })
    )

    @Test
    fun defaults_haveThirtySixRowsWithUniqueIds() {
        assertEquals(36, DEFAULT_SMS_PATTERNS.size)
        assertEquals(36, DEFAULT_SMS_PATTERNS.map { it.id }.toSet().size)
    }

    @Test
    fun defaults_coverEighteenSendersWithBothDirections() {
        val senders = DEFAULT_SMS_PATTERNS.map { it.sender }.toSet()
        assertEquals(18, senders.size)
        senders.forEach { sender ->
            val rows = DEFAULT_SMS_PATTERNS.filter { it.sender == sender }
            assertEquals(2, rows.size)
            assertTrue(rows.any { it.isIncome })
            assertTrue(rows.any { !it.isIncome })
        }
    }

    @Test
    fun depositKeywords_matchParserIncomeWords() {
        DEFAULT_SMS_PATTERNS.filter { it.isIncome }.forEach {
            assertEquals("واریز، دریافت", it.keywords)
            assertTrue(it.isFinancial)
        }
    }

    @Test
    fun withdrawalKeywords_matchParserExpenseWords() {
        DEFAULT_SMS_PATTERNS.filter { !it.isIncome }.forEach {
            assertEquals("برداشت، کسر، خرید، پرداخت، انتقال", it.keywords)
            assertTrue(it.isFinancial)
        }
    }

    @Test
    fun groundedSenders_present() {
        val senders = DEFAULT_SMS_PATTERNS.map { it.sender }.toSet()
        listOf(
            "200000", "10000000", "B.Pasargad", "300017",
            "200070", "200033", "300078"
        ).forEach { assertTrue(senders.contains(it)) }
    }

    @Test
    fun missingOnEmpty_returnsAllDefaults() {
        assertEquals(DEFAULT_SMS_PATTERNS, missingDefaultSmsPatterns(emptyList()))
    }

    @Test
    fun missingOnSecondRun_returnsEmpty() {
        assertEquals(
            emptyList<SmsPatternEntity>(),
            missingDefaultSmsPatterns(DEFAULT_SMS_PATTERNS)
        )
    }

    @Test
    fun userPatternWithSameSender_neverOverwritten() {
        val userPattern = SmsPatternEntity("user-1", "300017", "واریز حقوق", true, true)
        val missing = missingDefaultSmsPatterns(listOf(userPattern))
        // ردیف کاربر هرگز در فهرست بذر نیست (شناسه‌ها فرق می‌کنند) …
        assertTrue(missing.none { it.id == "user-1" })
        // … و الگوهای پیش‌فرض همان فرستنده با شناسه خودشان پیشنهاد می‌شوند.
        assertTrue(missing.any { it.id == "default-melli-deposit" })
        assertTrue(missing.any { it.id == "default-melli-withdrawal" })
    }

    @Test
    fun melliDepositSms_matchesDepositPatternAndParses() {
        val body = "واریز به حساب 1234 مبلغ 2,500,000 تومان - مانده: 10,000,000 تومان"
        val found = findMatchingPattern(daoOrdered(), "300017", body)
        assertEquals("default-melli-deposit", found?.id)
        assertEquals(2500000L, parseSmsAmount(body))
        assertTrue(resolveSmsIncome(found, false))
    }

    @Test
    fun melliWithdrawalSms_matchesWithdrawalPatternAndParses() {
        val body = "برداشت از حساب 1234 مبلغ 500,000 تومان - مانده: 9,500,000 تومان"
        val found = findMatchingPattern(daoOrdered(), "300017", body)
        assertEquals("default-melli-withdrawal", found?.id)
        assertEquals(500000L, parseSmsAmount(body))
        assertFalse(resolveSmsIncome(found, true))
    }

    @Test
    fun transferOutMentioningDeposit_stillResolvesWithdrawal() {
        val body = "انتقال وجه از کارت شما به کارت 603799 مبلغ 1,000,000 تومان " +
            "انجام شد؛ وجه به حساب مقصد واریز شد"
        val found = findMatchingPattern(daoOrdered(), "200033", body)
        assertEquals("default-mellat-withdrawal", found?.id)
    }

    @Test
    fun pasargadAlphaSender_matchesDeposit() {
        val body = "واریز به کارت شما مبلغ 3,000,000 تومان انجام شد"
        val found = findMatchingPattern(daoOrdered(), "B.Pasargad", body)
        assertEquals("default-pasargad-alpha-deposit", found?.id)
    }
}
