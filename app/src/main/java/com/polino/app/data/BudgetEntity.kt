package com.polino.app.data

import androidx.room.Entity

/**
 * بودجه ماهانه یک دسته. کلید مرکب (ماه شمسی، دسته) است؛
 * ماه با قالب «۱۴۰۵-۰۸» ذخیره می‌شود.
 */
@Entity(tableName = "budgets", primaryKeys = ["yearMonth", "category"])
internal data class BudgetEntity(
    val yearMonth: String,
    val category: String,
    val limitAmount: Long
)
