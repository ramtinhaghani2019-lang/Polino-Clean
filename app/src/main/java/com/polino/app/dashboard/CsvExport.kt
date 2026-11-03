package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction

/**
 * خروجی CSV تراکنش‌ها برای باز کردن در اکسل؛ BOM ابتدای فایل باعث می‌شود
 * اکسل متن فارسی را درست نشان دهد.
 */
internal fun transactionsToCsv(transactions: List<MoneyTransaction>): String = buildString {
    append('\uFEFF')
    append("عنوان,مبلغ,نوع,دسته,تاریخ شمسی,یادداشت\n")
    transactions.forEach { t ->
        append(csvCell(t.title))
        append(',')
        append(t.amount)
        append(',')
        append(if (t.income) "درآمد" else "هزینه")
        append(',')
        append(csvCell(t.category))
        append(',')
        append(csvCell(JalaliDate.formatJalali(t.dateMillis)))
        append(',')
        append(csvCell(t.note))
        append('\n')
    }
}

private fun csvCell(value: String): String {
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
    val escaped = value.replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}
