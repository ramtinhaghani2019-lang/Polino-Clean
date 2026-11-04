package com.polino.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteByName(name: String)

    /** هنگام تغییر نام دسته اصلی، زیردسته‌هایش به نام جدید وصل می‌شوند. */
    @Query("UPDATE categories SET parentId = :newName WHERE parentId = :oldName")
    suspend fun reassignParent(oldName: String, newName: String)
}
