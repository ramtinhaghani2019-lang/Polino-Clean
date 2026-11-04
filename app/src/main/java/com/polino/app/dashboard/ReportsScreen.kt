package com.polino.app.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.polino.app.data.BackupData
import com.polino.app.data.fullBackupToJson
import com.polino.app.data.parseBackup
import com.polino.app.data.parseLegacyTransactions
import com.polino.app.data.nextJalaliYearMonth
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
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var pendingImportText by remember { mutableStateOf<String?>(null) }

    var selectedYearMonth by remember { mutableStateOf(JalaliDate.currentJalaliYearMonth()) }
    var period by remember { mutableStateOf(ReportPeriod.MONTHLY) }

    LaunchedEffect(Unit) {
        runCatching {
            transactionRepository.migrateIfNeeded()
            transactions = transactionRepository.getAll()
            accounts = transactionRepository.getAccounts()
            categories = transactionRepository.getCategories()
            goals = transactionRepository.getGoals()
        }.onFailure {
            error = "بارگذاری اطلاعات ناموفق بود. دوباره تلاش کنید."
        }
    }

    LaunchedEffect(selectedYearMonth) {
        budgets = budgetRepository.getBudgets(selectedYearMonth)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val data = BackupData(
                        transactions = transactionRepository.getAll(),
                        budgets = budgetRepository.getAllBudgets(),
                        categories = transactionRepository.getCategories(),
                        accounts = transactionRepository.getAccounts(),
                        recurring = transactionRepository.getRecurring(),
                        goals = transactionRepository.getGoals(),
                        accountSmsSenders = transactionRepository.getAccountSmsLinks()
                    )
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(fullBackupToJson(data).toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("no output stream")
                }.onSuccess { info = "پشتیبان‌گیری کامل انجام شد." }
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
                    val viewed = transactionsInMonth(transactions, selectedYearMonth).filter {
                        selectedAccountId == "all" || it.accountId == selectedAccountId
                    }
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(transactionsToCsv(viewed).toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("no output stream")
                    viewed.size
                }.onSuccess { count -> info = "خروجی CSV $count تراکنش انجام شد." }
                    .onFailure { error = "خروجی CSV ناموفق بود. دوباره تلاش کنید." }
            }
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val viewed = transactionsInMonth(transactions, selectedYearMonth).filter {
                        selectedAccountId == "all" || it.accountId == selectedAccountId
                    }
                    val totals = monthTotals(viewed)
                    val spent = spentByCategory(viewed)
                    val spentAll = spentByCategory(transactionsInMonth(transactions, selectedYearMonth))
                    val accountLabel = if (selectedAccountId == "all") "همه حساب‌ها"
                    else accounts.firstOrNull { it.id == selectedAccountId }?.name ?: "همه حساب‌ها"
                    val lines = buildList {
                        add("گزارش ماه ${JalaliDate.jalaliMonthDisplay(selectedYearMonth)} ($accountLabel)")
                        add("درآمد: ${money(totals.income)} تومان")
                        add("هزینه: ${money(totals.expense)} تومان")
                        add("مانده: ${money(totals.income - totals.expense)} تومان")
                        add("")
                        add("هزینه‌ها به تفکیک دسته:")
                        spent.forEach { (category, amount) ->
                            add("$category: ${money(amount)} تومان")
                        }
                        add("")
                        add("بودجه‌ها:")
                        budgets.forEach { budget ->
                            val spentAmount = spentAll.firstOrNull { it.first == budget.category }?.second ?: 0L
                            add("${budget.category}: ${money(spentAmount)} از ${money(budget.limitAmount)} تومان")
                        }
                    }
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        writePdfReport(stream, lines)
                    } ?: throw IllegalStateException("no output stream")
                }.onSuccess { info = "خروجی PDF انجام شد." }
                    .onFailure { error = "خروجی PDF ناموفق بود. دوباره تلاش کنید." }
            }
        }
    }

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val viewed = transactionsInMonth(transactions, selectedYearMonth).filter {
                        selectedAccountId == "all" || it.accountId == selectedAccountId
                    }
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(transactionsToXlsx(viewed))
                    } ?: throw IllegalStateException("no output stream")
                    viewed.size
                }.onSuccess { count -> info = "خروجی اکسل $count تراکنش انجام شد." }
                    .onFailure { error = "خروجی اکسل ناموفق بود. دوباره تلاش کنید." }
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
                if (parseBackup(text) == null && parseLegacyTransactions(text).isEmpty()) {
                    error = "فایلی برای بازیابی پیدا نشد یا خراب است."
                } else {
                    pendingImportText = text
                }
            }
        }
    }

    if (pendingImportText != null) {
        val text = pendingImportText!!
        val backup = parseBackup(text)
        AlertDialog(
            onDismissRequest = { pendingImportText = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
            title = { Text("بازیابی از فایل", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (backup != null) {
                        "بازیابی شود؟ ${backup.transactions.size} تراکنش، " +
                            "${backup.budgets.size} بودجه، ${backup.categories.size} دسته، " +
                            "${backup.accounts.size} حساب، ${backup.recurring.size} تکرارشونده و " +
                            "${backup.goals.size} هدف. داده‌های هم‌شناسه با نسخه فایل جایگزین می‌شوند."
                    } else {
                        val imported = parseLegacyTransactions(text)
                        "بازیابی شود؟ ${imported.size} تراکنش. تراکنش‌های هم‌شناسه جایگزین می‌شوند."
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val confirmedText = pendingImportText
                    pendingImportText = null
                    if (confirmedText != null) {
                        scope.launch {
                            val confirmedBackup = parseBackup(confirmedText)
                            if (confirmedBackup != null) {
                                runCatching {
                                    confirmedBackup.accounts.forEach { transactionRepository.upsertAccount(it) }
                                    confirmedBackup.accountSmsSenders.forEach {
                                        transactionRepository.upsertAccountSmsSender(it)
                                    }
                                    confirmedBackup.transactions.forEach { transactionRepository.upsert(it) }
                                    confirmedBackup.categories.forEach {
                                        transactionRepository.addCategory(it.name, it.isIncome)
                                    }
                                    confirmedBackup.recurring.forEach { transactionRepository.addRecurring(it) }
                                    confirmedBackup.goals.forEach { transactionRepository.addGoal(it) }
                                    confirmedBackup.budgets.forEach {
                                        budgetRepository.setBudget(it.yearMonth, it.category, it.limitAmount)
                                    }
                                    transactions = transactionRepository.getAll()
                                    accounts = transactionRepository.getAccounts()
                                    categories = transactionRepository.getCategories()
                                    goals = transactionRepository.getGoals()
                                    budgets = budgetRepository.getBudgets(selectedYearMonth)
                                }.onSuccess {
                                    info = "بازیابی کامل انجام شد: ${confirmedBackup.transactions.size} تراکنش، " +
                                        "${confirmedBackup.budgets.size} بودجه، ${confirmedBackup.categories.size} دسته، " +
                                        "${confirmedBackup.accounts.size} حساب، ${confirmedBackup.recurring.size} تکرارشونده، " +
                                        "${confirmedBackup.goals.size} هدف."
                                }.onFailure { error = "بازیابی ناموفق بود. دوباره تلاش کنید." }
                            } else {
                                val imported = parseLegacyTransactions(confirmedText)
                                runCatching {
                                    imported.forEach { transactionRepository.upsert(it) }
                                    transactions = transactionRepository.getAll()
                                }.onSuccess { info = "${imported.size} تراکنش بازیابی شد." }
                                    .onFailure { error = "بازیابی ناموفق بود. دوباره تلاش کنید." }
                            }
                        }
                    }
                }) { Text("بازیابی") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportText = null }) { Text("انصراف") }
            }
        )
    }

    val monthTransactionsAll = transactionsInMonth(transactions, selectedYearMonth)
    val monthTransactions = monthTransactionsAll.filter {
        selectedAccountId == "all" || it.accountId == selectedAccountId
    }
    /** پیشرفت بودجه همیشه نسبت به کل ماه سنجیده می‌شود، نه حساب فیلترشده. */
    val spentAll = spentByCategory(monthTransactionsAll)

    val nowMillis = System.currentTimeMillis()
    val periodTransactions = transactionsInPeriod(
        transactions, period, nowMillis, selectedYearMonth
    ).filter { selectedAccountId == "all" || it.accountId == selectedAccountId }
    val periodTotals = monthTotals(periodTransactions)
    val periodSpent = spentByCategory(periodTransactions)
    val periodTitle = when (period) {
        ReportPeriod.DAILY -> "امروز ${JalaliDate.formatJalali(nowMillis)}"
        ReportPeriod.WEEKLY -> {
            val start = jalaliWeekStartMillis(nowMillis)
            "هفته ${JalaliDate.formatJalali(start)} تا " +
                JalaliDate.formatJalali(start + 6L * 24 * 60 * 60 * 1000)
        }
        ReportPeriod.MONTHLY -> "ماه ${JalaliDate.jalaliMonthDisplay(selectedYearMonth)}"
        ReportPeriod.YEARLY -> "سال ${jalaliYearOf(nowMillis)}"
    }

    val previousMonthTransactions = transactionsInMonth(
        transactions, JalaliDate.previousJalaliYearMonth(selectedYearMonth)
    ).filter { selectedAccountId == "all" || it.accountId == selectedAccountId }
    val radarInsights = spendingInsights(monthTransactions, previousMonthTransactions)

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
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriod.entries.forEach { entry ->
                        FilterChip(
                            selected = period == entry,
                            onClick = { period = entry },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            label = {
                                Text(
                                    when (entry) {
                                        ReportPeriod.DAILY -> "روزانه"
                                        ReportPeriod.WEEKLY -> "هفتگی"
                                        ReportPeriod.MONTHLY -> "ماهانه"
                                        ReportPeriod.YEARLY -> "سالانه"
                                    }
                                )
                            }
                        )
                    }
                }
            }
            item {
                if (period == ReportPeriod.MONTHLY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = {
                            selectedYearMonth = JalaliDate.previousJalaliYearMonth(selectedYearMonth)
                        }) { Text("ماه قبل") }
                        Text(
                            JalaliDate.jalaliMonthDisplay(selectedYearMonth),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(
                            onClick = {
                                selectedYearMonth = nextJalaliYearMonth(selectedYearMonth)
                            },
                            enabled = selectedYearMonth != JalaliDate.currentJalaliYearMonth()
                        ) { Text("ماه بعد") }
                    }
                } else {
                    Text(periodTitle, style = MaterialTheme.typography.titleMedium)
                }
            }
            item {
                // تم تیره: کارت تیره با بوردر بنفش؛ تم روشن: کارت روشن با بوردر بنفش.
                val isDarkSummary = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val summaryContainer = if (isDarkSummary) ReportDarkCard else MaterialTheme.colorScheme.surface
                val summaryBorder = if (isDarkSummary) ReportDarkBorder else MaterialTheme.colorScheme.outline
                val summaryLabel = if (isDarkSummary) ReportDarkMuted else MaterialTheme.colorScheme.onSurfaceVariant
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = summaryContainer),
                    border = BorderStroke(1.dp, summaryBorder)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "گزارش $periodTitle",
                            style = MaterialTheme.typography.titleMedium,
                            color = summaryLabel
                        )
                        Text(
                            "درآمد: ${money(periodTotals.income)}",
                            color = IncomeGreen
                        )
                        Text(
                            "هزینه: ${money(periodTotals.expense)}",
                            color = ExpenseRed
                        )
                        Text(
                            "مانده: ${money(periodTotals.income - periodTotals.expense)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (periodTotals.income - periodTotals.expense >= 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
            item { Text("هزینه‌ها به تفکیک دسته", style = MaterialTheme.typography.titleMedium) }
            val maxSpent = periodSpent.maxOfOrNull { it.second } ?: 1L
            if (periodSpent.isEmpty()) {
                item { Text("در این بازه هزینه‌ای ثبت نشده است.") }
            } else {
                periodSpent.forEach { (category, amount) ->
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
            item { Text("رادار مالی", style = MaterialTheme.typography.titleMedium) }
            if (radarInsights.isEmpty()) {
                item { Text("هنوز داده‌ای برای تحلیل رادار نیست.") }
            } else {
                radarInsights.forEach { insight ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                insight.text,
                                modifier = Modifier.padding(16.dp),
                                color = when (insight.kind) {
                                    RadarKind.UP -> MaterialTheme.colorScheme.error
                                    RadarKind.DOWN -> MaterialTheme.colorScheme.primary
                                    RadarKind.NEW -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                    }
                }
            }
            item { Text("روند ۶ ماه اخیر", style = MaterialTheme.typography.titleMedium) }
            val trendBase = if (selectedAccountId == "all") transactions
            else transactions.filter { it.accountId == selectedAccountId }
            val trend = monthlyTrend(trendBase, JalaliDate.lastJalaliYearMonths(6, selectedYearMonth))
            val maxTrend = (trend.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L)
                .takeIf { it > 0 } ?: 1L
            item {
                CashFlowBarChartCard(months = trend, maxValue = maxTrend)
            }
            item { Text("بودجه‌های ماه ${JalaliDate.jalaliMonthDisplay(selectedYearMonth)}", style = MaterialTheme.typography.titleMedium) }
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
                val accountName = accounts.firstOrNull { it.id == goal.accountId }?.name ?: "اصلی"
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
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { goalToEdit = goal }) { Text("ویرایش") }
                                    IconButton(onClick = { goalToDelete = goal }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف هدف")
                                    }
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { csvExportLauncher.launch("polino-transactions.csv") }) {
                        Text("خروجی CSV (اکسل)")
                    }
                    Button(onClick = { pdfExportLauncher.launch("polino-report.pdf") }) {
                        Text("خروجی PDF گزارش ماه")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { xlsxExportLauncher.launch("polino-transactions.xlsx") }) {
                        Text("خروجی اکسل (.xlsx)")
                    }
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
                    budgetRepository.setBudget(selectedYearMonth, category, limit)
                    budgets = budgetRepository.getBudgets(selectedYearMonth)
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
    if (goalToEdit != null) {
        val editingGoal = goalToEdit!!
        GoalDialog(
            initial = editingGoal,
            accounts = accounts,
            onDismiss = { goalToEdit = null },
            onSave = { name, target, accountId ->
                scope.launch {
                    transactionRepository.addGoal(
                        editingGoal.copy(name = name, target = target, accountId = accountId)
                    )
                    goals = transactionRepository.getGoals()
                    goalToEdit = null
                }
            }
        )
    }
    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
            title = { Text("حذف هدف", fontWeight = FontWeight.Bold) },
            text = { Text("هدف «${goalToDelete!!.name}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    val target = goalToDelete
                    goalToDelete = null
                    scope.launch {
                        if (target != null) {
                            transactionRepository.deleteGoal(target.id)
                            goals = transactionRepository.getGoals()
                        }
                    }
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun GoalDialog(
    accounts: List<AccountEntity>,
    initial: GoalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Long, accountId: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.target?.toString() ?: "") }
    var accountId by remember { mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id ?: "main") }
    var accountExpanded by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text(if (initial == null) "هدف پس‌انداز" else "ویرایش هدف", fontWeight = FontWeight.Bold) },
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
            Button(onClick = {
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
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("تعیین بودجه", fontWeight = FontWeight.Bold) },
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
            Button(onClick = {
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

private val ReportDarkCard = Color(0xFF1E1438)
private val ReportDarkBorder = Color(0xFF3D3368)
private val ReportDarkMuted = Color(0xFFB3ACD6)
private val IncomeGreen = Color(0xFF34C89E)
private val ExpenseRed = Color(0xFFFF6B6B)

@Composable
private fun CashFlowBarChartCard(months: List<MonthTrend>, maxValue: Long) {
    var selectedIndex by remember { mutableStateOf(months.indices.lastOrNull() ?: -1) }
    val isDarkChart = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val chartContainer = if (isDarkChart) ReportDarkCard else MaterialTheme.colorScheme.surface
    val chartBorder = if (isDarkChart) ReportDarkBorder else MaterialTheme.colorScheme.outline
    val chartLabel = if (isDarkChart) ReportDarkMuted else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = chartContainer),
        border = BorderStroke(1.dp, chartBorder)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "نمودار جریان مالی",
                style = MaterialTheme.typography.titleMedium,
                color = chartLabel
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                months.forEachIndexed { index, month ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = index },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            val barWidth = size.width * 0.28f
                            val incomeHeight =
                                if (maxValue > 0) (month.income.toFloat() / maxValue.toFloat()) * size.height else 0f
                            val expenseHeight =
                                if (maxValue > 0) (month.expense.toFloat() / maxValue.toFloat()) * size.height else 0f
                            drawRect(
                                color = IncomeGreen,
                                topLeft = Offset(size.width * 0.12f, size.height - incomeHeight),
                                size = Size(barWidth, incomeHeight)
                            )
                            drawRect(
                                color = ExpenseRed,
                                topLeft = Offset(size.width * 0.60f, size.height - expenseHeight),
                                size = Size(barWidth, expenseHeight)
                            )
                        }
                        Text(
                            JalaliDate.jalaliMonthDisplay(month.yearMonth).substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = chartLabel,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) { drawRect(color = IncomeGreen) }
                    Text("درآمد", style = MaterialTheme.typography.labelSmall, color = chartLabel)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) { drawRect(color = ExpenseRed) }
                    Text("هزینه", style = MaterialTheme.typography.labelSmall, color = chartLabel)
                }
            }
            val selected = months.getOrNull(selectedIndex)
            if (selected != null) {
                Text(
                    "${JalaliDate.jalaliMonthDisplay(selected.yearMonth)}: " +
                        "درآمد ${money(selected.income)} · هزینه ${money(selected.expense)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
