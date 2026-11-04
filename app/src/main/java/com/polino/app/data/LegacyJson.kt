package com.polino.app.data

import java.util.UUID

/**
 * تجزیه JSON قدیمی تراکنش‌ها بدون وابستگی خارجی.
 *
 * چرا دستی؟ org.json اندروید در تست واحد JVM فقط یک استاب است و فراخوانی‌اش
 * استثنا می‌دهد؛ پس تابع تجزیه باید بدون هیچ کلاس اندرویدی کار کند تا هم در
 * اپ و هم در تست، دقیقاً یک پیاده‌سازی اجرا شود. ورودی فقط همان فرمتی است که
 * نسخه قدیمی با JSONObject می‌ساخت: آرایه‌ای از آبجکت‌های تخت با فیلدهای
 * مشخص. ترتیب فیلدها مهم نیست؛ فیلدهای غایب مقدار پیش‌فرض می‌گیرند؛ رکوردی
 * که فیلد اصلی‌اش (عنوان، مبلغ، نوع) خراب باشد حذف می‌شود و بقیه حفظ می‌شوند.
 */
internal fun parseLegacyTransactions(raw: String): List<MoneyTransaction> {
    return try {
        LegacyJsonParser(raw).parseArray()
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * تجزیه پشتیبان کامل نسخه ۲: آرایه‌ای که عنصر اولش نشانگر
 * {"backupVersion":2} است و بقیه عناصرش بر اساس "type" به بخش‌های
 * مختلف (تراکنش، بودجه، دسته، حساب، تکرارشونده، هدف) تقسیم می‌شوند.
 * آرایه قدیمی (بدون نشانگر) یا ورودی خراب null برمی‌گرداند تا مسیر
 * قدیمی وارد شود.
 */
internal fun parseBackup(raw: String): BackupData? {
    return try {
        LegacyJsonParser(raw).parseBackupArray()
    } catch (_: Exception) {
        null
    }
}

private class LegacyJsonParser(private val text: String) {
    private var i = 0
    private val n = text.length

    fun parseArray(): List<MoneyTransaction> {
        skipWs()
        if (consume() != '[') throw IllegalArgumentException("not an array")
        val out = ArrayList<MoneyTransaction>()
        skipWs()
        if (peek() == ']') {
            i++
            return out
        }
        while (true) {
            val item = parseObject()
            if (item != null) out.add(item)
            skipWs()
            when (consume()) {
                ',' -> continue
                ']' -> return out
                else -> throw IllegalArgumentException("expected , or ]")
            }
        }
    }

    fun parseBackupArray(): BackupData? {
        skipWs()
        if (consume() != '[') return null
        val transactions = ArrayList<MoneyTransaction>()
        val budgets = ArrayList<BudgetEntity>()
        val categories = ArrayList<CategoryEntity>()
        val accounts = ArrayList<AccountEntity>()
        val accountSmsSenders = ArrayList<AccountSmsSenderEntity>()
        val recurring = ArrayList<RecurringEntity>()
        val goals = ArrayList<GoalEntity>()
        val installments = ArrayList<InstallmentEntity>()
        val plans = ArrayList<PlanEntity>()
        var isV2 = false
        skipWs()
        if (peek() == ']') {
            i++
            return null
        }
        while (true) {
            skipWs()
            if (consume() != '{') throw IllegalArgumentException("not an object")
            var type: String? = null
            var backupVersion: Long? = null
            var id: String? = null
            var title: String? = null
            var amount: Long? = null
            var income: Boolean? = null
            var category: String? = null
            var dateMillis: Long? = null
            var accountId: String? = null
            var note: String? = null
            var store: String? = null
            var yearMonth: String? = null
            var limitAmount: Long? = null
            var name: String? = null
            var isIncome: Boolean? = null
            var isInvestment: Boolean? = null
            var parentId: String? = null
            var dayOfMonth: Long? = null
            var startYearMonth: String? = null
            var lastAppliedYearMonth: String? = null
            var active: Boolean? = null
            var done: Boolean? = null
            var target: Long? = null
            var savedAmount: Long? = null
            var dueMillis: Long? = null
            var priority: String? = null
            var bankName: String? = null
            var sender: String? = null
            var totalAmount: Long? = null
            var monthCount: Long? = null
            var paidCount: Long? = null
            var installmentAmount: Long? = null
            var startMillis: Long? = null
            skipWs()
            if (peek() != '}') {
                while (true) {
                    val key = parseString() ?: throw IllegalArgumentException("bad key")
                    skipWs()
                    if (consume() != ':') throw IllegalArgumentException("expected :")
                    when (key) {
                        "type" -> type = parseValueAsString()
                        "backupVersion" -> backupVersion = parseValueAsLong()
                        "id" -> id = parseValueAsString()
                        "title" -> title = parseValueAsString()
                        "amount" -> amount = parseValueAsLong()
                        "income" -> income = parseValueAsBoolean()
                        "category" -> category = parseValueAsString()
                        "dateMillis" -> dateMillis = parseValueAsLong()
                        "accountId" -> accountId = parseValueAsString()
                        "note" -> note = parseValueAsString()
                        "store" -> store = parseValueAsString()
                        "yearMonth" -> yearMonth = parseValueAsString()
                        "limitAmount" -> limitAmount = parseValueAsLong()
                        "name" -> name = parseValueAsString()
                        "isIncome" -> isIncome = parseValueAsBoolean()
                        "isInvestment" -> isInvestment = parseValueAsBoolean()
                        "parentId" -> parentId = parseValueAsString()
                        "dayOfMonth" -> dayOfMonth = parseValueAsLong()
                        "startYearMonth" -> startYearMonth = parseValueAsString()
                        "lastAppliedYearMonth" -> lastAppliedYearMonth = parseValueAsString()
                        "active" -> active = parseValueAsBoolean()
                        "done" -> done = parseValueAsBoolean()
                        "target" -> target = parseValueAsLong()
                        "savedAmount" -> savedAmount = parseValueAsLong()
                        "dueMillis" -> dueMillis = parseValueAsLong()
                        "priority" -> priority = parseValueAsString()
                        "bankName" -> bankName = parseValueAsString()
                        "sender" -> sender = parseValueAsString()
                        "totalAmount" -> totalAmount = parseValueAsLong()
                        "monthCount" -> monthCount = parseValueAsLong()
                        "paidCount" -> paidCount = parseValueAsLong()
                        "installmentAmount" -> installmentAmount = parseValueAsLong()
                        "startMillis" -> startMillis = parseValueAsLong()
                        else -> skipValue()
                    }
                    skipWs()
                    when (consume()) {
                        ',' -> continue
                        '}' -> break
                        else -> throw IllegalArgumentException("expected , or }")
                    }
                }
            } else {
                i++
            }
            if (backupVersion == 2L) isV2 = true
            when (type) {
                "transaction" -> {
                    if (title != null && amount != null && income != null) {
                        transactions.add(
                            MoneyTransaction(
                                id = id?.ifBlank { UUID.randomUUID().toString() }
                                    ?: UUID.randomUUID().toString(),
                                title = title,
                                amount = amount,
                                income = income,
                                category = category?.ifBlank { "سایر" } ?: "سایر",
                                dateMillis = dateMillis ?: 0L,
                                accountId = accountId?.ifBlank { "main" } ?: "main",
                                note = note ?: "",
                                store = store ?: ""
                            )
                        )
                    }
                }
                "budget" -> {
                    if (yearMonth != null && category != null && limitAmount != null) {
                        budgets.add(BudgetEntity(yearMonth, category, limitAmount))
                    }
                }
                "category" -> {
                    if (name != null && isIncome != null) {
                        categories.add(
                            CategoryEntity(
                                name,
                                isIncome,
                                isInvestment ?: false,
                                parentId
                            )
                        )
                    }
                }
                "account" -> {
                    if (id != null && name != null) {
                        accounts.add(AccountEntity(id, name, bankName?.ifBlank { null }))
                    }
                }
                "accountSmsSender" -> {
                    if (accountId != null && !accountId.isBlank() && !sender.isNullOrBlank()) {
                        accountSmsSenders.add(AccountSmsSenderEntity(accountId, sender))
                    }
                }
                "recurring" -> {
                    if (id != null && title != null && amount != null && income != null &&
                        category != null && accountId != null && dayOfMonth != null &&
                        startYearMonth != null
                    ) {
                        recurring.add(
                            RecurringEntity(
                                id = id,
                                title = title,
                                amount = amount,
                                income = income,
                                category = category,
                                accountId = accountId,
                                dayOfMonth = dayOfMonth.toInt(),
                                startYearMonth = startYearMonth,
                                lastAppliedYearMonth = lastAppliedYearMonth,
                                active = active ?: true
                            )
                        )
                    }
                }
                "goal" -> {
                    if (id != null && name != null && target != null && accountId != null) {
                        goals.add(
                            GoalEntity(
                                id, name, target, accountId,
                                savedAmount ?: 0,
                                dueMillis,
                                priority ?: "متوسط",
                                category ?: "",
                                note ?: ""
                            )
                        )
                    }
                }
                "installment" -> {
                    if (id != null && title != null && totalAmount != null &&
                        monthCount != null && accountId != null
                    ) {
                        installments.add(
                            InstallmentEntity(
                                id = id,
                                title = title,
                                totalAmount = totalAmount,
                                monthCount = monthCount.toInt(),
                                paidCount = paidCount?.toInt() ?: 0,
                                bankName = bankName?.ifBlank { null },
                                installmentAmount = installmentAmount
                                    ?: if (monthCount > 0) totalAmount / monthCount else 0L,
                                startMillis = startMillis,
                                accountId = accountId
                            )
                        )
                    }
                }
                "plan" -> {
                    if (id != null && title != null) {
                        plans.add(
                            PlanEntity(
                                id = id,
                                title = title,
                                note = note ?: "",
                                dueMillis = dueMillis,
                                done = done ?: false
                            )
                        )
                    }
                }
                else -> {
                    // نشانگر نسخه یا نوع ناشناخته: نادیده گرفته می‌شود.
                }
            }
            skipWs()
            when (consume()) {
                ',' -> continue
                ']' -> break
                else -> throw IllegalArgumentException("expected , or ]")
            }
        }
        if (!isV2) return null
        return BackupData(transactions, budgets, categories, accounts, recurring, goals, accountSmsSenders, installments, plans)
    }

    private fun parseObject(): MoneyTransaction? {
        skipWs()
        if (consume() != '{') throw IllegalArgumentException("not an object")
        var id: String? = null
        var title: String? = null
        var amount: Long? = null
        var income: Boolean? = null
        var category: String? = null
        var dateMillis: Long? = null
        var accountId: String? = null
        var note: String? = null
        skipWs()
        if (peek() == '}') {
            i++
            return null
        }
        while (true) {
            val key = parseString() ?: throw IllegalArgumentException("bad key")
            skipWs()
            if (consume() != ':') throw IllegalArgumentException("expected :")
            when (key) {
                "id" -> id = parseValueAsString()
                "title" -> title = parseValueAsString()
                "amount" -> amount = parseValueAsLong()
                "income" -> income = parseValueAsBoolean()
                "category" -> category = parseValueAsString()
                "dateMillis" -> dateMillis = parseValueAsLong()
                "accountId" -> accountId = parseValueAsString()
                "note" -> note = parseValueAsString()
                else -> skipValue()
            }
            skipWs()
            when (consume()) {
                ',' -> continue
                '}' -> break
                else -> throw IllegalArgumentException("expected , or }")
            }
        }
        if (title == null || amount == null || income == null) return null
        return MoneyTransaction(
            id = id?.ifBlank { UUID.randomUUID().toString() } ?: UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            income = income,
            category = category?.ifBlank { "سایر" } ?: "سایر",
            dateMillis = dateMillis ?: 0L,
            accountId = accountId?.ifBlank { "main" } ?: "main",
            note = note ?: ""
        )
    }

    private fun parseValueAsString(): String? {
        val s = parseString()
        if (s == null) skipValue()
        return s
    }

    private fun parseValueAsLong(): Long? {
        skipWs()
        val start = i
        while (i < n && (text[i].isDigit() || text[i] == '-' || text[i] == '+')) i++
        if (start == i) {
            skipValue()
            return null
        }
        return text.substring(start, i).toLongOrNull()
    }

    private fun parseValueAsBoolean(): Boolean? {
        skipWs()
        return when {
            text.startsWith("true", i) -> {
                i += 4
                true
            }
            text.startsWith("false", i) -> {
                i += 5
                false
            }
            else -> {
                skipValue()
                null
            }
        }
    }

    private fun parseString(): String? {
        skipWs()
        if (peek() != '"') return null
        i++
        val sb = StringBuilder()
        while (i < n) {
            val c = text[i]
            when {
                c == '"' -> {
                    i++
                    return sb.toString()
                }
                c == '\\' && i + 1 < n -> {
                    when (val e = text[i + 1]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (i + 5 < n) {
                                text.substring(i + 2, i + 6).toIntOrNull(16)?.let {
                                    sb.append(it.toChar())
                                }
                                i += 4
                            }
                        }
                        else -> sb.append(e)
                    }
                    i += 2
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return null
    }

    private fun skipValue() {
        skipWs()
        when (peek()) {
            '"' -> parseString()
            '{', '[' -> {
                val open = consume()!!
                val close = if (open == '{') '}' else ']'
                var depth = 1
                while (i < n && depth > 0) {
                    val c = text[i]
                    if (c == '"') {
                        parseString()
                        continue
                    }
                    if (c == open) depth++
                    if (c == close) depth--
                    i++
                }
            }
            else -> {
                while (i < n && text[i] != ',' && text[i] != '}' && text[i] != ']') i++
            }
        }
    }

    private fun skipWs() {
        while (i < n && text[i].isWhitespace()) i++
    }

    private fun peek(): Char? = if (i < n) text[i] else null

    private fun consume(): Char? = if (i < n) text[i++] else null
}
