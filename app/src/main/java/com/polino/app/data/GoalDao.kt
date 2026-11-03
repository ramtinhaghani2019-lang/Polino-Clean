package com.polino.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY name")
    suspend fun getAll(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)

    /** انتقال هدف‌های یک حساب به حسابی دیگر (برای حذف حساب). */
    @Query("UPDATE goals SET accountId = :newId WHERE accountId = :oldId")
    suspend fun reassignAccounts(oldId: String, newId: String)
}
