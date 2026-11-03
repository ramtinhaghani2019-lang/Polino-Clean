package com.polino.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun getForMonth(yearMonth: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BudgetEntity)

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth AND category = :category")
    suspend fun delete(yearMonth: String, category: String)

    /** حذف همه بودجه‌های یک دسته (برای حذف دسته). */
    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteForCategory(category: String)

    /** تغییر نام دسته در همه بودجه‌ها (برای تغییر نام دسته). */
    @Query("UPDATE budgets SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)
}
