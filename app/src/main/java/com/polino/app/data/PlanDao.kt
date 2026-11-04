package com.polino.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface PlanDao {

    @Query("SELECT * FROM plans ORDER BY done ASC, title")
    suspend fun getAll(): List<PlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlanEntity)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deleteById(id: String)
}
