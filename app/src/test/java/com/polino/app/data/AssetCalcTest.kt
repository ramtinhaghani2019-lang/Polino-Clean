package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetCalcTest {

    private fun asset(
        name: String,
        quantity: Double,
        buyPrice: Double,
        currentPrice: Double?
    ) = AssetEntity(
        id = name,
        name = name,
        type = "طلا",
        unit = "گرم",
        quantity = quantity,
        buyPrice = buyPrice,
        currentPrice = currentPrice,
        accountId = "main"
    )

    @Test
    fun mixedAssets_totals() {
        val assets = listOf(
            asset("a", 10.0, 100.0, 120.0),
            asset("b", 2.0, 500.0, 450.0)
        )
        val totals = portfolioTotals(assets)
        assertEquals(2100.0, totals.totalValue, 0.001)
        assertEquals(2000.0, totals.totalCost, 0.001)
        assertEquals(100.0, totals.profitLoss, 0.001)
        assertEquals(5.0, totals.profitLossPercent, 0.001)
    }

    @Test
    fun profit_positiveAndNegative() {
        val gain = portfolioTotals(listOf(asset("g", 1.0, 100.0, 150.0)))
        assertEquals(50.0, gain.profitLoss, 0.001)
        assertEquals(50.0, gain.profitLossPercent, 0.001)
        val loss = portfolioTotals(listOf(asset("l", 1.0, 100.0, 70.0)))
        assertEquals(-30.0, loss.profitLoss, 0.001)
        assertEquals(-30.0, loss.profitLossPercent, 0.001)
    }

    @Test
    fun missingCurrentPrice_fallsBackToBuyPrice() {
        val item = asset("c", 4.0, 250.0, null)
        assertEquals(1000.0, assetCurrentValue(item), 0.001)
        val totals = portfolioTotals(listOf(item))
        assertEquals(0.0, totals.profitLoss, 0.001)
        assertEquals(0.0, totals.profitLossPercent, 0.001)
    }

    @Test
    fun invalidInput_rejected() {
        assertEquals(false, isValidAssetInput(0.0, 100.0))
        assertEquals(false, isValidAssetInput(-2.0, 100.0))
        assertEquals(false, isValidAssetInput(2.0, -1.0))
        assertEquals(true, isValidAssetInput(2.0, 0.0))
        assertEquals(true, isValidAssetInput(2.0, 100.0))
    }

    @Test
    fun emptyPortfolio_zeroTotals() {
        val totals = portfolioTotals(emptyList())
        assertEquals(0.0, totals.totalValue, 0.001)
        assertEquals(0.0, totals.totalCost, 0.001)
        assertEquals(0.0, totals.profitLoss, 0.001)
        assertEquals(0.0, totals.profitLossPercent, 0.001)
    }
}
