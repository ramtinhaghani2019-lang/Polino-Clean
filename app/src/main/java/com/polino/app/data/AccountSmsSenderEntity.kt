package com.polino.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * اتصال یک سرشماره پیامک بانکی به یک حساب: پیامک‌های دریافتی از این
 * فرستنده به‌طور خودکار به همین حساب می‌روند. یک حساب می‌تواند چند
 * سرشماره داشته باشد و یک سرشماره به یک حساب وصل است.
 */
@Entity(
    tableName = "account_sms_senders",
    primaryKeys = ["accountId", "sender"]
)
internal data class AccountSmsSenderEntity(
    val accountId: String,
    val sender: String
)

@Dao
internal interface AccountSmsSenderDao {

    @Query("SELECT * FROM account_sms_senders ORDER BY accountId, sender")
    suspend fun getAll(): List<AccountSmsSenderEntity>

    @Query("SELECT * FROM account_sms_senders WHERE accountId = :accountId ORDER BY sender")
    suspend fun getForAccount(accountId: String): List<AccountSmsSenderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: AccountSmsSenderEntity)

    @Query("DELETE FROM account_sms_senders WHERE accountId = :accountId AND sender = :sender")
    suspend fun deleteByAccountAndSender(accountId: String, sender: String)

    @Query("DELETE FROM account_sms_senders WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
