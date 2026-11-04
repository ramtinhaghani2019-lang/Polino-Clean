package com.polino.app.dashboard

import com.polino.app.data.BudgetEntity
import com.polino.app.data.MoneyTransaction
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

internal data class MonthTotals(val income: Long, val expense: Long)

/** تراکنش‌های یک ماه شمسی؛ ورودی‌های بدون تاریخ (قدیمی) کنار گذاشته می‌شوند. */
internal fun transactionsInMonth(
    transactions: List<MoneyTransaction>,
    yearMonth: String
): List<MoneyTransaction> =
    transactions.filter {
        it.dateMillis != 0L && JalaliDate.jalaliYearMonthOf(it.dateMillis) == yearMonth
    }

internal fun monthTotals(monthTransactions: List<MoneyTransaction>): MonthTotals {
    val income = monthTransactions.filter { it.income }.sumOf { it.amount }
    val expense = monthTransactions.filterNot { it.income }.sumOf { it.amount }
    return MonthTotals(income, expense)
}

/** هزینه هر دسته در ماه، از بیشترین به کمترین. */
internal fun spentByCategory(monthTransactions: List<MoneyTransaction>): List<Pair<String, Long>> =
    monthTransactions.filterNot { it.income }
        .groupBy { it.category }
        .map { (category, items) -> category to items.sumOf { it.amount } }
        .sortedByDescending { it.second }

/** دسته‌ای که هزینه ماه جاری‌اش از بودجه‌اش گذشته است. */
internal data class OverspentCategory(val category: String, val spent: Long, val limit: Long)

/** دسته‌ای که هزینه ماه جاری‌اش به ۸۰٪ سقف بودجه‌اش رسیده ولی هنوز رد نشده است. */
internal data class NearingCategory(val category: String, val spent: Long, val limit: Long)

/**
 * دسته‌های نزدیک به سقف بودجه ماه جاری (از ۸۰٪ سقف تا زیر سقف)، از
 * نزدیک‌ترین به سقف به دورترین. دسته ردشده اینجا نمی‌آید؛ آن در
 * overspentCategories است.
 */
internal fun nearingCategories(
    monthTransactions: List<MoneyTransaction>,
    budgets: List<BudgetEntity>
): List<NearingCategory> {
    val spent = spentByCategory(monthTransactions).toMap()
    return budgets.mapNotNull { budget ->
        val spentAmount = spent[budget.category] ?: 0L
        if (spentAmount < budget.limitAmount && spentAmount * 5 >= budget.limitAmount * 4) {
            NearingCategory(budget.category, spentAmount, budget.limitAmount)
        } else {
            null
        }
    }.sortedByDescending { it.spent.toDouble() / it.limit }
}

/**
 * دسته‌های ردشده از بودجه ماه جاری، از بیشترین اضافه‌خرج به کمترین.
 * درآمد نادیده گرفته می‌شود و دسته‌ای که دقیقاً به سقف رسیده ردشده
 * حساب نمی‌شود؛ بودجه دسته بدون هزینه هم هشدار نمی‌سازد.
 */
internal fun overspentCategories(
    monthTransactions: List<MoneyTransaction>,
    budgets: List<BudgetEntity>
): List<OverspentCategory> {
    val spent = spentByCategory(monthTransactions).toMap()
    return budgets.mapNotNull { budget ->
        val spentAmount = spent[budget.category] ?: 0L
        if (spentAmount > budget.limitAmount) {
            OverspentCategory(budget.category, spentAmount, budget.limitAmount)
        } else {
            null
        }
    }.sortedByDescending { it.spent - it.limit }
}

internal data class MonthTrend(val yearMonth: String, val income: Long, val expense: Long)

/** مجموع درآمد و هزینه هر ماه از فهرست داده‌شده، به همان ترتیب ورودی. */
internal fun monthlyTrend(
    transactions: List<MoneyTransaction>,
    yearMonths: List<String>
): List<MonthTrend> = yearMonths.map { yearMonth ->
    val totals = monthTotals(transactionsInMonth(transactions, yearMonth))
    MonthTrend(yearMonth, totals.income, totals.expense)
}

