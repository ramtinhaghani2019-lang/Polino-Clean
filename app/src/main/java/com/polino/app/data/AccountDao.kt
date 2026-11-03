package com.polino.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY name")
    suspend fun getAll(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    /** انتقال همه تراکنش‌های یک حساب به حسابی دیگر (برای حذف حساب). */
    @Query("UPDATE transactions SET accountId = :newId WHERE accountId = :oldId")
    suspend fun reassignTransactions(oldId: String, newId: String)
}
