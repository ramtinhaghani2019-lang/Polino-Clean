package com.polino.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        RecurringEntity::class,
        GoalEntity::class,
        InstallmentEntity::class,
        DebtEntity::class,
        TaskEntity::class,
        EventEntity::class,
        AssetEntity::class,
        SmsPatternEntity::class,
        ChequeReminderEntity::class,
        AccountSmsSenderEntity::class,
        PlanEntity::class
    ],
    version = 21,
    exportSchema = false
)
internal abstract class PolinoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringDao(): RecurringDao
    abstract fun goalDao(): GoalDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun debtDao(): DebtDao
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun assetDao(): AssetDao
    abstract fun smsPatternDao(): SmsPatternDao
    abstract fun chequeReminderDao(): ChequeReminderDao
    abstract fun accountSmsSenderDao(): AccountSmsSenderDao
    abstract fun planDao(): PlanDao

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
        /**
         * مهاجرت v3→v4: جدول تراکنش‌های تکرارشونده ساخته می‌شود؛
         * تراکنش‌ها، حساب‌ها و دسته‌های موجود دست نمی‌خورند.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recurring " +
                        "(id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, " +
                        "amount INTEGER NOT NULL, income INTEGER NOT NULL, " +
                        "category TEXT NOT NULL, accountId TEXT NOT NULL, " +
                        "dayOfMonth INTEGER NOT NULL, startYearMonth TEXT NOT NULL, " +
                        "lastAppliedYearMonth TEXT)"
                )
            }
        }
        /**
         * مهاجرت v4→v5: جدول اهداف پس‌انداز ساخته می‌شود؛
         * بقیه جدول‌ها دست نمی‌خورند.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS goals " +
                        "(id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, " +
                        "target INTEGER NOT NULL, accountId TEXT NOT NULL)"
                )
            }
        }
        /**
         * مهاجرت v5→v6: ستون یادداشت به جدول تراکنش‌ها اضافه می‌شود؛
         * تراکنش‌های موجود یادداشت خالی می‌گیرند و هیچ ردیفی تغییر نمی‌کند.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN note TEXT NOT NULL DEFAULT ''"
                )
            }
        }
        /**
         * مهاجرت v6→v7: ستون فعال بودن به جدول قاعده‌های تکرارشونده اضافه
         * می‌شود؛ همه قاعده‌های موجود فعال می‌مانند و هیچ ردیفی تغییر نمی‌کند.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recurring ADD COLUMN active INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
        /**
         * مهاجرت v7→v8: جدول اقساط ساخته می‌شود؛
         * بقیه جدول‌ها دست نمی‌خورند.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS installments " +
                        "(id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, " +
                        "totalAmount INTEGER NOT NULL, monthCount INTEGER NOT NULL, " +
                        "paidCount INTEGER NOT NULL, accountId TEXT NOT NULL)"
                )
            }
        }
        /**
         * مهاجرت v8→v9: جدول طلب و بدهی ساخته می‌شود؛
         * بقیه جدول‌ها دست نمی‌خورند.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS debts " +
                        "(id TEXT NOT NULL PRIMARY KEY, personName TEXT NOT NULL, " +
                        "amount INTEGER NOT NULL, isReceivable INTEGER NOT NULL, " +
                        "settled INTEGER NOT NULL, accountId TEXT NOT NULL)"
                )
            }
        }
        /**
         * مهاجرت v9→v10: جدول کارها و یادآورها ساخته می‌شود؛
         * بقیه جدول‌ها دست نمی‌خورند.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tasks " +
                        "(id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, " +
                        "dueMillis INTEGER NOT NULL, done INTEGER NOT NULL)"
                )
            }
        }
        /**
         * مهاجرت v10→v11: جدول رویدادهای تقویم ساخته می‌شود؛
         * بقیه جدول‌ها دست نمی‌خورند.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS events " +
                        "(id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, " +
                        "dateMillis INTEGER NOT NULL, note TEXT NOT NULL)"
                )
            }
        }
        /**
         * مهاجرت v11→v12: جدول دارایی‌های سرمایه‌گذاری ساخته می‌شود؛
         * بقیه جدول‌ها دست نمی‌خورند.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS assets " +
                        "(id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, " +
                        "type TEXT NOT NULL, unit TEXT NOT NULL, " +
                        "quantity REAL NOT NULL, buyPrice REAL NOT NULL, " +
                        "currentPrice REAL, accountId TEXT NOT NULL, " +
                        "note TEXT NOT NULL)"
                )
            }
        }
        /**
         * مهاجرت v12→v13: جدول الگوهای پیامک ساخته می‌شود؛
         * بقیه جدول‌ها دست نمی‌خورند.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sms_patterns " +
                        "(id TEXT NOT NULL PRIMARY KEY, sender TEXT NOT NULL, " +
                        "keywords TEXT NOT NULL, isFinancial INTEGER NOT NULL, " +
                        "isIncome INTEGER NOT NULL)"
                )
            }
        }
        /**
         * مهاجرت v13→v14: ستون فروشگاه/مبدأ به جدول تراکنش‌ها اضافه
         * می‌شود؛ تراکنش‌های موجود فروشگاه خالی می‌گیرند و هیچ ردیفی
         * تغییر نمی‌کند.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN store TEXT NOT NULL DEFAULT ''"
                )
            }
        }
        /**
         * مهاجرت v14→v15: دو ستون نوع سرمایه‌گذاری و والد به جدول
         * دسته‌ها اضافه می‌شود؛ دسته‌های موجود دقیقاً همان نوع قبلی
         * (هزینه/درآمد) را نگه می‌دارند و دسته اصلی می‌مانند، و
         * دسته‌های سرمایه‌گذاری پیش‌فرض پرگار یک بار اضافه می‌شوند.
         * هیچ ردیف موجودی تغییر نمی‌کند.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE categories " +
                        "ADD COLUMN isInvestment INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("ALTER TABLE categories ADD COLUMN parentId TEXT")
                db.execSQL(
                    "INSERT OR IGNORE INTO categories " +
                        "(name, isIncome, isInvestment) VALUES ('طلا و جواهر', 0, 1)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO categories " +
                        "(name, isIncome, isInvestment) VALUES ('سهام و بورس', 0, 1)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO categories " +
                        "(name, isIncome, isInvestment) VALUES ('صندوق سرمایه‌گذاری', 0, 1)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO categories " +
                        "(name, isIncome, isInvestment) VALUES ('ارز دیجیتال', 0, 1)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO categories " +
                        "(name, isIncome, isInvestment) VALUES ('ارز', 0, 1)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO categories " +
                        "(name, isIncome, isInvestment) VALUES ('ملک و املاک', 0, 1)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO categories " +
                        "(name, isIncome, isInvestment) VALUES ('سپرده و اوراق', 0, 1)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO categories " +
                        "(name, isIncome, isInvestment) VALUES ('سایر سرمایه‌گذاری', 0, 1)"
                )
            }
        }
        /**
         * مهاجرت v15→v16: جدول یادآورهای چک و قبض ساخته می‌شود؛
         * بقیه جدول‌ها دست نمی‌خورند.
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cheque_reminders " +
                        "(id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, " +
                        "amount INTEGER NOT NULL, dueMillis INTEGER NOT NULL, " +
                        "settled INTEGER NOT NULL, note TEXT NOT NULL)"
                )
            }
        }
        /**
         * مهاجرت v16→v17: ستون سررسید به جدول طلب و بدهی اضافه
         * می‌شود؛ بدهی‌های موجود سررسید خالی (بدون سررسید) می‌گیرند
         * و هیچ تاریخی ساخته یا حدس زده نمی‌شود.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN dueMillis INTEGER")
            }
        }
        /**
         * مهاجرت v17→v18: ستون نام بانک به جدول حساب‌ها و جدول اتصال
         * سرشماره‌های پیامک به حساب‌ها اضافه می‌شود؛ حساب‌های موجود نام
         * بانک خالی می‌گیرند و هیچ اتصالی ساخته یا حدس زده نمی‌شود.
         */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN bankName TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS account_sms_senders " +
                        "(accountId TEXT NOT NULL, sender TEXT NOT NULL, " +
                        "PRIMARY KEY(accountId, sender))"
                )
            }
        }
        /**
         * مهاجرت v18→v19: پنج ستون مدل هدف پرگار به جدول هدف‌ها اضافه
         * می‌شود؛ هدف‌های موجود پس‌انداز صفر، بدون موعد، اولویت متوسط و
         * بدون دسته و یادداشت می‌گیرند و هیچ مقداری ساخته یا حدس زده
         * نمی‌شود.
         */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN savedAmount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN dueMillis INTEGER")
                db.execSQL("ALTER TABLE goals ADD COLUMN priority TEXT NOT NULL DEFAULT 'متوسط'")
                db.execSQL("ALTER TABLE goals ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE goals ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }
        /**
         * مهاجرت v19→v20: سه ستون مدل قسط پرگار به جدول اقساط اضافه
         * می‌شود؛ مبلغ هر قسط موجود از تقسیم مبلغ کل بر تعداد ماه به دست
         * می‌آید (همان مقدار مشتق‌شده قبلی)، شمارنده پرداخت موجود دست
         * نمی‌خورد، و بانک و تاریخ شروع موجود خالی می‌مانند؛ هیچ مقداری
         * ساخته یا حدس زده نمی‌شود.
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE installments ADD COLUMN bankName TEXT")
                db.execSQL(
                    "ALTER TABLE installments " +
                        "ADD COLUMN installmentAmount INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("ALTER TABLE installments ADD COLUMN startMillis INTEGER")
                db.execSQL(
                    "UPDATE installments SET installmentAmount = totalAmount / monthCount " +
                        "WHERE monthCount > 0"
                )
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS plans (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, " +
                        "note TEXT NOT NULL DEFAULT '', " +
                        "dueMillis INTEGER, " +
                        "done INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }
    }
}
