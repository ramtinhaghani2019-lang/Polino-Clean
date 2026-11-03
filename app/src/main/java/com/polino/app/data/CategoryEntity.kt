package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** یک دسته تراکنش؛ «سایر» در هر دو نوع استفاده می‌شود و حذف نمی‌شود. */
@Entity(tableName = "categories")
internal data class CategoryEntity(
    @PrimaryKey val name: String,
    val isIncome: Boolean
)
