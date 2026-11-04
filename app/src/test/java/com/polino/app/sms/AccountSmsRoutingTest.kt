package com.polino.app.sms

import com.polino.app.data.AccountSmsSenderEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountSmsRoutingTest {

    private fun link(accountId: String, sender: String) =
        AccountSmsSenderEntity(accountId, sender)

    @Test
    fun linkedSender_routesToItsAccount() {
        val links = listOf(link("wallet-1", "Bank"), link("main", "Other"))
        assertEquals("wallet-1", resolveSmsAccountId(links, "Bank"))
    }

    @Test
    fun match_isCaseInsensitiveSubstring() {
        val links = listOf(link("wallet-1", "pasargad"))
        assertEquals("wallet-1", resolveSmsAccountId(links, "B.Pasargad"))
    }

    @Test
    fun noMatch_fallsBackToMain() {
        val links = listOf(link("wallet-1", "Bank"))
        assertEquals("main", resolveSmsAccountId(links, "UnknownSender"))
    }

    @Test
    fun emptyLinks_fallBackToMain() {
        assertEquals("main", resolveSmsAccountId(emptyList(), "Bank"))
    }

    @Test
    fun blankSenderLink_isIgnored() {
        val links = listOf(link("wallet-1", "   "))
        assertEquals("main", resolveSmsAccountId(links, "Bank"))
    }

    @Test
    fun emptyOriginator_fallsBackToMain() {
        val links = listOf(link("wallet-1", "Bank"))
        assertEquals("main", resolveSmsAccountId(links, ""))
    }

    @Test
    fun firstMatchingLink_wins() {
        val links = listOf(link("a", "Bank"), link("b", "ank"))
        assertEquals("a", resolveSmsAccountId(links, "Bank"))
    }
}
