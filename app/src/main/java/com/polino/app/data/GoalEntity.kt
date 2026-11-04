package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * هدف پس‌انداز؛ مثلاً «سفر» با سقف ۵ میلیون تومان.
 * پیشرفت از روی مبلغ پس‌اندازشده صریح حساب می‌شود، نه موجودی حساب.
 * حسابِ قدیمی فقط برای سازگاری پشتیبان نگه داشته می‌شود و در فرم
 * جدید پرسیده نمی‌شود؛ هدف‌های قدیمی پس‌انداز صفر، بدون موعد،
 * اولویت متوسط و بدون دسته و یادداشت می‌گیرند.
 */
@Entity(tableName = "goals")
internal data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val target: Long,
    val accountId: String,
    /** مبلغ پس‌اندازشده تا کنون. */
    val savedAmount: Long = 0,
    /** تاریخ موعد دستیابی؛ null یعنی بدون موعد. */
    val dueMillis: Long? = null,
    /** اولویت: کم، متوسط یا زیاد. */
    val priority: String = "متوسط",
    /** دسته‌بندی هدف (پس‌انداز، سرمایه‌گذاری، …)؛ خالی یعنی بی‌دسته. */
    val category: String = "",
    val note: String = ""
)
