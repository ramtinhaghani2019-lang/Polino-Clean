package com.polino.app.data

import java.util.Locale

/** همه بخش‌های قابل پشتیبان‌گیری پولینو در یک فایل. */
internal data class BackupData(
    val transactions: List<MoneyTransaction>,
    val budgets: List<BudgetEntity>,
    val categories: List<CategoryEntity>,
    val accounts: List<AccountEntity>,
    val recurring: List<RecurringEntity>,
    val goals: List<GoalEntity>
)

/**
 * خروجی JSON تراکنش‌ها برای پشتیبان‌گیری قدیمی. فرمت دقیقاً همان فرمت
 * قدیمی است تا ورود آن با همان تجزیه‌کننده مهاجرت انجام شود؛
 * خروجی جاری برنامه fullBackupToJson است.
 */
internal fun transactionsToJson(transactions: List<MoneyTransaction>): String = buildString {
    append('[')
    transactions.forEachIndexed { index, t ->
        if (index > 0) append(',')
        append(
            "{\"id\":\"${escapeJsonString(t.id)}\"," +
                "\"title\":\"${escapeJsonString(t.title)}\"," +
                "\"amount\":${t.amount}," +
                "\"income\":${t.income}," +
                "\"category\":\"${escapeJsonString(t.category)}\"," +
                "\"dateMillis\":${t.dateMillis}," +
                "\"accountId\":\"${escapeJsonString(t.accountId)}\"}"
        )
    }
    append(']')
}

internal fun escapeJsonString(value: String): String = buildString {
    for (c in value) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (c < ' ') append("\\u%04x".format(Locale.US, c.code)) else append(c)
            }
        }
    }
}

/**
 * خروجی پشتیبان کامل نسخه ۲: آرایه‌ای با نشانگر نسخه در عنصر اول و
 * بقیه عناصر بر اساس "type". واردکننده قدیمی (parseLegacyTransactions)
 * نشانگر و عناصر غیرتراکنش را نادیده می‌گیرد و فقط تراکنش‌ها را می‌خواند،
 * پس فایل جدید با نسخه‌های قدیمی هم سازگار است.
 */
internal fun fullBackupToJson(data: BackupData): String = buildString {
    append("[{\"backupVersion\":2}")
    data.transactions.forEach { t ->
        append(
            ",{\"type\":\"transaction\"," +
                "\"id\":\"${escapeJsonString(t.id)}\"," +
                "\"title\":\"${escapeJsonString(t.title)}\"," +
                "\"amount\":${t.amount}," +
                "\"income\":${t.income}," +
                "\"category\":\"${escapeJsonString(t.category)}\"," +
                "\"dateMillis\":${t.dateMillis}," +
                "\"accountId\":\"${escapeJsonString(t.accountId)}\"," +
                "\"note\":\"${escapeJsonString(t.note)}\"," +
                "\"store\":\"${escapeJsonString(t.store)}\"}"
        )
    }
    data.budgets.forEach { b ->
        append(
            ",{\"type\":\"budget\"," +
                "\"yearMonth\":\"${escapeJsonString(b.yearMonth)}\"," +
                "\"category\":\"${escapeJsonString(b.category)}\"," +
                "\"limitAmount\":${b.limitAmount}}"
        )
    }
    data.categories.forEach { c ->
        append(
            ",{\"type\":\"category\"," +
                "\"name\":\"${escapeJsonString(c.name)}\"," +
                "\"isIncome\":${c.isIncome}}"
        )
    }
    data.accounts.forEach { a ->
        append(
            ",{\"type\":\"account\"," +
                "\"id\":\"${escapeJsonString(a.id)}\"," +
                "\"name\":\"${escapeJsonString(a.name)}\"}"
        )
    }
    data.recurring.forEach { r ->
        val lastApplied = r.lastAppliedYearMonth?.let { "\"${escapeJsonString(it)}\"" } ?: "null"
        append(
            ",{\"type\":\"recurring\"," +
                "\"id\":\"${escapeJsonString(r.id)}\"," +
                "\"title\":\"${escapeJsonString(r.title)}\"," +
                "\"amount\":${r.amount}," +
                "\"income\":${r.income}," +
                "\"category\":\"${escapeJsonString(r.category)}\"," +
                "\"accountId\":\"${escapeJsonString(r.accountId)}\"," +
                "\"dayOfMonth\":${r.dayOfMonth}," +
                "\"startYearMonth\":\"${escapeJsonString(r.startYearMonth)}\"," +
                "\"lastAppliedYearMonth\":$lastApplied,\"active\":${r.active}}"
        )
    }
    data.goals.forEach { g ->
        append(
            ",{\"type\":\"goal\"," +
                "\"id\":\"${escapeJsonString(g.id)}\"," +
                "\"name\":\"${escapeJsonString(g.name)}\"," +
                "\"target\":${g.target}," +
                "\"accountId\":\"${escapeJsonString(g.accountId)}\"}"
        )
    }
    append(']')
}
