package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** یک حساب مالی (مثل «اصلی»، «پس‌انداز»)؛ شناسه «main» همان حساب پیش‌فرض است. */
@Entity(tableName = "accounts")
internal data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** نام بانک یا مؤسسه؛ خالی یعنی کاربر بانکی ثبت نکرده است. */
    val bankName: String? = null
)
