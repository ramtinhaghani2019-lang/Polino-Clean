package com.polino.app.data

/** دسته‌های پیش‌فرض؛ در نصب تازه یا مهاجرت، همین‌ها ساخته می‌شوند. */
internal val DEFAULT_CATEGORIES = listOf(
    CategoryEntity("خوراک", false),
    CategoryEntity("مسکن", false),
    CategoryEntity("حمل‌ونقل", false),
    CategoryEntity("سلامت", false),
    CategoryEntity("آموزش", false),
    CategoryEntity("تفریح", false),
    CategoryEntity("پوشاک", false),
    CategoryEntity("حقوق", true),
    CategoryEntity("فروش", true),
    CategoryEntity("هدیه", true),
    CategoryEntity("سایر", false)
)

/** مقصد انتقال هنگام حذف دسته؛ برای هر دو نوع قابل استفاده است. */
internal const val FALLBACK_CATEGORY = "سایر"

/** «سایر» هرگز حذف نمی‌شود، چون مقصد انتقال حذف‌هاست. */
internal fun canDeleteCategory(name: String): Boolean = name != FALLBACK_CATEGORY

internal fun expenseCategoryNames(categories: List<CategoryEntity>): List<String> =
    categories.filter { !it.isIncome || it.name == FALLBACK_CATEGORY }.map { it.name }

internal fun incomeCategoryNames(categories: List<CategoryEntity>): List<String> =
    categories.filter { it.isIncome || it.name == FALLBACK_CATEGORY }.map { it.name }
