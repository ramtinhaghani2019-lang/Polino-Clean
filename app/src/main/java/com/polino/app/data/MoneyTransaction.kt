package com.polino.app.data

/** مدل دامنه تراکنش؛ همان مدلی که داشبورد نمایش می‌دهد. */
internal data class MoneyTransaction(
    val id: String,
    val title: String,
    val amount: Long,
    val income: Boolean,
    val category: String,
    val dateMillis: Long,
    /** حساب مالک تراکنش؛ پیش‌فرض «اصلی» است تا تراکنش‌های قدیمی همین‌جا بنشینند. */
    val accountId: String = "main"
)