internal enum class ReportPeriod { DAILY, WEEKLY, MONTHLY, YEARLY }

/** سه‌تایی شمسی یک لحظه: (سال، ماه ۱ تا ۱۲، روز). */
internal fun jalaliTripleOf(millis: Long): Triple<Int, Int, Int> {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return JalaliDate.gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
}

/** آیا دو لحظه در یک روز شمسی هستند؟ */
internal fun isSameJalaliDay(aMillis: Long, bMillis: Long): Boolean =
    jalaliTripleOf(aMillis) == jalaliTripleOf(bMillis)

/** سال شمسی یک لحظه. */
internal fun jalaliYearOf(millis: Long): Int = jalaliTripleOf(millis).first

/**
 * شروع هفته شمسی (شنبه) شامل این لحظه، به‌صورت میلی‌ثانیه ابتدای روز.
 * شنبه در تقویم ISO مقدار ۶ دارد؛ فاصله تا شنبه با (value + 1) % ۷ به دست می‌آید.
 */
internal fun jalaliWeekStartMillis(millis: Long): Long {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val offset = (date.dayOfWeek.value + 1) % 7
    return date.minusDays(offset.toLong()).atStartOfDay(ZoneId.systemDefault())
        .toInstant().toEpochMilli()
}

/** تراکنش‌های یک بازه گزارشی نسبت به لحظه مرجع؛ ماه از مسیر جداگانه می‌آید. */
internal fun transactionsInPeriod(
    transactions: List<MoneyTransaction>,
    period: ReportPeriod,
    nowMillis: Long,
    yearMonth: String
): List<MoneyTransaction> = when (period) {
    ReportPeriod.DAILY -> transactions.filter {
        it.dateMillis != 0L && isSameJalaliDay(it.dateMillis, nowMillis)
    }
    ReportPeriod.WEEKLY -> {
        val start = jalaliWeekStartMillis(nowMillis)
        val end = start + 7L * 24 * 60 * 60 * 1000
        transactions.filter { it.dateMillis in start until end }
    }
    ReportPeriod.YEARLY -> {
        val year = jalaliYearOf(nowMillis)
        transactions.filter { it.dateMillis != 0L && jalaliYearOf(it.dateMillis) == year }
    }
    ReportPeriod.MONTHLY -> transactionsInMonth(transactions, yearMonth)
}

internal data class DayTotal(val day: Int, val income: Long, val expense: Long)

/** جمع درآمد و هزینه هر روز ماه شمسی؛ روزهای بدون تراکنش صفر می‌گیرند. */
internal fun dailyTotals(monthTransactions: List<MoneyTransaction>, yearMonth: String): List<DayTotal> {
    val parts = yearMonth.split("-")
    val jy = parts.getOrNull(0)?.toIntOrNull() ?: return emptyList()
    val jm = parts.getOrNull(1)?.toIntOrNull() ?: return emptyList()
    val days = JalaliDate.daysInJalaliMonth(jy, jm)
    val incomeByDay = LongArray(days + 1)
    val expenseByDay = LongArray(days + 1)
    monthTransactions.forEach { tx ->
        if (tx.dateMillis == 0L) return@forEach
        val date = Instant.ofEpochMilli(tx.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val (ty, tm, td) = JalaliDate.gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
        if (ty == jy && tm == jm && td in 1..days) {
            if (tx.income) incomeByDay[td] += tx.amount else expenseByDay[td] += tx.amount
        }
    }
    return (1..days).map { DayTotal(it, incomeByDay[it], expenseByDay[it]) }
}

internal data class PeriodBucket(val label: String, val income: Long, val expense: Long)

/** عدد صحیح با ارقام فارسی؛ برای برچسب محور نمودار (مقادیر کوچک، بدون جداکننده). */
internal fun persianNumber(value: Int): String =
    NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(value)

/** ساعت روز یک لحظه در منطقه زمانی دستگاه. */
internal fun hourOfDay(millis: Long): Int =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).hour

/**
 * تراکنش‌های یک روز شمسی در ۶ بازه ۴ساعته: ۰ تا ۴، ۴ تا ۸، …، ۲۰ تا ۲۴.
 * بازه‌های بدون تراکنش صفر می‌گیرند و تراکنش روز دیگر نادیده گرفته می‌شود.
 */
internal fun bucketByHourOfDay(
    transactions: List<MoneyTransaction>,
    dayMillis: Long
): List<PeriodBucket> {
    val income = LongArray(6)
    val expense = LongArray(6)
    transactions.forEach { tx ->
        if (tx.dateMillis == 0L || !isSameJalaliDay(tx.dateMillis, dayMillis)) return@forEach
        val bucket = (hourOfDay(tx.dateMillis) / 4).coerceIn(0, 5)
        if (tx.income) income[bucket] += tx.amount else expense[bucket] += tx.amount
    }
    return (0 until 6).map { bucket ->
        val start = bucket * 4
        PeriodBucket(
            "${persianNumber(start)} تا ${persianNumber(start + 4)}",
            income[bucket],
            expense[bucket]
        )
    }
}

internal val JALALI_WEEKDAYS = listOf(
    "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
)

/**
 * تراکنش‌های یک هفته شمسی به ترتیب شنبه تا جمعه؛ هفته از شنبه شروع می‌شود
 * و تراکنش بیرون از هفته نادیده گرفته می‌شود.
 */
internal fun bucketByWeekday(
    transactions: List<MoneyTransaction>,
    weekStartMillis: Long
): List<PeriodBucket> {
    val income = LongArray(7)
    val expense = LongArray(7)
    val weekEnd = weekStartMillis + 7L * 24 * 60 * 60 * 1000
    transactions.forEach { tx ->
        if (tx.dateMillis !in weekStartMillis until weekEnd) return@forEach
        val date = Instant.ofEpochMilli(tx.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val index = (date.dayOfWeek.value + 1) % 7
        if (tx.income) income[index] += tx.amount else expense[index] += tx.amount
    }
    return JALALI_WEEKDAYS.mapIndexed { index, name ->
        PeriodBucket(name, income[index], expense[index])
    }
}

/** تراکنش‌های یک ماه شمسی به تفکیک روز ماه؛ روزهای بدون تراکنش صفر می‌گیرند. */
internal fun bucketByDayOfMonth(
    transactions: List<MoneyTransaction>,
    yearMonth: String
): List<PeriodBucket> =
    dailyTotals(transactions, yearMonth).map { day ->
        PeriodBucket(persianNumber(day.day), day.income, day.expense)
    }

/** تراکنش‌های یک سال شمسی به تفکیک ماه؛ ماه‌های بدون تراکنش صفر می‌گیرند. */
internal fun bucketByJalaliMonth(
    transactions: List<MoneyTransaction>,
    jalaliYear: Int
): List<PeriodBucket> {
    val income = LongArray(13)
    val expense = LongArray(13)
    transactions.forEach { tx ->
        if (tx.dateMillis == 0L || jalaliYearOf(tx.dateMillis) != jalaliYear) return@forEach
        val month = jalaliTripleOf(tx.dateMillis).second
        if (month in 1..12) {
            if (tx.income) income[month] += tx.amount else expense[month] += tx.amount
        }
    }
    return (1..12).map { month ->
        val name = JalaliDate.jalaliMonthDisplay(
            "%04d-%02d".format(Locale.US, jalaliYear, month)
        ).substringBefore(" ")
        PeriodBucket(name, income[month], expense[month])
    }
}

/** جابه‌جایی یک لحظه به اندازه سال شمسی، با سقف روز ماه مقصد (برای اسفند کبیسه). */
internal fun shiftJalaliYear(millis: Long, deltaYears: Int): Long {
    val (jy, jm, jd) = jalaliTripleOf(millis)
    val targetYear = jy + deltaYears
    val targetDay = jd.coerceAtMost(JalaliDate.daysInJalaliMonth(targetYear, jm))
    return JalaliDate.jalaliToMillis(targetYear, jm, targetDay)
}
