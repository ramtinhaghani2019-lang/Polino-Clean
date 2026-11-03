package com.polino.app.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private val EXPENSE_CATEGORIES =
    listOf("خوراک", "مسکن", "حمل‌ونقل", "سلامت", "آموزش", "تفریح", "پوشاک", "سایر")
private val INCOME_CATEGORIES = listOf("حقوق", "فروش", "هدیه", "سایر")

private enum class TypeFilter { ALL, INCOME, EXPENSE }

private fun money(value: Long): String =
    NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(value) + " تومان"

private fun normalizeDigits(input: String): String = input.map { char ->
    when (char) {
        in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
        else -> char
    }
}.joinToString("")

@Composable
fun FinanceDashboard() {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MoneyTransaction?>(null) }
    var error by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TypeFilter.ALL) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilterExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            repository.migrateIfNeeded()
            transactions = repository.getAll()
        }.onFailure {
            error = "بارگذاری اطلاعات ناموفق بود. دوباره تلاش کنید."
        }
    }

    val income = transactions.filter { it.income }.sumOf { it.amount }
    val expense = transactions.filterNot { it.income }.sumOf { it.amount }

    val visible = transactions
        .filter {
            when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.INCOME -> it.income
                TypeFilter.EXPENSE -> !it.income
            }
        }
        .filter { categoryFilter == null || it.category == categoryFilter }
        .filter { searchQuery.isBlank() || it.title.contains(searchQuery.trim(), ignoreCase = true) }
        .sortedByDescending { it.dateMillis }

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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("موجودی کل", color = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        money(income - expense),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { SummaryCard("درآمد", money(income), true) }
                Box(Modifier.weight(1f)) { SummaryCard("هزینه", money(expense), false) }
            }
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("جستجو در عنوان تراکنش") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = typeFilter == TypeFilter.ALL,
                    onClick = { typeFilter = TypeFilter.ALL },
                    label = { Text("همه") }
                )
                FilterChip(
                    selected = typeFilter == TypeFilter.INCOME,
                    onClick = { typeFilter = TypeFilter.INCOME },
                    label = { Text("درآمد") }
                )
                FilterChip(
                    selected = typeFilter == TypeFilter.EXPENSE,
                    onClick = { typeFilter = TypeFilter.EXPENSE },
                    label = { Text("هزینه") }
                )
            }
        }
        item {
            Box {
                OutlinedButton(onClick = { categoryFilterExpanded = true }) {
                    Text(categoryFilter ?: "همه دسته‌ها")
                }
                DropdownMenu(
                    expanded = categoryFilterExpanded,
                    onDismissRequest = { categoryFilterExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("همه دسته‌ها") },
                        onClick = { categoryFilter = null; categoryFilterExpanded = false }
                    )
                    (EXPENSE_CATEGORIES + INCOME_CATEGORIES).distinct().forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = { categoryFilter = category; categoryFilterExpanded = false }
                        )
                    }
                }
            }
        }
        item { Text("تراکنش‌ها", style = MaterialTheme.typography.titleLarge) }
        if (transactions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("هنوز تراکنشی ثبت نشده است.", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "با دکمه «افزودن تراکنش» اولین تراکنش را ثبت کنید.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else if (visible.isEmpty()) {
            item { Text("تراکنشی با این فیلتر پیدا نشد.") }
        }
        items(visible) { transaction ->
            Card(
                onClick = { editing = transaction; showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(transaction.title)
                        Text(
                            (if (transaction.income) "+" else "−") + money(transaction.amount),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (transaction.income) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(transaction.category, style = MaterialTheme.typography.bodySmall)
                        Text(JalaliDate.formatJalali(transaction.dateMillis), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Button(
                onClick = { editing = null; error = ""; showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("افزودن تراکنش")
            }
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showDialog) {
        TransactionDialog(
            initial = editing,
            onDismiss = { showDialog = false; editing = null; error = "" },
            onSave = { title, amount, isIncome, category, dateMillis ->
                val transaction = if (editing == null) {
                    MoneyTransaction(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = amount,
                        income = isIncome,
                        category = category,
                        dateMillis = dateMillis
                    )
                } else {
                    editing!!.copy(
                        title = title,
                        amount = amount,
                        income = isIncome,
                        category = category,
                        dateMillis = dateMillis
                    )
                }
                val ok = runCatching {
                    repository.upsert(transaction)
                    transactions = repository.getAll()
                }.isSuccess
                if (ok) {
                    showDialog = false
                    editing = null
                    error = ""
                } else {
                    error = "ذخیره اطلاعات ناموفق بود. دوباره تلاش کنید."
                }
                ok
            },
            onDelete = { target ->
                val ok = runCatching {
                    repository.delete(target.id)
                    transactions = repository.getAll()
                }.isSuccess
                if (ok) {
                    showDialog = false
                    editing = null
                } else {
                    error = "حذف اطلاعات ناموفق بود. دوباره تلاش کنید."
                    showDialog = false
                    editing = null
                }
                ok
            }
        )
    }
}

@Composable
private fun SummaryCard(title: String, amount: String, isIncome: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title)
            Text(
                amount,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TransactionDialog(
    initial: MoneyTransaction?,
    onDismiss: () -> Unit,
    onSave: suspend (title: String, amount: Long, income: Boolean, category: String, dateMillis: Long) -> Boolean,
    onDelete: suspend (MoneyTransaction) -> Boolean
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var isIncome by remember { mutableStateOf(initial?.income ?: false) }
    var category by remember {
        mutableStateOf(initial?.category ?: EXPENSE_CATEGORIES.first())
    }
    var dateMillis by remember {
        mutableStateOf(
            initial?.dateMillis?.takeIf { it != 0L } ?: System.currentTimeMillis()
        )
    }
    var validation by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val categories = if (isIncome) INCOME_CATEGORIES else EXPENSE_CATEGORIES
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("انصراف") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm && initial != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف تراکنش") },
            text = { Text("«${initial.title}» حذف شود؟ این کار قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch { onDelete(initial) }
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("انصراف") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "تراکنش جدید" else "ویرایش تراکنش") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newAmount ->
                        amount = newAmount.filter { char ->
                            char.isDigit() || char in "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩"
                        }
                    },
                    label = { Text("مبلغ به تومان") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row {
                    RadioButton(selected = !isIncome, onClick = {
                        isIncome = false
                        if (category !in EXPENSE_CATEGORIES) category = EXPENSE_CATEGORIES.first()
                    })
                    Text("هزینه")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = isIncome, onClick = {
                        isIncome = true
                        if (category !in INCOME_CATEGORIES) category = INCOME_CATEGORIES.first()
                    })
                    Text("درآمد")
                }
                Box {
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(category)
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = { category = item; categoryExpanded = false }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تاریخ: ${JalaliDate.formatJalali(dateMillis)}")
                    TextButton(onClick = { showDatePicker = true }) { Text("تغییر تاریخ") }
                }
                if (initial != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("حذف این تراکنش", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (validation.isNotEmpty()) {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = normalizeDigits(amount).toLongOrNull()
                if (title.isBlank() || value == null || value <= 0L) {
                    validation = "عنوان و مبلغ معتبر وارد کنید."
                } else {
                    scope.launch {
                        if (!onSave(title.trim(), value, isIncome, category, dateMillis)) {
                            validation = "ذخیره اطلاعات ناموفق بود. دوباره تلاش کنید."
                        }
                    }
                }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
