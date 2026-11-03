package com.polino.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, AccountEntity::class],
    version = 2,
    exportSchema = false
)
internal abstract class PolinoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao

    companion object {
        /**
         * مهاجرت v1→v2: ستون حساب به جدول تراکنش‌ها اضافه می‌شود و جدول
         * حساب‌ها ساخته می‌شود؛ همه تراکنش‌های موجود به حساب «اصلی» می‌روند
         * و هیچ ردیفی حذف یا تغییر مقدار نمی‌شود.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions " +
                        "ADD COLUMN accountId TEXT NOT NULL DEFAULT 'main'"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS accounts " +
                        "(id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO accounts (id, name) VALUES ('main', 'اصلی')"
                )
            }
        }
    }
}
