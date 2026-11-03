package com.polino.app.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun deposit_parsesAmountBeforeBalance() {
        val parsed = parseBankSms(
            "09123456789",
            "واریز مبلغ ۲٬۵۰۰٬۰۰۰ تومان به حساب شما انجام شد. مانده: ۱۲٬۰۰۰٬۰۰۰ تومان"
        )
        assertEquals(2500000L, parsed?.amount)
        assertEquals(true, parsed?.income)
    }

    @Test
    fun purchase_parsesAsExpense() {
        val parsed = parseBankSms(
            "Bank",
            "خرید به مبلغ ۳۵۰٬۰۰۰ تومان از کارت شما انجام شد. مانده حساب: ۱٬۰۰۰٬۰۰۰ تومان"
        )
        assertEquals(350000L, parsed?.amount)
        assertEquals(false, parsed?.income)
    }

    @Test
    fun rialAmount_convertsToToman() {
        val parsed = parseBankSms(
            "Bank",
            "برداشت مبلغ ۵٬۰۰۰٬۰۰۰ ریال از حساب شما"
        )
        assertEquals(500000L, parsed?.amount)
        assertEquals(false, parsed?.income)
    }

    @Test
    fun arabicDigits_parsed() {
        val parsed = parseBankSms(
            "Bank",
            "واریز ١٢٣٤٥ تومان به حساب شما"
        )
        assertEquals(12345L, parsed?.amount)
        assertEquals(true, parsed?.income)
    }

    @Test
    fun noAmount_returnsNull() {
        assertNull(parseBankSms("Bank", "واریز به حساب شما انجام شد"))
    }

    @Test
    fun noDirection_returnsNull() {
        assertNull(parseBankSms("Bank", "مبلغ ۱۰۰٬۰۰۰ تومان ثبت شد"))
    }

    @Test
    fun balanceOnly_returnsNull() {
        assertNull(parseBankSms("Bank", "مانده حساب شما: ۲٬۰۰۰٬۰۰۰ تومان"))
    }
}
