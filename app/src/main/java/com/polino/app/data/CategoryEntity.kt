package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * یک دسته تراکنش؛ «سایر» در هر دو نوع استفاده می‌شود و حذف نمی‌شود.
 * نوع دسته از روی ستون‌ها خوانده می‌شود: سرمایه‌گذاری اگر isInvestment
 * باشد، وگرنه درآمد اگر isIncome باشد وگرنه هزینه. parentId تهی یعنی
 * دسته اصلی، وگرنه نام دسته اصلیِ زیردسته است.
 */
@Entity(tableName = "categories")
internal data class CategoryEntity(
    @PrimaryKey val name: String,
    val isIncome: Boolean,
    val isInvestment: Boolean = false,
    val parentId: String? = null
)
