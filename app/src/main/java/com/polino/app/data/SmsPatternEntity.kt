package com.polino.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** الگوی پیامک: فرستنده + کلمات کلیدی + قانون تراکنش (مالی بودن و جهت). */
@Entity(tableName = "sms_patterns")
internal data class SmsPatternEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val keywords: String = "",
    val isFinancial: Boolean = true,
    val isIncome: Boolean = false
)

/**
 * الگوها بر اساس فرستنده مرتب می‌شوند و برای یک فرستنده، برداشت
 * (isIncome=false) پیش از واریز می‌آید — همان اولویت هزینه‌بر‌درآمد
 * در parseBankSms — تا پیامکی که هم «انتقال» دارد و هم «واریز»
 * (مثل تأیید انتقال وجه خروجی) برداشت شناخته شود، نه واریز.
 */
@Dao
internal interface SmsPatternDao {
    @Query("SELECT * FROM sms_patterns ORDER BY sender, isIncome ASC")
    suspend fun getAll(): List<SmsPatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SmsPatternEntity)

    @Query("DELETE FROM sms_patterns WHERE id = :id")
    suspend fun deleteById(id: String)
}

/** کلمات کلیدی الگو: با کاما (فارسی یا لاتین) جدا می‌شوند و خالی‌ها حذف می‌شوند. */
internal fun smsPatternKeywords(keywords: String): List<String> =
    keywords.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }
