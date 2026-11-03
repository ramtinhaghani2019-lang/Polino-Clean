package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction

internal enum class RadarKind { UP, DOWN, NEW }

internal data class RadarInsight(val text: String, val kind: RadarKind)

/**
 * رادار مالی هوشمند (نسخه اول): هزینه‌های ماه جاری را با ماه قبل مقایسه می‌کند.
 * - دسته‌ای که تازه خرج داشته: NEW
 * - دسته‌ای که خرجش بیشتر شده: UP (به ترتیب بیشترین افزایش)
 * - اگر مجموع هزینه‌ها کمتر شده: یک DOWN کلی
 * فقط ۳ بینش دسته‌ای اول برگردانده می‌شود تا کارت شلوغ نشود،
 * به‌علاوه یک خط کلی اگر مجموع هزینه‌ها کمتر شده باشد.
 */
internal fun spendingInsights(
    currentMonth: List<MoneyTransaction>,
    previousMonth: List<MoneyTransaction>
): List<RadarInsight> {
    val current = spentByCategory(currentMonth).toMap()
    val previous = spentByCategory(previousMonth).toMap()
    if (current.isEmpty()) return emptyList()
    val insights = mutableListOf<RadarInsight>()
    current.entries
        .mapNotNull { (category, amount) ->
            val prev = previous[category] ?: 0L
            when {
                prev == 0L -> RadarInsight(
                    "هزینه «$category» تازه به خرج‌های این ماه اضافه شده (${money(amount)})",
                    RadarKind.NEW
                ) to null
                amount > prev -> RadarInsight(
                    "هزینه «$category» نسبت به ماه قبل ${money(amount - prev)} بیشتر شده",
                    RadarKind.UP
                ) to (amount - prev)
                else -> null
            }
        }
        .sortedWith(
            compareByDescending<Pair<RadarInsight, Long?>> { it.second ?: Long.MAX_VALUE }
                .thenBy { it.first.text }
        )
        .map { it.first }
        .take(3)
        .let { insights.addAll(it) }
    val currentTotal = current.values.sum()
    val previousTotal = previous.values.sum()
    if (currentTotal < previousTotal) {
        insights.add(
            RadarInsight(
                "مجموع هزینه‌ها نسبت به ماه قبل ${money(previousTotal - currentTotal)} کمتر شده",
                RadarKind.DOWN
            )
        )
    }
    return insights
}
