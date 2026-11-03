package com.polino.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, AccountEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
internal abstract class PolinoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao

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

        /**
         * مهاجرت v2→v3: جدول دسته‌ها ساخته می‌شود و دسته‌های پیش‌فرض
         * پر می‌شوند؛ تراکنش‌ها و حساب‌های موجود دست نمی‌خورند.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories " +
                        "(name TEXT NOT NULL PRIMARY KEY, isIncome INTEGER NOT NULL)"
                )
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('خوراک', 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('مسکن', 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('حمل‌ونقل', 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('سلامت', 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('آموزش', 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('تفریح', 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('پوشاک', 0)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('حقوق', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('فروش', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('هدیه', 1)")
                db.execSQL("INSERT OR IGNORE INTO categories (name, isIncome) VALUES ('سایر', 0)")
            }
        }
    }
}
