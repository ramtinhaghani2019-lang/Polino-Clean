package com.polino.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface RecurringDao {

    @Query("SELECT * FROM recurring ORDER BY title")
    suspend fun getAll(): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RecurringEntity)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun deleteById(id: String)
}
