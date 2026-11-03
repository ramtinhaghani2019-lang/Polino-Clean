package com.polino.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun getForMonth(yearMonth: String): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BudgetEntity)

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth AND category = :category")
    suspend fun delete(yearMonth: String, category: String)

    /** حذف همه بودجه‌های یک دسته (برای حذف دسته). */
    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteForCategory(category: String)
}
