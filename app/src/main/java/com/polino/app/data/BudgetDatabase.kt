package com.polino.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * دیتابیس جداگانه بودجه‌ها. جدا از دیتابیس تراکنش‌ها نگه داشته شده تا
 * افزودن بودجه هیچ مهاجرتی روی جدول تراکنش‌ها لازم نداشته باشد و
 * داده موجود هیچ ریسکی نبیند.
 */
@Database(entities = [BudgetEntity::class], version = 1, exportSchema = false)
internal abstract class BudgetDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
}
