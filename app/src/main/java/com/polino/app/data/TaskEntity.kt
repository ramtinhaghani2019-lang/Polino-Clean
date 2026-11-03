package com.polino.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * کار و یادآور: بدون سرویس پس‌زمینه؛ کارهای سررسیدگذشته در بخش مدیریت
 * برجسته می‌شوند.
 */
@Entity(tableName = "tasks")
internal data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueMillis: Long,
    val done: Boolean = false
)

@Dao
internal interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueMillis")
    suspend fun getAll(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}

/** کار انجام‌نشده‌ای که سررسیدش گذشته است. */
internal fun isTaskOverdue(dueMillis: Long, nowMillis: Long, done: Boolean): Boolean =
    !done && dueMillis < nowMillis
