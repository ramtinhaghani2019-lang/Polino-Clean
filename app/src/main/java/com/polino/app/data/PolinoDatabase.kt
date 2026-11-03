package com.polino.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
internal abstract class PolinoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
