package com.polino.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * طلب و بدهی به ازای هر شخص. تا وقتی تسویه نشده، در درآمد و هزینه واقعی
 * حساب نمی‌شود؛ با تسویه، یک تراکنش واقعی (درآمد برای طلب، هزینه برای بدهی)
 * ثبت می‌شود.
 */
@Entity(tableName = "debts")
internal data class DebtEntity(
    @PrimaryKey val id: String,
    val personName: String,
    val amount: Long,
    val isReceivable: Boolean,
    val settled: Boolean = false,
    val accountId: String = "main",
    val dueMillis: Long? = null
)

@Dao
internal interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY personName")
    suspend fun getAll(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: String)
}

/** جمع طلب‌های تسویه‌نشده و بدهی‌های تسویه‌نشده، به ترتیب. */
internal fun unsettledTotals(debts: List<DebtEntity>): Pair<Long, Long> {
    val open = debts.filter { !it.settled }
    return Pair(
        open.filter { it.isReceivable }.sumOf { it.amount },
        open.filter { !it.isReceivable }.sumOf { it.amount }
    )
}

/**
 * بدهی یا طلب تسویه‌نشده‌ای که سررسیدش از امروز گذشته است؛ بدون
 * سررسید هرگز سررسیدگذشته نیست، و سررسیدِ امروز هم هنوز
 * سررسیدگذشته نیست، چون سررسید تاریخ است نه ساعت — همان قاعده
 * یادآورهای چک و قبض.
 */
internal fun isDebtOverdue(dueMillis: Long?, nowMillis: Long, settled: Boolean): Boolean {
    if (settled || dueMillis == null) return false
    val startOfToday = nowMillis - Math.floorMod(nowMillis, 86_400_000L)
    return dueMillis < startOfToday
}
