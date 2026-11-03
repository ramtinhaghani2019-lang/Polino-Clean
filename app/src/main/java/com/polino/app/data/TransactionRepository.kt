package com.polino.app.data

import android.content.Context
import androidx.room.Room

internal class TransactionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext, PolinoDatabase::class.java, "polino_db"
    ).build()
    private val dao = database.transactionDao()
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun getAll(): List<MoneyTransaction> =
        dao.getAll().map { it.toDomain() }

    suspend fun upsert(transaction: MoneyTransaction) {
        dao.upsert(transaction.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /**
     * مهاجرت یک‌بارمصرف از JSON قدیمی SharedPreferences به Room.
     * JSON قدیمی دست نمی‌خورد و به‌عنوان پشتیبان باقی می‌ماند؛
     * پرچم مهاجرت از اجرای دوباره جلوگیری می‌کند.
     */
    suspend fun migrateIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        val parsed = parseLegacyTransactions(prefs.getString(KEY, "[]") ?: "[]")
        if (parsed.isNotEmpty()) dao.insertAll(parsed.map { it.toEntity() })
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    private companion object {
        const val PREFS = "polino_finance_v1"
        const val KEY = "transactions"
        const val KEY_MIGRATED = "room_migrated_v1"
    }
}
