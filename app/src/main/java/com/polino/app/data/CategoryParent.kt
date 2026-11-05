package com.polino.app.data

/**
 * قاعده‌های خالص تغییر نوع و والد دسته (مرحله ۱۰۶، ورق ویرایش پرگاری):
 * نوع دسته فقط از یک جفت پرچم ساخته می‌شود تا isIncome و isInvestment
 * هرگز از هم جدا نیفتند، و والد فقط از دسته‌های اصلیِ هم‌نوع انتخاب
 * می‌شود تا چرخه و سه‌طبقه‌شدن پیش نیاید. هیچ‌کدام به اندروید وابسته
 * نیستند و در تست واحد JVM اجرا می‌شوند.
 */

/** پرچم‌های (isIncome و isInvestment) متناظر با یک نوع دسته. */
internal fun typeFlags(type: CategoryType): Pair<Boolean, Boolean> = when (type) {
    CategoryType.EXPENSE -> false to false
    CategoryType.INCOME -> true to false
    CategoryType.INVESTMENT -> false to true
}

/**
 * همه نوادگان یک دسته (زیردسته‌ها و زیردسته‌های آن‌ها) با پیمایش
 * پیوندهای parentId؛ برای داده معتبر حداکثر یک سطح است، ولی پیمایش
 * گذرا چرخه‌های احتمالی داده خراب را هم پوشش می‌دهد.
 */
internal fun descendantNames(
    categories: List<CategoryEntity>,
    rootName: String
): Set<String> {
    val result = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    categories.filter { it.parentId == rootName }.mapTo(queue) { it.name }
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!result.add(current)) continue
        categories.filter { it.parentId == current }.mapTo(queue) { it.name }
    }
    return result
}

/**
 * نامزدهای معتبر والد برای دسته‌ای با نوع داده‌شده: فقط دسته‌های اصلی
 * هم‌نوع، به‌جز خود دسته و نوادگانش. دسته‌ای که زیردسته دارد اصلاً
 * نمی‌تواند والد بگیرد (وگرنه زیردسته‌هایش سه‌طبقه می‌شوند)، پس
 * نامزدهایش تهی است.
 */
internal fun parentCandidates(
    categories: List<CategoryEntity>,
    selfName: String,
    type: CategoryType
): List<CategoryEntity> {
    if (categories.any { it.parentId == selfName }) return emptyList()
    val descendants = descendantNames(categories, selfName)
    return categories.filter {
        it.parentId == null &&
            categoryType(it) == type &&
            it.name != selfName &&
            it.name !in descendants
    }
}

/**
 * آیا والد داده‌شده برای این دسته معتبر است؟ تهی (دسته اصلی شدن) همیشه
 * معتبر است؛ غیرتهی باید دسته اصلیِ موجودِ هم‌نوع باشد، نه خود دسته،
 * نه نواده‌اش، و دسته‌ای که زیردسته دارد نمی‌تواند والد بگیرد.
 */
internal fun isValidParent(
    categories: List<CategoryEntity>,
    selfName: String,
    type: CategoryType,
    parentName: String?
): Boolean {
    if (parentName == null) return true
    if (parentName == selfName) return false
    if (categories.any { it.parentId == selfName }) return false
    val parent = categories.firstOrNull { it.name == parentName } ?: return false
    if (parent.parentId != null) return false
    if (categoryType(parent) != type) return false
    if (parentName in descendantNames(categories, selfName)) return false
    return true
}
