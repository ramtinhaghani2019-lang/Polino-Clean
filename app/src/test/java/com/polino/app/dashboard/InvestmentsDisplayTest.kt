package com.polino.app.dashboard

import com.polino.app.data.AssetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class InvestmentsDisplayTest {

    private fun asset(
        name: String,
        type: String,
        unit: String
    ) = AssetEntity(
        id = "$type-$unit-$name",
        name = name,
        type = type,
        unit = unit,
        quantity = 1.0,
        buyPrice = 100.0,
        currentPrice = null,
        accountId = "main"
    )

    @Test
    fun stockSubtypeFollowsUnit() {
        assertEquals("سهام", assetSubtypeLabel(asset("x", "سهام و سایر", "سهم")))
        assertEquals(
            "صندوق سرمایه‌گذاری",
            assetSubtypeLabel(asset("x", "سهام و سایر", "واحد صندوق"))
        )
        assertEquals("اوراق مشارکت", assetSubtypeLabel(asset("x", "سهام و سایر", "واحد")))
    }

    @Test
    fun otherTypeHasOtherAssetsSubtypeAndPlainTypesHaveNone() {
        assertEquals("سایر دارایی‌ها", assetSubtypeLabel(asset("x", "سایر", "عدد")))
        assertEquals("", assetSubtypeLabel(asset("x", "طلا", "گرم")))
        assertEquals("", assetSubtypeLabel(asset("x", "ارز دیجیتال", "BTC")))
    }

    @Test
    fun groupOrderMatchesPergarTypeOrder() {
        val types = listOf("سایر", "سهام و سایر", "ارز دیجیتال", "ارز", "طلا")
        assertEquals(
            listOf("طلا", "ارز", "ارز دیجیتال", "سهام و سایر", "سایر"),
            types.sortedBy { investmentGroupOrder(it) }
        )
    }

    @Test
    fun cryptoCoinUnitsAreDistinctInFirstAppearanceOrder() {
        val assets = listOf(
            asset("بیت‌کوین", "ارز دیجیتال", "BTC"),
            asset("دلار", "ارز", "دلار"),
            asset("اتریوم", "ارز دیجیتال", "ETH"),
            asset("بیت‌کوین ۲", "ارز دیجیتال", "BTC")
        )
        assertEquals(listOf("BTC", "ETH"), cryptoCoinUnits(assets))
    }

    @Test
    fun cryptoCoinUnitsEmptyWithoutCrypto() {
        val assets = listOf(asset("سکه", "طلا", "گرم"))
        assertEquals(emptyList<String>(), cryptoCoinUnits(assets))
    }
}
