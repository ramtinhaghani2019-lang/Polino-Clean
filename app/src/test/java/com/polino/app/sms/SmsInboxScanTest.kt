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
    fun isSmsDuplicate_matchesTitleAmountAndTimestamp() {
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
        assertTrue(isSmsDuplicate(existing, 50000L, 1000L))
        assertFalse(isSmsDuplicate(existing, 50001L, 1000L))
        assertFalse(isSmsDuplicate(existing, 50000L, 1001L))
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
        assertFalse(isSmsDuplicate(existing, 50000L, 1000L))
    }
}
