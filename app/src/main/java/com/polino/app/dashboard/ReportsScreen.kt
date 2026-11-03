package com.polino.app.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.polino.app.data.AccountEntity
import com.polino.app.data.BudgetEntity
import com.polino.app.data.BudgetRepository
import com.polino.app.data.CategoryEntity
import com.polino.app.data.GoalEntity
import com.polino.app.data.expenseCategoryNames
import com.polino.app.data.goalProgressFraction
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import com.polino.app.data.parseLegacyTransactions
import com.polino.app.data.transactionsToJson
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val transactionRepository = remember { TransactionRepository(context) }
    val budgetRepository = remember { BudgetRepository(context) }
    val scope = rememberCoroutineScope()
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var budgets by remember { mutableStateOf<List<BudgetEntity>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var selectedAccountId by remember { mutableStateOf("all") }
    var accountExpanded by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var goals by remember { mutableStateOf<List<GoalEntity>>(emptyList()) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val yearMonth = remember { JalaliDate.currentJalaliYearMonth() }

    LaunchedEffect(Unit) {
        runCatching {
            transactionRepository.migrateIfNeeded()
            transactions = transactionRepository.getAll()
            accounts = transactionRepository.getAccounts()
            categories = transactionRepository.getCategories()
            goals = transactionRepository.getGoals()
            budgets = budgetRepository.getBudgets(yearMonth)
        }.onFailure {
            error = "بارگذاری اطلاعات ناموفق بود. دوباره تلاش کنید."
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(transactionsToJson(transactions).toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("no output stream")
                }.onSuccess { info = "پشتیبان‌گیری انجام شد." }
                    .onFailure { error = "پشتیبان‌گیری ناموفق بود. دوباره تلاش کنید." }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(transactionsToCsv(transactions).toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("no output stream")
                }.onSuccess { info = "خروجی CSV انجام شد." }
                    .onFailure { error = "خروجی CSV ناموفق بود. دوباره تلاش کنید." }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(Charsets.UTF_8)
                    } ?: ""
                }.getOrDefault("")
                val imported = parseLegacyTransactions(text)
                if (imported.isEmpty()) {
                    error = "فایلی برای بازیابی پیدا نشد یا خراب است."
                } else {
                    runCatching {
                        imported.forEach { transactionRepository.upsert(it) }
                        transactions = transactionRepository.getAll()
                    }.onSuccess { info = "${imported.size} تراکنش بازیابی شد." }
                        .onFailure { error = "بازیابی ناموفق بود. دوباره تلاش کنید." }
                }
            }
        }
    }

    val monthTransactionsAll = transactionsInMonth(transactions, yearMonth)
    val monthTransactions = monthTransactionsAll.filter {
        selectedAccountId == "all" || it.accountId == selectedAccountId
    }
    val totals = monthTotals(monthTransactions)
    val spent = spentByCategory(monthTransactions)
    /** پیشرفت بودجه همیشه نسبت به کل ماه سنجیده می‌شود، نه حساب فیلترشده. */
    val spentAll = spentByCategory(monthTransactionsAll)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) { Text("بازگشت") }
            Text("گزارش و بودجه", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box {
                    OutlinedButton(onClick = { accountExpanded = true }) {
                        Text(
                            if (selectedAccountId == "all") "همه حساب‌ها"
                            else accounts.firstOrNull { it.id == selectedAccountId }?.name
                                ?: "همه حساب‌ها"
                        )
                    }
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("همه حساب‌ها") },
                            onClick = { selectedAccountId = "all"; accountExpanded = false }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = { selectedAccountId = account.id; accountExpanded = false }
                            )
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "گزارش ماه ${JalaliDate.jalaliMonthDisplay(yearMonth)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("درآمد: ${money(totals.income)}")
                        Text("هزینه: ${money(totals.expense)}")
                        Text(
                            "مانده: ${money(totals.income - totals.expense)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (totals.income - totals.expense >= 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
            item { Text("هزینه‌ها به تفکیک دسته", style = MaterialTheme.typography.titleMedium) }
            val maxSpent = spent.maxOfOrNull { it.second } ?: 1L
            if (spent.isEmpty()) {
                item { Text("در این ماه هزینه‌ای ثبت نشده است.") }
            } else {
                spent.forEach { (category, amount) ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(category)
                                Text(money(amount))
                            }
                            LinearProgressIndicator(
                                progress = {
                                    (amount.toFloat() / maxSpent.toFloat()).coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            item { Text("روند ۶ ماه اخیر", style = MaterialTheme.typography.titleMedium) }
            val trendBase = if (selectedAccountId == "all") transactions
            else transactions.filter { it.accountId == selectedAccountId }
            val trend = monthlyTrend(trendBase, JalaliDate.lastJalaliYearMonths(6))
            val maxTrend = (trend.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L)
                .takeIf { it > 0 } ?: 1L
            trend.forEach { month ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            JalaliDate.jalaliMonthDisplay(month.yearMonth),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("درآمد")
                            LinearProgressIndicator(
                                progress = {
                                    (month.income.toFloat() / maxTrend.toFloat()).coerceIn(0f, 1f)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Text(money(month.income))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("هزینه")
                            LinearProgressIndicator(
                                progress = {
                                    (month.expense.toFloat() / maxTrend.toFloat()).coerceIn(0f, 1f)
                                },
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            Text(money(month.expense))
                        }
                    }
                }
            }
            item { Text("بودجه‌های ماه جاری", style = MaterialTheme.typography.titleMedium) }
            if (budgets.isEmpty()) {
                item { Text("هنوز بودجه‌ای تعیین نشده است.") }
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
                                        budgetRepository.deleteBudget(yearMonth, budget.category)
                                        budgets = budgetRepository.getBudgets(yearMonth)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف بودجه")
                                }
                            }
                            Text("خرج شده: ${money(spentAmount)} از ${money(budget.limitAmount)}")
                            LinearProgressIndicator(
                                progress = {
                                    (spentAmount.toFloat() / budget.limitAmount.toFloat())
                                        .coerceIn(0f, 1f)
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
                    onClick = { showBudgetDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تعیین بودجه")
                }
            }
            item { Text("اهداف پس‌انداز", style = MaterialTheme.typography.titleMedium) }
            if (goals.isEmpty()) {
                item { Text("هنوز هدفی تعیین نشده است.") }
            }
            goals.forEach { goal ->
                // پیشرفت هدف از روی موجودی کل حساب حساب می‌شود، نه فقط ماه جاری.
                val balance = transactions.filter { it.accountId == goal.accountId }
                    .sumOf { if (it.income) it.amount else -it.amount }
                val fraction = goalProgressFraction(balance, goal.target)
                val accountName = accounts.firstOrNull { it.id == goal.accountId }?.name
                    ?: "اصلی"
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
                                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    scope.launch {
                                        transactionRepository.deleteGoal(goal.id)
                                        goals = transactionRepository.getGoals()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف هدف")
                                }
                            }
                            Text("حساب: $accountName")
                            Text("${money(balance)} از ${money(goal.target)}")
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (fraction >= 1f) {
                                Text("تکمیل شد", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("هدف جدید")
                }
            }
            item { Text("پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { exportLauncher.launch("polino-backup.json") }) {
                        Text("خروجی JSON")
                    }
                    Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Text("بازیابی از فایل")
                    }
                }
            }
            item {
                Button(onClick = { csvExportLauncher.launch("polino-transactions.csv") }) {
                    Text("خروجی CSV (اکسل)")
                }
            }
            item {
                if (info.isNotEmpty()) Text(info, color = MaterialTheme.colorScheme.primary)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            expenseCategories = expenseCategoryNames(categories),
            onDismiss = { showBudgetDialog = false },
            onSave = { category, limit ->
                scope.launch {
                    budgetRepository.setBudget(yearMonth, category, limit)
                    budgets = budgetRepository.getBudgets(yearMonth)
                    showBudgetDialog = false
                }
            }
        )
    }

    if (showGoalDialog) {
        GoalDialog(
            accounts = accounts,
            onDismiss = { showGoalDialog = false },
            onSave = { name, target, accountId ->
                scope.launch {
                    transactionRepository.addGoal(
                        GoalEntity(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            target = target,
                            accountId = accountId
                        )
                    )
                    goals = transactionRepository.getGoals()
                    showGoalDialog = false
                }
            }
        )
    }
}

@Composable
private fun GoalDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Long, accountId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "main") }
    var accountExpanded by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("هدف پس‌انداز") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "پیشرفت هدف از روی موجودی حساب انتخاب‌شده حساب می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام هدف") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newAmount ->
                        amount = newAmount.filter { char ->
                            char.isDigit() || char in "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩"
                        }
                    },
                    label = { Text("مبلغ هدف به تومان") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (accounts.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { accountExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(accounts.firstOrNull { it.id == accountId }?.name ?: "اصلی")
                        }
                        DropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = { accountId = account.id; accountExpanded = false }
                                )
                            }
                        }
                    }
                }
                if (validation.isNotEmpty()) {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = normalizeDigits(amount).toLongOrNull()
                val error = when {
                    name.isBlank() -> "نام هدف را وارد کنید."
                    parsed == null || parsed <= 0 -> "مبلغ معتبر وارد کنید."
                    else -> ""
                }
                validation = error
                if (error.isEmpty() && parsed != null) {
                    onSave(name.trim(), parsed, accountId)
                }
            }) { Text("ثبت") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun BudgetDialog(
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
        title = { Text("تعیین بودجه") },
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
                    label = { Text("سقف بودجه به تومان") },
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
