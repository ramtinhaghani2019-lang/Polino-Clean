package com.polino.app.dashboard

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

private data class MoneyTransaction(val title: String, val amount: Long, val income: Boolean)
private const val PREFS = "polino_finance_v1"
private const val KEY = "transactions"

private fun loadTransactions(context: Context): List<MoneyTransaction> = runCatching {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
    val array = JSONArray(raw)
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        MoneyTransaction(item.getString("title"), item.getLong("amount"), item.getBoolean("income"))
    }
}.getOrDefault(emptyList())

private fun saveTransactions(context: Context, transactions: List<MoneyTransaction>): Boolean {
    val array = JSONArray()
    transactions.forEach { transaction ->
        array.put(JSONObject().put("title", transaction.title).put("amount", transaction.amount).put("income", transaction.income))
    }
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).commit()
}

private fun money(value: Long): String = NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(value) + " تومان"

@Composable
fun FinanceDashboard() {
    val context = LocalContext.current
    var transactions by remember { mutableStateOf(loadTransactions(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val income = transactions.filter { it.income }.sumOf { it.amount }
    val expense = transactions.filterNot { it.income }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("داشبورد مالی پولینو", style = MaterialTheme.typography.headlineSmall)
            Text("اطلاعات ثبت‌شده روی همین دستگاه", style = MaterialTheme.typography.labelMedium)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("موجودی کل")
                    Text(money(income - expense), style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { SummaryCard("درآمد", money(income)) }
                Box(Modifier.weight(1f)) { SummaryCard("هزینه", money(expense)) }
            }
        }
        item { Text("آخرین تراکنش‌ها", style = MaterialTheme.typography.titleLarge) }
        if (transactions.isEmpty()) {
            item { Text("هنوز تراکنشی ثبت نشده است.") }
        }
        items(transactions) { transaction ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(transaction.title)
                    Text(
                        (if (transaction.income) "+" else "−") + money(transaction.amount),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        item {
            Button(onClick = { error = ""; showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("افزودن تراکنش")
            }
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showDialog) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var isIncome by remember { mutableStateOf(false) }
        var validation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("تراکنش جدید") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان") }, singleLine = true)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { char -> char.isDigit() || char in "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩" } },
                        label = { Text("مبلغ به تومان") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Row {
                        RadioButton(selected = !isIncome, onClick = { isIncome = false })
                        Text("هزینه")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = isIncome, onClick = { isIncome = true })
                        Text("درآمد")
                    }
                    if (validation.isNotEmpty()) Text(validation, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val normalized = amount.map { char ->
                        when (char) {
                            in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
                            in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
                            else -> char
                        }
                    }.joinToString("")
                    val value = normalized.toLongOrNull()
                    if (title.isBlank() || value == null || value <= 0L) {
                        validation = "عنوان و مبلغ معتبر وارد کنید."
                    } else {
                        val updated = listOf(MoneyTransaction(title.trim(), value, isIncome)) + transactions
                        if (saveTransactions(context, updated)) {
                            transactions = updated
                            showDialog = false
                            error = ""
                        } else {
                            validation = "ذخیره اطلاعات ناموفق بود. دوباره تلاش کنید."
                        }
                    }
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun SummaryCard(title: String, amount: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title)
            Text(amount, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
