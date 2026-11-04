package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * برنامه مالی: یک کار برنامه‌ریزی‌شده با عنوان، یادداشت اختیاری، موعد
 * اختیاری و نشان انجام‌شدن. برخلاف هدف پس‌انداز، مبلغ ندارد؛ برخلاف
 * کار (TaskEntity)، یادداشت و موعد اختیاری دارد.
 */
@Entity(tableName = "plans")
internal data class PlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    /** تاریخ موعد؛ null یعنی بدون موعد. */
    val dueMillis: Long? = null,
    val done: Boolean = false
)
