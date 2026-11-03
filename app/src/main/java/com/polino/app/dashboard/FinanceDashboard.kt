package com.polino.app.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.polino.app.R
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
private enum class SortOrder { NEWEST, OLDEST, AMOUNT_DESC, AMOUNT_ASC }

private val MidnightHero = Color(0xFF0F1730)
private val HeroGold = Color(0xFFE3B04B)
private val HeroIncome = Color(0xFF34C89E)
private val HeroExpense = Color(0xFFFF6B6B)
private val HeroMuted = Color(0xFF9AA3B8)

private fun ymKey(yearMonth: String): Int {
    val parts = yearMonth.split("-")
    return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 12 +
        (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

@Composable
fun FinanceDashboard(
    onNavigateToReports: () -> Unit = {}
) {
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
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST) }
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

    val accountTransactions = transactions.filter {
        selectedAccountId == "all" || it.accountId == selectedAccountId
    }
    // موجودی بعد از هر تراکنش، از روی کل تاریخچه حساب (نه فیلترشده) تا
    // با فیلتر فعال هم عدد درست بماند؛ مرتب‌سازی با id برای ترتیب قطعی.
    // برای هر حساب جداگانه حساب می‌شود تا در نمای «همه حساب‌ها» هم درست بماند.
    val runningBalances = remember(accountTransactions) {
        accountTransactions
            .groupBy { it.accountId }
            .flatMap { (_, group) ->
                var balance = 0L
                group
                    .sortedWith(compareBy({ it.dateMillis }, { it.id }))
                    .map { tx ->
                        balance += if (tx.income) tx.amount else -tx.amount
                        tx.id to balance
                    }
            }
            .toMap()
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

    val filtered = accountTransactions
        .filter {
            when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.INCOME -> it.income
                TypeFilter.EXPENSE -> !it.income
            }
        }
        .filter { categoryFilter == null || it.category == categoryFilter }
        .filter {
            val query = searchQuery.trim()
            query.isBlank() ||
                it.title.contains(query, ignoreCase = true) ||
                it.note.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                normalizeDigits(query).toLongOrNull()?.let { amount -> it.amount == amount } == true
        }
    val visible = when (sortOrder) {
        SortOrder.NEWEST -> filtered.sortedByDescending { it.dateMillis }
        SortOrder.OLDEST -> filtered.sortedBy { it.dateMillis }
        SortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
        SortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amount }
    }

    // موجودی پایان ۶ ماه اخیر شمسی برای نمودار کارت قهرمان؛ بر اساس
    // همان حساب انتخاب‌شده (یا همه حساب‌ها) محاسبه می‌شود.
    val currentMonth = JalaliDate.currentJalaliYearMonth()
    val heroMonths = remember(accountTransactions, currentMonth) {
        JalaliDate.lastJalaliYearMonths(6, currentMonth)
    }
    val heroBalances = remember(accountTransactions, heroMonths) {
        heroMonths.map { month ->
            val key = ymKey(month)
            accountTransactions
                .filter { ymKey(JalaliDate.jalaliYearMonthOf(it.dateMillis)) <= key }
                .sumOf { if (it.income) it.amount else -it.amount }
        }
    }
    val monthTotals = remember(accountTransactions, currentMonth) {
        monthTotals(transactionsInMonth(accountTransactions, currentMonth))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.polino_logo),
                    contentDescription = "لوگوی پولینو",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("پولینو", style = MaterialTheme.typography.headlineSmall)
                    Text("نسخه ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { accountExpanded = true }) {
                        Text(
                            if (selectedAccountId == "all") "همه حساب‌ها"
                            else accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "اصلی"
                        )
                    }
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("همه حساب‌ها") },
                            onClick = {
                                selectedAccountId = "all"
                                accountExpanded = false
                            }
                        )
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
                if (selectedAccountId != "all") {
                    TextButton(onClick = { showRenameAccountDialog = true }) {
                        Text("تغییر نام حساب")
                    }
                }
                if (selectedAccountId != "main" && selectedAccountId != "all") {
                    TextButton(onClick = { showDeleteAccountConfirm = true }) {
                        Text("حذف حساب", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightHero)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "موجودی کل",
                        style = MaterialTheme.typography.labelLarge,
                        color = HeroMuted
                    )
                    Text(
                        money(income - expense),
                        style = MaterialTheme.typography.displaySmall,
                        color = HeroGold
                    )
                    BalanceChart(months = heroMonths, balances = heroBalances)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "درآمد این ماه",
                            style = MaterialTheme.typography.labelMedium,
                            color = HeroIncome
                        )
                        Text(
                            "+" + money(monthTotals.income),
                            style = MaterialTheme.typography.titleMedium,
                            color = HeroIncome
                        )
                        Text(
                            JalaliDate.jalaliMonthDisplay(currentMonth),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "هزینه این ماه",
                            style = MaterialTheme.typography.labelMedium,
                            color = HeroExpense
                        )
                        Text(
                            "−" + money(monthTotals.expense),
                            style = MaterialTheme.typography.titleMedium,
                            color = HeroExpense
                        )
                        Text(
                            JalaliDate.jalaliMonthDisplay(currentMonth),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
            Text("دسترسی سریع", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile(
                    modifier = Modifier.weight(1f),
                    label = "دسته‌ها",
                    icon = Icons.Default.Category,
                    onClick = { showCategoryDialog = true }
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    label = "تکرارشونده‌ها",
                    icon = Icons.Default.Repeat,
                    onClick = { showRecurringDialog = true }
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    label = "بودجه‌ها",
                    icon = Icons.Default.Savings,
                    onClick = onNavigateToReports
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    label = "اهداف",
                    icon = Icons.Default.Flag,
                    onClick = onNavigateToReports
                )
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
            var sortExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { sortExpanded = true }) {
                    Text(
                        when (sortOrder) {
                            SortOrder.NEWEST -> "مرتب‌سازی: جدیدترین"
                            SortOrder.OLDEST -> "مرتب‌سازی: قدیمی‌ترین"
                            SortOrder.AMOUNT_DESC -> "مرتب‌سازی: بیشترین مبلغ"
                            SortOrder.AMOUNT_ASC -> "مرتب‌سازی: کمترین مبلغ"
                        }
                    )
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("جدیدترین") },
                        onClick = { sortOrder = SortOrder.NEWEST; sortExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("قدیمی‌ترین") },
                        onClick = { sortOrder = SortOrder.OLDEST; sortExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("بیشترین مبلغ") },
                        onClick = { sortOrder = SortOrder.AMOUNT_DESC; sortExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("کمترین مبلغ") },
                        onClick = { sortOrder = SortOrder.AMOUNT_ASC; sortExpanded = false }
                    )
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
        item { Text("آخرین تراکنش‌ها", style = MaterialTheme.typography.titleLarge) }
        if (accountTransactions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("در این حساب هنوز تراکنشی ثبت نشده است.", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "با دکمه + پایین صفحه اولین تراکنش این حساب را ثبت کنید.",
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
                            if (selectedAccountId == "all") {
                                Text(
                                    accounts.firstOrNull { it.id == transaction.accountId }?.name ?: "اصلی",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
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
    }

    if (showDialog) {
        TransactionDialog(
            initial = editing,
            accounts = accounts,
            initialAccountId = editing?.accountId
                ?: if (selectedAccountId == "all") "main" else selectedAccountId,
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
            },
            onDuplicate = { source ->
                val copy = source.copy(id = UUID.randomUUID().toString())
                val ok = runCatching {
                    repository.upsert(copy)
                    transactions = repository.getAll()
                }.isSuccess
                if (ok) {
                    showDialog = false
                    editing = null
                } else {
                    error = "تکثیر اطلاعات ناموفق بود. دوباره تلاش کنید."
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
                if (categoryFilter == name) categoryFilter = null
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
private fun BalanceChart(months: List<String>, balances: List<Long>) {
    if (balances.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val min = balances.minOrNull() ?: 0L
            val max = balances.maxOrNull() ?: 0L
            val span = (max - min).takeIf { it > 0 } ?: 1L
            val stepX = if (balances.size > 1) size.width / (balances.size - 1) else 0f
            val points = balances.mapIndexed { index, value ->
                Offset(
                    x = stepX * index,
                    y = size.height - ((value - min).toFloat() / span) * (size.height - 16f) - 8f
                )
            }
            for (index in 0 until points.size - 1) {
                drawLine(HeroGold, points[index], points[index + 1], strokeWidth = 3f)
            }
            points.forEach { drawCircle(HeroGold, radius = 5f, center = it) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { month ->
                Text(
                    JalaliDate.jalaliMonthDisplay(month).substringBefore(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = HeroMuted
                )
            }
        }
    }
}

@Composable
private fun QuickTile(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label)
            Text(label, style = MaterialTheme.typography.bodySmall)
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
    onDelete: suspend (MoneyTransaction) -> Boolean,
    onDuplicate: suspend (MoneyTransaction) -> Boolean
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("حذف این تراکنش", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = {
                            scope.launch {
                                if (!onDuplicate(initial)) {
                                    validation = "تکثیر ناموفق بود. دوباره تلاش کنید."
                                }
                            }
                        }) { Text("تکثیر این تراکنش") }
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
