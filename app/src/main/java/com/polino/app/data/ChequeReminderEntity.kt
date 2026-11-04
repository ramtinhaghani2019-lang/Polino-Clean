package com.polino.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * یادآور چک و قبض: جدا از کارهای عمومی؛ سررسیدهای مالی در بخش
 * مدیریت برجسته می‌شوند و مبلغ سررسید باز جمع زده می‌شود.
 * تسویه فقط یادآور را پرداخت‌شده می‌کند و تراکنشی ثبت نمی‌کند.
 */
@Entity(tableName = "cheque_reminders")
internal data class ChequeReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Long,
    val dueMillis: Long,
    val settled: Boolean = false,
    val note: String = ""
)

@Dao
internal interface ChequeReminderDao {
    @Query("SELECT * FROM cheque_reminders ORDER BY dueMillis")
    suspend fun getAll(): List<ChequeReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ChequeReminderEntity)

    @Query("DELETE FROM cheque_reminders WHERE id = :id")
    suspend fun deleteById(id: String)
}

/**
 * یادآور تسویه‌نشده‌ای که سررسیدش از امروز گذشته است؛ سررسیدِ امروز
 * هنوز سررسیدگذشته نیست، چون سررسید تاریخ است نه ساعت.
 */
internal fun isChequeReminderOverdue(dueMillis: Long, nowMillis: Long, settled: Boolean): Boolean {
    if (settled) return false
    val startOfToday = nowMillis - Math.floorMod(nowMillis, 86_400_000L)
    return dueMillis < startOfToday
}

/** جمع مبلغ یادآورهای باز (تسویه‌نشده). */
internal fun openChequeRemindersTotal(items: List<ChequeReminderEntity>): Long =
    items.filter { !it.settled }.sumOf { it.amount }
