package com.polino.app.sms

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsInboxScanTest {

    @Test
    fun detectSenders_groupsAndSortsByCountDesc() {
        val rows = listOf(
            InboxSmsRow("BankA", "واریز ۱۰۰ تومان", 1L),
            InboxSmsRow("BankB", "واریز ۲۰۰ تومان", 2L),
            InboxSmsRow("BankA", "برداشت ۵۰ تومان", 3L),
            InboxSmsRow("BankA", "برداشت ۷۰ تومان", 4L)
        )
        val detected = detectSenders(rows)
        assertEquals(2, detected.size)
        assertEquals(DetectedSender("BankA", 3), detected[0])
        assertEquals(DetectedSender("BankB", 1), detected[1])
    }

    @Test
    fun detectSenders_skipsBlankSendersAndTrims() {
        val rows = listOf(
            InboxSmsRow("  BankA  ", "واریز ۱۰۰ تومان", 1L),
            InboxSmsRow("", "واریز ۲۰۰ تومان", 2L),
            InboxSmsRow("   ", "واریز ۳۰۰ تومان", 3L)
        )
        val detected = detectSenders(rows)
        assertEquals(listOf(DetectedSender("BankA", 1)), detected)
    }

    @Test
    fun detectSenders_emptyInbox_returnsEmpty() {
        assertEquals(emptyList<DetectedSender>(), detectSenders(emptyList()))
    }

    @Test
    fun isSmsDuplicate_matchesTitleAmountIncomeAccountAndWindow() {
        val existing = listOf(
            MoneyTransaction(
                id = "1",
                title = "تراکنش بانکی",
                amount = 50000L,
                income = true,
                category = "سایر",
                dateMillis = 1000L
            )
        )
        assertTrue(isSmsDuplicate(existing, 50000L, true, "main", 1000L))
        assertFalse(isSmsDuplicate(existing, 50001L, true, "main", 1000L))
        assertFalse(isSmsDuplicate(existing, 50000L, false, "main", 1000L))
        assertFalse(isSmsDuplicate(existing, 50000L, true, "other", 1000L))
    }

    @Test
    fun isSmsDuplicate_windowCoversLiveVsRescanSkew() {
        // دریافت زنده زمان PDU و اسکن صندوق ستون date را می‌بینند؛
        // اختلاف ۴۵ ثانیه‌ای همان پیامک هنوز تکراری است؛ درست در مرز
        // پنجره هم تکراری است و یک میلی‌ثانیه بعد از آن نه.
        val existing = listOf(
            MoneyTransaction(
                id = "1",
                title = "تراکنش بانکی",
                amount = 128000L,
                income = false,
                category = "سایر",
                dateMillis = 1_000_000L
            )
        )
        assertTrue(isSmsDuplicate(existing, 128000L, false, "main", 1_045_000L))
        assertTrue(
            isSmsDuplicate(existing, 128000L, false, "main", 1_000_000L + SMS_DUPLICATE_WINDOW_MILLIS)
        )
        assertFalse(
            isSmsDuplicate(existing, 128000L, false, "main", 1_000_000L + SMS_DUPLICATE_WINDOW_MILLIS + 1)
        )
    }

    @Test
    fun isSmsDuplicate_sameAmountDifferentTransactionsAreNotDuplicates() {
        // دو تراکنش واقعاً جدا با همان مبلغ در یک روز، با فاصله بیش از پنجره.
        val existing = listOf(
            MoneyTransaction(
                id = "1",
                title = "تراکنش بانکی",
                amount = 128000L,
                income = false,
                category = "سایر",
                dateMillis = 1_000_000L
            )
        )
        assertFalse(isSmsDuplicate(existing, 128000L, false, "main", 1_000_000L + 10 * 60 * 1000L))
    }

    @Test
    fun isSmsDuplicate_multipartSameSmsIsDuplicate() {
        // بخش‌های یک پیامک چندپاره همان زمان را دارند؛ بخش دوم تکراری است.
        val existing = listOf(
            MoneyTransaction(
                id = "1",
                title = "تراکنش بانکی",
                amount = 128000L,
                income = false,
                category = "سایر",
                dateMillis = 2_000_000L
            )
        )
        assertTrue(isSmsDuplicate(existing, 128000L, false, "main", 2_000_000L))
    }

    @Test
    fun isSmsDuplicate_ignoresNonBankTitles() {
        val existing = listOf(
            MoneyTransaction(
                id = "1",
                title = "خرید",
                amount = 50000L,
                income = false,
                category = "سایر",
                dateMillis = 1000L
            )
        )
        assertFalse(isSmsDuplicate(existing, 50000L, false, "main", 1000L))
    }
}
