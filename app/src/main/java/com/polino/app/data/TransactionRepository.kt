package com.polino.app.data

import android.content.Context
import androidx.room.Room
import org.json.JSONArray
import java.util.UUID

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

/**
 * تجزیه JSON قدیمی؛ تابع خالص و قابل آزمون با تست واحد.
 * ورودی‌های قدیمیِ بدون id/category/date مقدار پیش‌فرض می‌گیرند،
 * دقیقاً با همان معنایی که نسخه SharedPreferences نشان می‌داد.
 */
internal fun parseLegacyTransactions(raw: String): List<MoneyTransaction> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        MoneyTransaction(
            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = item.getString("title"),
            amount = item.getLong("amount"),
            income = item.getBoolean("income"),
            category = item.optString("category").ifBlank { "سایر" },
            dateMillis = item.optLong("dateMillis", 0L)
        )
    }
}.getOrDefault(emptyList())
