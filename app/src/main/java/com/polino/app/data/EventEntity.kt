package com.polino.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.time.Instant
import java.time.ZoneId

/** رویداد تقویمی: یادداشت یک روز مشخص، بدون سرویس پس‌زمینه. */
@Entity(tableName = "events")
internal data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dateMillis: Long,
    val note: String = ""
)

@Dao
internal interface EventDao {
    @Query("SELECT * FROM events ORDER BY dateMillis")
    suspend fun getAll(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)
}

/** رویدادهای همان روز تقویمیِ dayMillis (به وقت محلی دستگاه). */
internal fun eventsOnDay(events: List<EventEntity>, dayMillis: Long): List<EventEntity> {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(dayMillis).atZone(zone).toLocalDate()
    return events.filter {
        Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() == day
    }
}
