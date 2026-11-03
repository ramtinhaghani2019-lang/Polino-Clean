package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * هدف پس‌انداز؛ مثلاً «سفر» با سقف ۵ میلیون تومان روی یک حساب.
 * پیشرفت از روی موجودی همان حساب (درآمدها منهای هزینه‌ها) حساب می‌شود.
 */
@Entity(tableName = "goals")
internal data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val target: Long,
    val accountId: String
)
