package com.polino.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** دارایی سرمایه‌گذاری: طلا، ارز، رمزارز، سهام و سایر. */
@Entity(tableName = "assets")
internal data class AssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val unit: String,
    val quantity: Double,
    val buyPrice: Double,
    val currentPrice: Double?,
    val accountId: String,
    val note: String = ""
)

@Dao
internal interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY name")
    suspend fun getAll(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: String)
}

/** ارزش روز دارایی: مقدار × قیمت روز، و اگر قیمت روز ندارد قیمت خرید. */
internal fun assetCurrentValue(asset: AssetEntity): Double =
    asset.quantity * (asset.currentPrice ?: asset.buyPrice)

/** جمع‌های پرتفوی: ارزش روز، بهای تمام‌شده، سود/زیان و درصد سود/زیان. */
internal data class PortfolioTotals(
    val totalValue: Double,
    val totalCost: Double,
    val profitLoss: Double,
    val profitLossPercent: Double
)

internal fun portfolioTotals(assets: List<AssetEntity>): PortfolioTotals {
    val totalValue = assets.sumOf { assetCurrentValue(it) }
    val totalCost = assets.sumOf { it.quantity * it.buyPrice }
    val profitLoss = totalValue - totalCost
    val percent = if (totalCost > 0) profitLoss / totalCost * 100 else 0.0
    return PortfolioTotals(totalValue, totalCost, profitLoss, percent)
}

/** اعتبارسنجی ورودی دارایی: مقدار باید مثبت و قیمت خرید نامنفی باشد. */
internal fun isValidAssetInput(quantity: Double, buyPrice: Double): Boolean =
    quantity > 0 && buyPrice >= 0
