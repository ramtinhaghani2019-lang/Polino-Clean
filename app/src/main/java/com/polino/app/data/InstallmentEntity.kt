package com.polino.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "installments")
internal data class InstallmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val totalAmount: Long,
    val monthCount: Int,
    val paidCount: Int = 0,
    val bankName: String? = null,
    val installmentAmount: Long = 0L,
    val startMillis: Long? = null,
    val accountId: String = "main"
)

@Dao
internal interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY title")
    suspend fun getAll(): List<InstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InstallmentEntity)

    @Query("DELETE FROM installments WHERE id = :id")
    suspend fun deleteById(id: String)
}

/** مبلغ هر قسط: تقسیم صحیح؛ باقیمانده در قسط آخر می‌نشیند. */
internal fun installmentMonthlyAmount(totalAmount: Long, monthCount: Int): Long =
    if (monthCount <= 0) 0L else totalAmount / monthCount

/** پیشرفت پرداخت، بین ۰ و ۱. */
internal fun installmentProgressFraction(paidCount: Int, monthCount: Int): Float =
    if (monthCount <= 0) 0f
    else (paidCount.coerceIn(0, monthCount).toFloat() / monthCount)
