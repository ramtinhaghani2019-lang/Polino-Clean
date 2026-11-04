package com.polino.app.data

/** سه نوع دسته؛ سرمایه‌گذاری بر درآمد/هزینه اولویت دارد. */
internal enum class CategoryType { EXPENSE, INCOME, INVESTMENT }

/** نوع دسته از روی ستون‌ها: سرمایه‌گذاری، وگرنه درآمد، وگرنه هزینه. */
internal fun categoryType(category: CategoryEntity): CategoryType = when {
    category.isInvestment -> CategoryType.INVESTMENT
    category.isIncome -> CategoryType.INCOME
    else -> CategoryType.EXPENSE
}

/** برچسب نمایشی نوع دسته، مثل «دسته اصلی · سرمایه‌گذاری». */
internal fun categoryTypeLabel(type: CategoryType): String = when (type) {
    CategoryType.EXPENSE -> "هزینه‌ای"
    CategoryType.INCOME -> "درآمدی"
    CategoryType.INVESTMENT -> "سرمایه‌گذاری"
}

/** دسته‌های سرمایه‌گذاری پیش‌فرض پرگار؛ کاربر می‌تواند هرکدام را حذف کند. */
internal val INVESTMENT_CATEGORIES = listOf(
    "طلا و جواهر",
    "سهام و بورس",
    "صندوق سرمایه‌گذاری",
    "ارز دیجیتال",
    "ارز",
    "ملک و املاک",
    "سپرده و اوراق",
    "سایر سرمایه‌گذاری"
)

/** دسته‌های پیش‌فرض؛ در نصب تازه یا مهاجرت، همین‌ها ساخته می‌شوند.
 * نام‌های هزینه/درآمد با دسته‌های پیش‌فرض پرگار هم‌تراز شده‌اند؛ دسته‌های
 * موجود کاربر هرگز با این فهرست بازنویسی نمی‌شوند (بذرها فقط وقتی جدول
 * خالی است درج می‌شوند). */
internal val DEFAULT_CATEGORIES = listOf(
    CategoryEntity("درمان و سلامت", false),
    CategoryEntity("تفریح و ورزش", false),
    CategoryEntity("خریدهای روزانه", false),
    CategoryEntity("تعهدات و اقساط", false),
    CategoryEntity("خوراک و پوشاک", false),
    CategoryEntity("حمل و نقل", false),
    CategoryEntity("قبوض و ارتباطات", false),
    CategoryEntity("خیریه", false),
    CategoryEntity("اقساط", false),
    CategoryEntity("حقوق و دستمزد", true),
    CategoryEntity("پاداش و کارانه", true),
    CategoryEntity("فروش و درآمد جانبی", true),
    CategoryEntity("سایر", false)
) + INVESTMENT_CATEGORIES.map { CategoryEntity(it, false, true) }

/** مقصد انتقال هنگام حذف دسته؛ برای هر دو نوع قابل استفاده است. */
internal const val FALLBACK_CATEGORY = "سایر"

/** «سایر» هرگز حذف نمی‌شود، چون مقصد انتقال حذف‌هاست. */
internal fun canDeleteCategory(name: String): Boolean = name != FALLBACK_CATEGORY

internal fun expenseCategoryNames(categories: List<CategoryEntity>): List<String> =
    categories.filter { categoryType(it) == CategoryType.EXPENSE }.map { it.name }

internal fun incomeCategoryNames(categories: List<CategoryEntity>): List<String> =
    categories.filter {
        categoryType(it) == CategoryType.INCOME || it.name == FALLBACK_CATEGORY
    }.map { it.name }

/** دسته‌های اصلی (بدون والد) از یک نوع، به ترتیب نام. */
internal fun mainCategories(
    categories: List<CategoryEntity>,
    type: CategoryType
): List<CategoryEntity> =
    categories.filter { it.parentId == null && categoryType(it) == type }

/** زیردسته‌های یک دسته اصلی، به ترتیب نام. */
internal fun subcategoriesOf(
    categories: List<CategoryEntity>,
    parentName: String
): List<CategoryEntity> =
    categories.filter { it.parentId == parentName }
