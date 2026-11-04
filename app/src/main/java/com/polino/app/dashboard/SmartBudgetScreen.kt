package com.polino.app.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.polino.app.data.BudgetEntity
import com.polino.app.data.BudgetRepository
import com.polino.app.data.CategoryEntity
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import com.polino.app.data.expenseCategoryNames
import com.polino.app.data.nextJalaliYearMonth
import kotlinx.coroutines.launch

/**
 * صفحه اختصاصی بودجه‌بندی هوشمند: ناوبر ماه شمسی با نشان «ماه جاری»،
 * فهرست بودجه‌های ماه با پیشرفت خرج در برابر سقف، حالت خالی، و فرم
 * «بودجه جدید» با دسته (اجباری) و سقف ماهانه (اجباری).
 * داده‌ها همان BudgetRepository سراسری‌اند؛ فروشگاه دومی ساخته نمی‌شود.
 */
@Composable
fun SmartBudgetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val transactionRepository = remember { TransactionRepository(context) }
    val budgetRepository = remember { BudgetRepository(context) }
    val scope = rememberCoroutineScope()
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var budgets by remember { mutableStateOf<List<BudgetEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var selectedYearMonth by remember { mutableStateOf(JalaliDate.currentJalaliYearMonth()) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            transactions = transactionRepository.getAll()
            categories = transactionRepository.getCategories()
        }
    }

    LaunchedEffect(selectedYearMonth) {
        budgets = budgetRepository.getBudgets(selectedYearMonth)
    }

    /** پیشرفت بودجه همیشه نسبت به کل ماه سنجیده می‌شود، نه حساب فیلترشده. */
    val spentAll = spentByCategory(transactionsInMonth(transactions, selectedYearMonth))
    val isCurrentMonth = selectedYearMonth == JalaliDate.currentJalaliYearMonth()
    val totalCap = budgets.sumOf { it.limitAmount }
    val totalSpent = budgets.sumOf { budget ->
        spentAll.firstOrNull { it.first == budget.category }?.second ?: 0L
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) { Text("بازگشت") }
            Text("بودجه‌بندی هوشمند", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = {
                        selectedYearMonth = JalaliDate.previousJalaliYearMonth(selectedYearMonth)
                    }) { Text("ماه قبل") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            JalaliDate.jalaliMonthDisplay(selectedYearMonth),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (isCurrentMonth) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "ماه جاری",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            selectedYearMonth = nextJalaliYearMonth(selectedYearMonth)
                        },
                        enabled = !isCurrentMonth
                    ) { Text("ماه بعد") }
                }
            }
            if (budgets.isNotEmpty()) {
                item {
                    Text(
                        "مجموع سقف‌ها: ${money(totalCap)} — خرج شده: ${money(totalSpent)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (budgets.isEmpty()) {
                item {
                    Text(
                        "برای ${JalaliDate.jalaliMonthDisplay(selectedYearMonth)} بودجه‌ای تعیین نشده است.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                item {
                    Text(
                        "با «بودجه جدید» برای یک دسته سقف ماهانه تعیین کنید.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            budgets.forEach { budget ->
                val spentAmount = spentAll.firstOrNull { it.first == budget.category }?.second ?: 0L
                val remaining = budget.limitAmount - spentAmount
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(budget.category, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    scope.launch {
                                        budgetRepository.deleteBudget(selectedYearMonth, budget.category)
                                        budgets = budgetRepository.getBudgets(selectedYearMonth)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف بودجه")
                                }
                            }
                            Text("خرج شده: ${money(spentAmount)} از ${money(budget.limitAmount)}")
                            LinearProgressIndicator(
                                progress = {
                                    if (budget.limitAmount > 0L) {
                                        (spentAmount.toFloat() / budget.limitAmount.toFloat())
                                            .coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                if (remaining >= 0) {
                                    "باقی‌مانده: ${money(remaining)}"
                                } else {
                                    "بیشتر از بودجه: ${money(-remaining)}"
                                },
                                color = if (remaining >= 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بودجه جدید برای ${JalaliDate.jalaliMonthDisplay(selectedYearMonth)}")
                }
            }
        }
    }

    if (showDialog) {
        SmartBudgetDialog(
            expenseCategories = expenseCategoryNames(categories),
            onDismiss = { showDialog = false },
            onSave = { category, limit ->
                scope.launch {
                    budgetRepository.setBudget(selectedYearMonth, category, limit)
                    budgets = budgetRepository.getBudgets(selectedYearMonth)
                    showDialog = false
                }
            }
        )
    }
}

@Composable
private fun SmartBudgetDialog(
    expenseCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Long) -> Unit
) {
    var category by remember { mutableStateOf(expenseCategories.firstOrNull() ?: "سایر") }
    var amount by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بودجه جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        expenseCategories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = { category = item; categoryExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newAmount ->
                        amount = newAmount.filter { char ->
                            char.isDigit() || char in "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩"
                        }
                    },
                    label = { Text("سقف بودجه ماهانه به تومان") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (validation.isNotEmpty()) {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = normalizeDigits(amount).toLongOrNull()
                if (value == null || value <= 0L) {
                    validation = "مبلغ معتبر وارد کنید."
                } else {
                    onSave(category, value)
                }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
