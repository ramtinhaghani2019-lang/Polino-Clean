package com.polino.app.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.polino.app.BuildConfig
import com.polino.app.data.AccountEntity
import com.polino.app.data.BudgetEntity
import com.polino.app.data.BudgetRepository
import com.polino.app.data.CategoryEntity
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.RecurringEntity
import com.polino.app.data.TransactionRepository
import com.polino.app.data.dueYearMonths
import com.polino.app.data.expenseCategoryNames
import com.polino.app.data.incomeCategoryNames
import kotlinx.coroutines.launch
import java.util.UUID

private enum class TypeFilter { ALL, INCOME, EXPENSE }

@Composable
fun FinanceDashboard() {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val budgetRepository = remember { BudgetRepository(context) }
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MoneyTransaction?>(null) }
    var error by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TypeFilter.ALL) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilterExpanded by remember { mutableStateOf(false) }
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var selectedAccountId by remember { mutableStateOf("main") }
    var accountExpanded by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showRenameAccountDialog by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var budgets by remember { mutableStateOf<List<BudgetEntity>>(emptyList()) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var recurring by remember { mutableStateOf<List<RecurringEntity>>(emptyList()) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching {
            repository.migrateIfNeeded()
            transactions = repository.getAll()
            accounts = repository.getAccounts()
            categories = repository.getCategories()
            // اعمال تراکنش‌های تکرارشونده عقب‌افتاده تا ماه جاری؛
            // هر ماه فقط یک‌بار ساخته می‌شود و تکرار نمی‌شود.
            val currentYm = JalaliDate.currentJalaliYearMonth()
            var applied = 0
            repository.getRecurring().filter { it.active }.forEach { rule ->
                var ruleApplied = false
                dueYearMonths(rule.startYearMonth, rule.lastAppliedYearMonth, currentYm)
                    .forEach { ym ->
                        val parts = ym.split("-")
                        val jy = parts[0].toInt()
                        val jm = parts[1].toInt()
                        val day = minOf(rule.dayOfMonth, JalaliDate.daysInJalaliMonth(jy, jm))
                        repository.upsert(
                            MoneyTransaction(
                                id = UUID.randomUUID().toString(),
                                title = rule.title,
                                amount = rule.amount,
                                income = rule.income,
                                category = rule.category,
                                dateMillis = JalaliDate.jalaliToMillis(jy, jm, day),
                                accountId = rule.accountId
                            )
                        )
                        applied++
                        ruleApplied = true
                    }
                if (ruleApplied) {
                    repository.updateRecurring(rule.copy(lastAppliedYearMonth = currentYm))
                }
            }
            if (applied > 0) transactions = repository.getAll()
            recurring = repository.getRecurring()
            budgets = budgetRepository.getBudgets(JalaliDate.currentJalaliYearMonth())
        }.onFailure {
            error = "بارگذاری اطلاعات ناموفق بود. دوباره تلاش کنید."
        }
    }

    val accountTransactions = transactions.filter { it.accountId == selectedAccountId }
    // موجودی بعد از هر تراکنش، از روی کل تاریخچه حساب (نه فیلترشده) تا
    // با فیلتر فعال هم عدد درست بماند؛ مرتب‌سازی با id برای ترتیب قطعی.
    val runningBalances = remember(accountTransactions) {
        var balance = 0L
        accountTransactions
            .sortedWith(compareBy({ it.dateMillis }, { it.id }))
            .associate { tx ->
                balance += if (tx.income) tx.amount else -tx.amount
                tx.id to balance
            }
    }
    val income = accountTransactions.filter { it.income }.sumOf { it.amount }
    val expense = accountTransactions.filterNot { it.income }.sumOf { it.amount }
    // بودجه‌ها سراسریِ ماه جاری‌اند (مثل صفحه گزارش)؛ هشدار هم بر اساس
    // همه حساب‌ها محاسبه می‌شود تا با همان بودجه‌ها هم‌خوان باشد.
    val overspent = remember(transactions, budgets) {
        overspentCategories(
            transactionsInMonth(transactions, JalaliDate.currentJalaliYearMonth()),
            budgets
        )
    }
    val nearing = remember(transactions, budgets) {
        nearingCategories(
            transactionsInMonth(transactions, JalaliDate.currentJalaliYearMonth()),
            budgets
        )
    }

    val visible = accountTransactions
        .filter {
            when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.INCOME -> it.income
                TypeFilter.EXPENSE -> !it.income
            }
        }
        .filter { categoryFilter == null || it.category == categoryFilter }
        .filter {
            searchQuery.isBlank() ||
                it.title.contains(searchQuery.trim(), ignoreCase = true) ||
                it.note.contains(searchQuery.trim(), ignoreCase = true) ||
                it.category.contains(searchQuery.trim(), ignoreCase = true)
        }
        .sortedByDescending { it.dateMillis }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("داشبورد مالی پولینو", style = MaterialTheme.typography.headlineSmall)
            Text("اطلاعات ثبت‌شده روی همین دستگاه", style = MaterialTheme.typography.labelMedium)
            Text("نسخه ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { accountExpanded = true }) {
                        Text(
                            accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "اصلی"
                        )
                    }
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
                Button(onClick = { showAccountDialog = true }) {
                    Text("حساب جدید")
                }
                TextButton(onClick = { showRenameAccountDialog = true }) {
                    Text("تغییر نام حساب")
                }
                if (selectedAccountId != "main") {
                    TextButton(onClick = { showDeleteAccountConfirm = true }) {
                        Text("حذف حساب", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
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
            val currentMonth = JalaliDate.currentJalaliYearMonth()
            val totals = remember(accountTransactions, currentMonth) {
                monthTotals(transactionsInMonth(accountTransactions, currentMonth))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "این ماه (${JalaliDate.jalaliMonthDisplay(currentMonth)})",
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
        if (overspent.isNotEmpty() || nearing.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "⚠ هشدار بودجه ماه جاری",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (overspent.isNotEmpty()) {
                            Text(
                                "رد شده از سقف:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            overspent.forEach { entry ->
                                Text(
                                    "${entry.category}: ${money(entry.spent)} از ${money(entry.limit)} تومان",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        if (nearing.isNotEmpty()) {
                            Text(
                                "نزدیک به سقف:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            nearing.forEach { entry ->
                                Text(
                                    "${entry.category}: ${money(entry.spent)} از ${money(entry.limit)} تومان",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
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
                label = { Text("جستجو در عنوان، یادداشت و دسته") },
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
                if (searchQuery.isNotBlank() || typeFilter != TypeFilter.ALL || categoryFilter != null) {
                    TextButton(onClick = {
                        searchQuery = ""
                        typeFilter = TypeFilter.ALL
                        categoryFilter = null
                    }) { Text("پاک کردن فیلترها") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        (expenseCategoryNames(categories) + incomeCategoryNames(categories))
                            .distinct().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = { categoryFilter = category; categoryFilterExpanded = false }
                                )
                            }
                    }
                }
                Button(onClick = { showCategoryDialog = true }) {
                    Text("دسته‌ها")
                }
                Button(onClick = { showRecurringDialog = true }) {
                    Text("تکرارشونده‌ها")
                }
            }
        }
        item { Text("تراکنش‌ها", style = MaterialTheme.typography.titleLarge) }
        if (accountTransactions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("در این حساب هنوز تراکنشی ثبت نشده است.", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "با دکمه «افزودن تراکنش» اولین تراکنش این حساب را ثبت کنید.",
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
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        categoryIcon(transaction.category),
                        contentDescription = transaction.category,
                        tint = if (transaction.income) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Column(
                        Modifier.weight(1f),
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
                        if (transaction.note.isNotBlank()) {
                            Text(transaction.note, style = MaterialTheme.typography.bodySmall)
                        }
                        runningBalances[transaction.id]?.let { balance ->
                            Text("موجودی: ${money(balance)}", style = MaterialTheme.typography.bodySmall)
                        }
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
            accounts = accounts,
            initialAccountId = editing?.accountId ?: selectedAccountId,
            expenseCategories = expenseCategoryNames(categories),
            incomeCategories = incomeCategoryNames(categories),
            onDismiss = { showDialog = false; editing = null; error = "" },
            onSave = { title, amount, isIncome, category, dateMillis, note, accountId ->
                val transaction = if (editing == null) {
                    MoneyTransaction(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = amount,
                        income = isIncome,
                        category = category,
                        dateMillis = dateMillis,
                        accountId = accountId,
                        note = note
                    )
                } else {
                    editing!!.copy(
                        title = title,
                        amount = amount,
                        income = isIncome,
                        category = category,
                        dateMillis = dateMillis,
                        accountId = accountId,
                        note = note
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

    if (showCategoryDialog) {
        CategoryDialog(
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onAdd = { name, isIncome ->
                val ok = repository.addCategory(name, isIncome)
                if (ok) categories = repository.getCategories()
                ok
            },
            onDelete = { name ->
                repository.deleteCategory(name)
                categories = repository.getCategories()
                transactions = repository.getAll()
            },
            onRename = { oldName, newName ->
                val ok = repository.renameCategory(oldName, newName)
                if (ok) {
                    categories = repository.getCategories()
                    transactions = repository.getAll()
                    if (categoryFilter == oldName) categoryFilter = newName
                }
                ok
            }
        )
    }

    if (showRecurringDialog) {
        RecurringDialog(
            recurring = recurring,
            accounts = accounts,
            expenseCategories = expenseCategoryNames(categories),
            incomeCategories = incomeCategoryNames(categories),
            onDismiss = { showRecurringDialog = false },
            onAdd = { rule ->
                repository.addRecurring(rule)
                recurring = repository.getRecurring()
            },
            onDelete = { id ->
                repository.deleteRecurring(id)
                recurring = repository.getRecurring()
            }
        )
    }

    if (showAccountDialog) {
        AccountDialog(
            onDismiss = { showAccountDialog = false },
            onSave = { name ->
                scope.launch {
                    val created = repository.addAccount(name)
                    accounts = repository.getAccounts()
                    selectedAccountId = created.id
                    showAccountDialog = false
                }
            }
        )
    }

    if (showRenameAccountDialog) {
        RenameAccountDialog(
            currentName = accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "اصلی",
            onDismiss = { showRenameAccountDialog = false },
            onSave = { name ->
                val ok = repository.renameAccount(selectedAccountId, name)
                if (ok) accounts = repository.getAccounts()
                ok
            }
        )
    }

    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("حذف حساب") },
            text = {
                Text("تراکنش‌های این حساب به حساب «اصلی» منتقل می‌شوند و حساب حذف می‌شود.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountConfirm = false
                    scope.launch {
                        repository.deleteAccount(selectedAccountId)
                        accounts = repository.getAccounts()
                        selectedAccountId = "main"
                    }
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun AccountDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حساب جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام حساب") },
                    singleLine = true
                )
                if (validation.isNotEmpty()) {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    validation = "نام حساب را وارد کنید."
                } else {
                    onSave(name.trim())
                }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun RenameAccountDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Boolean
) {
    var name by remember { mutableStateOf(currentName) }
    var validation by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر نام حساب") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام حساب") },
                    singleLine = true
                )
                if (validation.isNotEmpty()) {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    validation = "نام حساب را وارد کنید."
                } else {
                    scope.launch {
                        if (onSave(name.trim())) onDismiss()
                        else validation = "حسابی با این نام از قبل وجود دارد."
                    }
                }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun CategoryDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: suspend (String, Boolean) -> Boolean,
    onDelete: suspend (String) -> Unit,
    onRename: suspend (String, String) -> Boolean
) {
    var name by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دسته‌ها") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "حذف یک دسته، تراکنش‌هایش را به «سایر» می‌برد و بودجه‌هایش را پاک می‌کند. «سایر» قابل حذف نیست.",
                    style = MaterialTheme.typography.bodySmall
                )
                categories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category.name + if (category.isIncome) " (درآمد)" else " (هزینه)")
                        if (category.name != "سایر") {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { categoryToRename = category.name }) {
                                    Text("ویرایش")
                                }
                                TextButton(onClick = { categoryToDelete = category.name }) {
                                    Text("حذف", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("دسته جدید") },
                    singleLine = true
                )
                Row {
                    RadioButton(selected = !isIncome, onClick = { isIncome = false })
                    Text("هزینه")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = isIncome, onClick = { isIncome = true })
                    Text("درآمد")
                }
                if (validation.isNotEmpty()) {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    validation = "نام دسته را وارد کنید."
                } else {
                    scope.launch {
                        if (onAdd(name.trim(), isIncome)) {
                            name = ""
                            validation = ""
                        } else {
                            validation = "این دسته از قبل هست."
                        }
                    }
                }
            }) { Text("افزودن") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
    if (categoryToRename != null) {
        RenameCategoryDialog(
            currentName = categoryToRename!!,
            onDismiss = { categoryToRename = null },
            onSave = { newName ->
                val ok = onRename(categoryToRename!!, newName)
                if (ok) categoryToRename = null
                ok
            }
        )
    }
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("حذف دسته") },
            text = {
                Text("دسته «$categoryToDelete» حذف شود؟ تراکنش‌هایش به «سایر» می‌روند و بودجه‌هایش پاک می‌شوند.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = categoryToDelete
                    categoryToDelete = null
                    if (target != null) {
                        scope.launch { onDelete(target) }
                    }
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun RenameCategoryDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Boolean
) {
    var name by remember { mutableStateOf(currentName) }
    var validation by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر نام دسته") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دسته") },
                    singleLine = true
                )
                if (validation.isNotEmpty()) {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    validation = "نام دسته را وارد کنید."
                } else {
                    scope.launch {
                        if (onSave(name.trim())) onDismiss()
                        else validation = "این دسته از قبل هست."
                    }
                }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun RecurringDialog(
    recurring: List<RecurringEntity>,
    accounts: List<AccountEntity>,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    onDismiss: () -> Unit,
    onAdd: suspend (RecurringEntity) -> Unit,
    onDelete: suspend (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var category by remember {
        mutableStateOf(expenseCategories.firstOrNull() ?: "سایر")
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountId by remember {
        mutableStateOf(accounts.firstOrNull()?.id ?: "main")
    }
    var accountExpanded by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val categories = if (isIncome) incomeCategories else expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تراکنش‌های تکرارشونده") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "هر ماه در روز مشخص‌شده، این تراکنش خودکار ساخته می‌شود؛ مثلاً حقوق اول ماه یا اجاره. حذف قاعده، تراکنش‌های ساخته‌شده را پاک نمی‌کند.",
                    style = MaterialTheme.typography.bodySmall
                )
                recurring.forEach { rule ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${rule.title}${if (rule.active) "" else " (متوقف)"} — روز ${rule.dayOfMonth} هر ماه")
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    scope.launch { onAdd(rule.copy(active = !rule.active)) }
                                }) { Text(if (rule.active) "توقف" else "ادامه") }
                                TextButton(onClick = {
                                    editingId = rule.id
                                    title = rule.title
                                    amount = rule.amount.toString()
                                    isIncome = rule.income
                                    category = rule.category
                                    accountId = rule.accountId
                                    day = rule.dayOfMonth.toString()
                                    validation = ""
                                }) { Text("ویرایش") }
                                TextButton(onClick = {
                                    scope.launch { onDelete(rule.id) }
                                }) {
                                    Text("حذف", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Text(
                            rule.lastAppliedYearMonth?.let {
                                "آخرین اجرا: ${JalaliDate.jalaliMonthDisplay(it)}"
                            } ?: "هنوز اجرا نشده",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("مبلغ (تومان)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row {
                    RadioButton(selected = !isIncome, onClick = {
                        isIncome = false
                        if (category !in expenseCategories) {
                            category = expenseCategories.firstOrNull() ?: "سایر"
                        }
                    })
                    Text("هزینه")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = isIncome, onClick = {
                        isIncome = true
                        if (category !in incomeCategories) {
                            category = incomeCategories.firstOrNull() ?: "سایر"
                        }
                    })
                    Text("درآمد")
                }
                Box {
                    OutlinedButton(onClick = { categoryExpanded = true }) {
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
                if (accounts.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { accountExpanded = true }) {
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
                OutlinedTextField(
                    value = day,
                    onValueChange = { day = it },
                    label = { Text("روز ماه (۱ تا ۳۱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (validation.isNotEmpty()) {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
                if (editingId != null) {
                    TextButton(onClick = {
                        editingId = null
                        title = ""
                        amount = ""
                        day = ""
                        validation = ""
                    }) { Text("لغو ویرایش") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = normalizeDigits(amount).toLongOrNull()
                val parsedDay = normalizeDigits(day).toIntOrNull()
                val error = when {
                    title.isBlank() -> "عنوان را وارد کنید."
                    parsedAmount == null || parsedAmount <= 0 -> "مبلغ معتبر وارد کنید."
                    parsedDay == null || parsedDay !in 1..31 -> "روز ماه باید بین ۱ تا ۳۱ باشد."
                    else -> ""
                }
                validation = error
                if (error.isEmpty() && parsedAmount != null && parsedDay != null) {
                    val existing = recurring.firstOrNull { it.id == editingId }
                    val rule = RecurringEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        amount = parsedAmount,
                        income = isIncome,
                        category = category,
                        accountId = accountId,
                        dayOfMonth = parsedDay,
                        startYearMonth = existing?.startYearMonth
                            ?: JalaliDate.currentJalaliYearMonth(),
                        lastAppliedYearMonth = existing?.lastAppliedYearMonth,
                        active = existing?.active ?: true
                    )
                    scope.launch {
                        onAdd(rule)
                        title = ""
                        amount = ""
                        day = ""
                        editingId = null
                    }
                }
            }) { Text(if (editingId != null) "ذخیره" else "افزودن") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
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
    accounts: List<AccountEntity>,
    initialAccountId: String,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (title: String, amount: Long, income: Boolean, category: String, dateMillis: Long, note: String, accountId: String) -> Boolean,
    onDelete: suspend (MoneyTransaction) -> Boolean
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var isIncome by remember { mutableStateOf(initial?.income ?: false) }
    var category by remember {
        mutableStateOf(initial?.category ?: expenseCategories.firstOrNull() ?: "سایر")
    }
    var accountId by remember { mutableStateOf(initialAccountId) }
    var accountExpanded by remember { mutableStateOf(false) }
    var dateMillis by remember {
        mutableStateOf(
            initial?.dateMillis?.takeIf { it != 0L } ?: System.currentTimeMillis()
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var validation by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val categories = if (isIncome) incomeCategories else expenseCategories
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
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)") },
                    singleLine = true
                )
                Row {
                    RadioButton(selected = !isIncome, onClick = {
                        isIncome = false
                        if (category !in expenseCategories) {
                            category = expenseCategories.firstOrNull() ?: "سایر"
                        }
                    })
                    Text("هزینه")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = isIncome, onClick = {
                        isIncome = true
                        if (category !in incomeCategories) {
                            category = incomeCategories.firstOrNull() ?: "سایر"
                        }
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
                        accounts.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = { accountId = item.id; accountExpanded = false }
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
                        if (!onSave(title.trim(), value, isIncome, category, dateMillis, note.trim(), accountId)) {
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
