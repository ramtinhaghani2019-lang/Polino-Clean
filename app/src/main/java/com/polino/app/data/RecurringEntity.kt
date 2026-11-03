package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * تراکنش تکرارشونده ماهانه؛ مثلاً حقوق یا اجاره.
 * هر ماه، روز مشخص‌شده (dayOfMonth) یک تراکنش تازه می‌سازد.
 * lastAppliedYearMonth ماهی است که تا آن ماه اعمال شده؛ null یعنی هنوز هیچ‌وقت اعمال نشده.
 */
@Entity(tableName = "recurring")
internal data class RecurringEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Long,
    val income: Boolean,
    val category: String,
    val accountId: String,
    val dayOfMonth: Int,
    val startYearMonth: String,
    val lastAppliedYearMonth: String?
)
