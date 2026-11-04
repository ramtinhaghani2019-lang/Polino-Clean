package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
internal data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Long,
    val income: Boolean,
    val category: String,
    val dateMillis: Long,
    val accountId: String,
    val note: String,
    val store: String = ""
)

internal fun MoneyTransaction.toEntity(): TransactionEntity =
    TransactionEntity(id, title, amount, income, category, dateMillis, accountId, note, store)

internal fun TransactionEntity.toDomain(): MoneyTransaction =
    MoneyTransaction(id, title, amount, income, category, dateMillis, accountId, note, store)
