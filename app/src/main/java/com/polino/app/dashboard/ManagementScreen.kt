package com.polino.app.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Switch
import androidx.core.content.ContextCompat
import com.polino.app.sms.SMS_ENABLED_KEY
import com.polino.app.sms.SMS_PREFS_NAME
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.polino.app.data.AccountEntity
import com.polino.app.data.BudgetRepository
import com.polino.app.data.CategoryEntity
import com.polino.app.data.CategoryType
import com.polino.app.data.categoryType
import com.polino.app.data.categoryTypeLabel
import com.polino.app.data.mainCategories
import com.polino.app.data.subcategoriesOf
import com.polino.app.data.GoalEntity
import com.polino.app.data.InstallmentEntity
import com.polino.app.data.DebtEntity
import com.polino.app.data.TaskEntity
import com.polino.app.data.ChequeReminderEntity
import com.polino.app.data.isChequeReminderOverdue
import com.polino.app.data.isDebtOverdue
import com.polino.app.data.openChequeRemindersTotal
import com.polino.app.data.AssetEntity
import com.polino.app.data.SmsPatternEntity
import com.polino.app.data.assetCurrentValue
import com.polino.app.data.isValidAssetInput
import com.polino.app.data.portfolioTotals
import com.polino.app.data.isTaskOverdue
import com.polino.app.data.unsettledTotals
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.dongiShares
import com.polino.app.data.DongiMember
import com.polino.app.data.dongiSettlements
import com.polino.app.data.installmentMonthlyAmount
import com.polino.app.data.installmentProgressFraction
import com.polino.app.data.RecurringEntity
import java.util.UUID
import kotlinx.coroutines.launch
import com.polino.app.data.TransactionRepository

@Composable
fun ManagementScreen(
    onNavigateHome: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSmartBudget: () -> Unit = {},
    themeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    lockEnabled: Boolean = false,
    onLockChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val budgetRepository = remember { BudgetRepository(context) }
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var recurring by remember { mutableStateOf<List<RecurringEntity>>(emptyList()) }
    var goals by remember { mutableStateOf<List<GoalEntity>>(emptyList()) }
    var budgetCount by remember { mutableStateOf(0) }
    var installments by remember { mutableStateOf<List<InstallmentEntity>>(emptyList()) }
    var debts by remember { mutableStateOf<List<DebtEntity>>(emptyList()) }
    var tasks by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }
    var cheques by remember { mutableStateOf<List<ChequeReminderEntity>>(emptyList()) }
    var assets by remember { mutableStateOf<List<AssetEntity>>(emptyList()) }
    var patterns by remember { mutableStateOf<List<SmsPatternEntity>>(emptyList()) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var showInstallmentDialog by remember { mutableStateOf(false) }
    var showDebtDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showChequeDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var renameAccountTarget by remember { mutableStateOf<AccountEntity?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showAssetDialog by remember { mutableStateOf(false) }
    var renameCategoryTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedCategoryType by remember { mutableStateOf(CategoryType.EXPENSE) }
    var categoryMessage by remember { mutableStateOf("") }
    var subcategoryParent by remember { mutableStateOf<CategoryEntity?>(null) }
    val scope = rememberCoroutineScope()
    var smsEnabled by remember { mutableStateOf(false) }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            context.getSharedPreferences(SMS_PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(SMS_ENABLED_KEY, true).apply()
            smsEnabled = true
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            transactions = repository.getAll()
            accounts = repository.getAccounts()
            categories = repository.getCategories()
            recurring = repository.getRecurring()
            goals = repository.getGoals()
            installments = repository.getInstallments()
            debts = repository.getDebts()
            tasks = repository.getTasks()
            cheques = repository.getCheques()
            assets = repository.getAssets()
            patterns = repository.getPatterns()
            budgetCount = budgetRepository.getBudgets(JalaliDate.currentJalaliYearMonth()).size
        }
        smsEnabled = context.getSharedPreferences(SMS_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SMS_ENABLED_KEY, false)
    }

    val totalBalance = transactions.sumOf { if (it.income) it.amount else -it.amount }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("مدیریت مالی", style = MaterialTheme.typography.headlineSmall)
            Text("ابزارهای یکپارچه مالی", style = MaterialTheme.typography.labelMedium)
        }
        item {
            val isDarkHub = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val hubContainer = if (isDarkHub) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface
            val hubBorder = if (isDarkHub) Color(0xFF3D3368) else MaterialTheme.colorScheme.outline
            val hubLabel = if (isDarkHub) Color(0xFFB3ACD6) else MaterialTheme.colorScheme.onSurfaceVariant
            val hubNumber = if (isDarkHub) Color.White else MaterialTheme.colorScheme.onSurface
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = hubContainer),
                border = BorderStroke(1.dp, hubBorder)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "خلاصه دارایی",
                        style = MaterialTheme.typography.labelLarge,
                        color = hubLabel
                    )
                    Text("مجموع مانده نقدی", color = hubNumber)
                    Text(
                        money(totalBalance),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = hubNumber
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "اهداف پس‌انداز",
                    value = goals.size.toString()
                )
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "تکرارشونده‌ها",
                    value = recurring.size.toString()
                )
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "بودجه‌های این ماه",
                    value = budgetCount.toString()
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("تم برنامه", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "سیستم", "light" to "روشن", "dark" to "تیره")
                            .forEach { (mode, label) ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    label = { Text(label) }
                                )
                            }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("قفل برنامه", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = lockEnabled,
                            onCheckedChange = onLockChange
                        )
                    }
                    Text(
                        "با اثر انگشت یا رمز دستگاه باز می‌شود؛ از اجرای بعد فعال است.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item { Text("بخش‌ها", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = "خوانش پیامک")
                    Column(Modifier.weight(1f)) {
                        Text("خوانش پیامک", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (smsEnabled) "فعال — تراکنش‌های بانکی به‌صورت خودکار ثبت می‌شوند"
                            else "خاموش — برای ثبت خودکار، دسترسی خواندن پیامک لازم است",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = smsEnabled,
                        onCheckedChange = { checked ->
                            val prefs = context.getSharedPreferences(
                                SMS_PREFS_NAME, Context.MODE_PRIVATE
                            )
                            if (checked) {
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECEIVE_SMS
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    prefs.edit().putBoolean(SMS_ENABLED_KEY, true).apply()
                                    smsEnabled = true
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                                }
                            } else {
                                prefs.edit().putBoolean(SMS_ENABLED_KEY, false).apply()
                                smsEnabled = false
                            }
                        }
                    )
                }
            }
        }
        item { Text("مدیریت الگوهای پیامک", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            Text(
                "تعیین فرستنده‌ها، کلمات کلیدی و قوانین تراکنش",
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            Button(onClick = { showPatternDialog = true }) {
                Text("افزودن الگوی جدید")
            }
        }
        if (patterns.isEmpty()) {
            item { Text("الگوی سفارشی هنوز ثبت نشده است.") }
        } else {
            patterns.forEach { pattern ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(pattern.sender, style = MaterialTheme.typography.titleMedium)
                            if (pattern.keywords.isNotBlank()) {
                                Text("کلمات کلیدی: ${pattern.keywords}")
                            }
                            Text(if (pattern.isIncome) "درآمد" else "هزینه")
                            if (!pattern.isFinancial) {
                                Text("غیرمالی — ثبت خودکار نمی‌شود")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deletePattern(pattern.id)
                                            patterns = repository.getPatterns()
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            ManagementCard(
                title = "حساب‌ها و کارت‌ها",
                subtitle = "${accounts.size} حساب",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onNavigateHome
            )
        }
        item {
            ManagementCard(
                title = "دسته‌ها",
                subtitle = "${categories.size} دسته",
                icon = Icons.Default.Category,
                onClick = onNavigateHome
            )
        }
        item {
            ManagementCard(
                title = "تکرارشونده‌ها",
                subtitle = "${recurring.size} قانون",
                icon = Icons.Default.Repeat,
                onClick = onNavigateHome
            )
        }
        item {
            ManagementCard(
                title = "بودجه‌ها و اهداف",
                subtitle = "$budgetCount بودجه، ${goals.size} هدف",
                icon = Icons.Default.Savings,
                onClick = onNavigateToReports
            )
        }
        item {
            ManagementCard(
                title = "بودجه‌بندی هوشمند",
                subtitle = "$budgetCount بودجه ماه جاری",
                icon = Icons.Default.PieChart,
                onClick = onNavigateToSmartBudget
            )
        }
        item {
            ManagementCard(
                title = "گزارش‌ها",
                subtitle = "گزارش ماه و روند مالی",
                icon = Icons.Default.BarChart,
                onClick = onNavigateToReports
            )
        }
        item { Text("اقساط", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            Button(onClick = { showInstallmentDialog = true }) {
                Text("قسط جدید")
            }
        }
        if (installments.isEmpty()) {
            item { Text("هنوز قسطی ثبت نشده است.") }
        } else {
            installments.forEach { installment ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(installment.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "ماهانه: ${money(installmentMonthlyAmount(installment.totalAmount, installment.monthCount))} — " +
                                    "پرداخت‌شده: ${installment.paidCount} از ${installment.monthCount}"
                            )
                            LinearProgressIndicator(
                                progress = {
                                    installmentProgressFraction(
                                        installment.paidCount, installment.monthCount
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.payInstallment(installment)
                                            installments = repository.getInstallments()
                                        }
                                    },
                                    enabled = installment.paidCount < installment.monthCount
                                ) {
                                    Text(
                                        if (installment.paidCount >= installment.monthCount) {
                                            "تمام شد"
                                        } else {
                                            "پرداخت قسط"
                                        }
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteInstallment(installment.id)
                                            installments = repository.getInstallments()
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("یادآور چک و قبض", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            Text("مبلغ سررسید باز: ${money(openChequeRemindersTotal(cheques))}")
        }
        item {
            Button(onClick = { showChequeDialog = true }) {
                Text("ثبت یادآور جدید")
            }
        }
        if (cheques.isEmpty()) {
            item { Text("هنوز یادآوری ثبت نشده است.") }
        } else {
            cheques.forEach { cheque ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val overdue = isChequeReminderOverdue(
                                cheque.dueMillis, System.currentTimeMillis(), cheque.settled
                            )
                            Text(
                                cheque.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (overdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                "مبلغ: ${money(cheque.amount)} — " +
                                    "سررسید: ${JalaliDate.formatJalali(cheque.dueMillis)}" +
                                    if (overdue) " — سررسید گذشته" else "" +
                                    if (cheque.settled) " — پرداخت شد" else "" +
                                    if (cheque.note.isNotBlank()) " — ${cheque.note}" else ""
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.settleCheque(cheque)
                                            cheques = repository.getCheques()
                                        }
                                    },
                                    enabled = !cheque.settled
                                ) {
                                    Text(if (cheque.settled) "پرداخت شد" else "پرداخت")
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteCheque(cheque.id)
                                            cheques = repository.getCheques()
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("طلب و بدهی", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            val (receivableTotal, payableTotal) = unsettledTotals(debts)
            Text("طلب‌های باز: ${money(receivableTotal)} — بدهی‌های باز: ${money(payableTotal)}")
        }
        item {
            Button(onClick = { showDebtDialog = true }) {
                Text("طلب یا بدهی جدید")
            }
        }
        if (debts.isEmpty()) {
            item { Text("هنوز طلب یا بدهی ثبت نشده است.") }
        } else {
            debts.forEach { debt ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val overdue = isDebtOverdue(
                                debt.dueMillis, System.currentTimeMillis(), debt.settled
                            )
                            Text(
                                debt.personName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (overdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                "${if (debt.isReceivable) "طلب" else "بدهی"}: ${money(debt.amount)}" +
                                    if (debt.dueMillis != null) " — سررسید: ${JalaliDate.formatJalali(debt.dueMillis)}" else "" +
                                    if (overdue) " — سررسید گذشته" else "" +
                                    if (debt.settled) " — تسویه شد" else ""
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.settleDebt(debt)
                                            debts = repository.getDebts()
                                        }
                                    },
                                    enabled = !debt.settled
                                ) {
                                    Text(if (debt.settled) "تسویه شد" else "تسویه")
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteDebt(debt.id)
                                            debts = repository.getDebts()
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("کارها و یادآورها", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            Button(onClick = { showTaskDialog = true }) {
                Text("کار جدید")
            }
        }
        if (tasks.isEmpty()) {
            item { Text("هنوز کاری ثبت نشده است.") }
        } else {
            tasks.forEach { task ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val overdue = isTaskOverdue(
                                task.dueMillis, System.currentTimeMillis(), task.done
                            )
                            Text(
                                task.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (overdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                "سررسید: ${JalaliDate.formatJalali(task.dueMillis)}" +
                                    if (overdue) " — سررسید گذشته" else "" +
                                    if (task.done) " — انجام شد" else ""
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.toggleTaskDone(task)
                                            tasks = repository.getTasks()
                                        }
                                    }
                                ) {
                                    Text(if (task.done) "باز کردن" else "انجام شد")
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteTask(task.id)
                                            tasks = repository.getTasks()
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("محاسبه دنگی", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            var dongiMembers by remember {
                mutableStateOf(
                    listOf(DongiMember("من", 0L), DongiMember("نفر ۲", 0L))
                )
            }
            var dongiSaved by remember { mutableStateOf(false) }
            val dongiTotal = dongiMembers.sumOf { it.paid.coerceAtLeast(0L) }
            val dongiSharesList = dongiShares(dongiTotal, dongiMembers.size)
            val dongiSettlementsList = dongiSettlements(dongiMembers)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "مبلغی که هر نفر پرداخته را وارد کنید تا سهم مساوی و تسویه‌ها حساب شود؛ باقیمانده یکی‌یکی به نفرات اول می‌رسد.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "جمع کل هزینه‌ها: ${money(dongiTotal)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (dongiSharesList.isEmpty()) {
                            "${dongiMembers.size} نفر"
                        } else {
                            val minShare = dongiSharesList.min()
                            val maxShare = dongiSharesList.max()
                            if (minShare == maxShare) {
                                "${dongiMembers.size} نفر · سهم مساوی هر نفر: ${money(minShare)}"
                            } else {
                                "${dongiMembers.size} نفر · سهم مساوی هر نفر: ${money(minShare)} تا ${money(maxShare)}"
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "تعداد تسویه‌ها: ${dongiSettlementsList.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("اعضای گروه و مبالغ پرداختی", style = MaterialTheme.typography.titleMedium)
                    dongiMembers.forEachIndexed { index, member ->
                        val share = dongiSharesList.getOrNull(index) ?: 0L
                        val balance = member.paid.coerceAtLeast(0L) - share
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextField(
                                        value = member.name,
                                        onValueChange = { newName ->
                                            dongiMembers = dongiMembers.toMutableList().also {
                                                it[index] = member.copy(name = newName)
                                            }
                                            dongiSaved = false
                                        },
                                        label = { Text("نام شخص") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            dongiMembers = dongiMembers.filterIndexed { i, _ -> i != index }
                                            dongiSaved = false
                                        }
                                    ) { Text("حذف") }
                                }
                                TextField(
                                    value = if (member.paid == 0L) "" else member.paid.toString(),
                                    onValueChange = { raw ->
                                        val value = normalizeDigits(raw)
                                            .filter { it.isDigit() }
                                            .toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                                        dongiMembers = dongiMembers.toMutableList().also {
                                            it[index] = member.copy(paid = value)
                                        }
                                        dongiSaved = false
                                    },
                                    label = { Text("مبلغ پرداختی (تومان)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        50_000L to "+۵۰ هزار",
                                        100_000L to "+۱۰۰ هزار",
                                        500_000L to "+۵۰۰ هزار"
                                    ).forEach { (quick, label) ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                dongiMembers = dongiMembers.toMutableList().also {
                                                    it[index] = member.copy(paid = member.paid + quick)
                                                }
                                                dongiSaved = false
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                                Text("سهم: ${money(share)}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    when {
                                        member.settled -> "تسویه شده"
                                        balance > 0 -> "باید بگیرد: ${money(balance)}"
                                        balance < 0 -> "باید بدهد: ${money(-balance)}"
                                        else -> "متوازن"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("تسویه شده")
                                    Switch(
                                        checked = member.settled,
                                        onCheckedChange = { settled ->
                                            dongiMembers = dongiMembers.toMutableList().also {
                                                it[index] = member.copy(settled = settled)
                                            }
                                            dongiSaved = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            dongiMembers = dongiMembers + DongiMember("نفر ${dongiMembers.size + 1}", 0L)
                            dongiSaved = false
                        }
                    ) { Text("افزودن شخص") }
                    Text("تسویه‌حساب و پرداخت‌ها", style = MaterialTheme.typography.titleMedium)
                    if (dongiSettlementsList.isEmpty()) {
                        Text("تمام حساب‌ها متوازن است و هیچ پرداختی لازم نیست.")
                    } else {
                        dongiSettlementsList.forEach { settlement ->
                            Text("${settlement.from} به ${settlement.to}: ${money(settlement.amount)}")
                        }
                    }
                    if (dongiMembers.isNotEmpty() && dongiSharesList.isNotEmpty()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.upsert(
                                        MoneyTransaction(
                                            id = UUID.randomUUID().toString(),
                                            title = "سهم دنگی",
                                            amount = dongiSharesList.first(),
                                            income = false,
                                            category = "سایر",
                                            dateMillis = System.currentTimeMillis(),
                                            accountId = "main",
                                            note = "محاسبه دنگی: ${money(dongiTotal)} بین ${dongiMembers.size} نفر"
                                        )
                                    )
                                    dongiSaved = true
                                }
                            }
                        ) {
                            Text("ثبت سهم من به‌عنوان هزینه")
                        }
                    }
                    if (dongiSaved) {
                        Text("سهم شما ثبت شد.", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { Text("مدیریت حساب‌ها", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            Button(onClick = { showAccountDialog = true }) {
                Text("حساب جدید")
            }
        }
        accounts.forEach { account ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        val balance = transactions.filter { it.accountId == account.id }
                            .sumOf { if (it.income) it.amount else -it.amount }
                        Text("مانده: ${money(balance)}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { renameAccountTarget = account }) {
                                Text("تغییر نام")
                            }
                            if (account.id != "main") {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteAccount(account.id)
                                            accounts = repository.getAccounts()
                                            transactions = repository.getAll()
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("مدیریت دسته‌ها", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryType.entries.forEach { type ->
                    val count = mainCategories(categories, type).size
                    FilterChip(
                        selected = selectedCategoryType == type,
                        onClick = { selectedCategoryType = type },
                        label = { Text("${categoryTypeLabel(type)} ($count)") }
                    )
                }
            }
        }
        item {
            Button(onClick = { showCategoryDialog = true }) {
                Text("دسته جدید")
            }
        }
        if (categoryMessage.isNotEmpty()) {
            item {
                Text(categoryMessage, color = MaterialTheme.colorScheme.error)
            }
        }
        mainCategories(categories, selectedCategoryType).forEach { category ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                categoryIcon(category.name),
                                contentDescription = category.name
                            )
                            Column {
                                Text(category.name, style = MaterialTheme.typography.titleMedium)
                                Text("دسته اصلی · ${categoryTypeLabel(categoryType(category))}")
                            }
                        }
                        subcategoriesOf(categories, category.name).forEach { sub ->
                            Row(
                                modifier = Modifier.padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(sub.name, modifier = Modifier.weight(1f))
                                TextButton(onClick = { renameCategoryTarget = sub }) {
                                    Text("تغییر نام")
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteCategory(sub.name)
                                            categories = repository.getCategories()
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { subcategoryParent = category }) {
                                Text("زیردسته جدید")
                            }
                            if (category.name != "سایر") {
                                TextButton(onClick = { renameCategoryTarget = category }) {
                                    Text("تغییر نام")
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            if (subcategoriesOf(categories, category.name).isNotEmpty()) {
                                                categoryMessage = "اول زیردسته‌های این دسته را حذف کنید."
                                            } else if (repository.deleteCategory(category.name)) {
                                                categoryMessage = ""
                                                categories = repository.getCategories()
                                            }
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("سرمایه‌گذاری و دارایی‌ها", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            val totals = portfolioTotals(assets)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ارزش کل پرتفوی: ${money(totals.totalValue.toLong())}")
                    Text("دارایی‌های فعال: ${assets.size}")
                    Text("ارزش روز: ${money(totals.totalValue.toLong())}")
                    val profitLoss = totals.profitLoss
                    val percent = Math.round(totals.profitLossPercent * 10) / 10.0
                    Text(
                        "سود/زیان: ${money(profitLoss.toLong())} ($percent٪)",
                        color = if (profitLoss >= 0) {
                            Color(0xFF2E7D32)
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
        item {
            Button(onClick = { showAssetDialog = true }) {
                Text("افزودن دارایی جدید")
            }
        }
        if (assets.isEmpty()) {
            item { Text("هنوز دارایی ثبت نشده است.") }
        } else {
            assets.forEach { asset ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(asset.name, style = MaterialTheme.typography.titleMedium)
                            Text("${asset.type} — ${formatAssetQuantity(asset.quantity)} ${asset.unit}")
                            Text("ارزش روز: ${money(assetCurrentValue(asset).toLong())}")
                            val assetProfitLoss =
                                assetCurrentValue(asset) - asset.quantity * asset.buyPrice
                            Text(
                                "سود/زیان: ${money(assetProfitLoss.toLong())}",
                                color = if (assetProfitLoss >= 0) {
                                    Color(0xFF2E7D32)
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            if (asset.note.isNotBlank()) {
                                Text(asset.note, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteAsset(asset.id)
                                            assets = repository.getAssets()
                                        }
                                    }
                                ) {
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInstallmentDialog) {
        InstallmentDialog(
            accounts = accounts,
            onDismiss = { showInstallmentDialog = false },
            onSave = { title, total, months, accountId ->
                scope.launch {
                    repository.addInstallment(
                        InstallmentEntity(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            totalAmount = total,
                            monthCount = months,
                            paidCount = 0,
                            accountId = accountId
                        )
                    )
                    installments = repository.getInstallments()
                    showInstallmentDialog = false
                }
            }
        )
    }

    if (showDebtDialog) {
        DebtDialog(
            accounts = accounts,
            onDismiss = { showDebtDialog = false },
            onSave = { person, amount, isReceivable, accountId, dueMillis ->
                scope.launch {
                    repository.addDebt(
                        DebtEntity(
                            id = UUID.randomUUID().toString(),
                            personName = person,
                            amount = amount,
                            isReceivable = isReceivable,
                            settled = false,
                            accountId = accountId,
                            dueMillis = dueMillis
                        )
                    )
                    debts = repository.getDebts()
                    showDebtDialog = false
                }
            }
        )
    }

    if (showTaskDialog) {
        TaskDialog(
            onDismiss = { showTaskDialog = false },
            onSave = { title, dueMillis ->
                scope.launch {
                    repository.addTask(
                        TaskEntity(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            dueMillis = dueMillis,
                            done = false
                        )
                    )
                    tasks = repository.getTasks()
                    showTaskDialog = false
                }
            }
        )
    }

    if (showChequeDialog) {
        ChequeReminderDialog(
            onDismiss = { showChequeDialog = false },
            onSave = { title, amount, dueMillis, note ->
                scope.launch {
                    repository.addCheque(
                        ChequeReminderEntity(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            amount = amount,
                            dueMillis = dueMillis,
                            settled = false,
                            note = note
                        )
                    )
                    cheques = repository.getCheques()
                    showChequeDialog = false
                }
            }
        )
    }

    if (showAccountDialog) {
        AccountDialog(
            existingNames = accounts.map { it.name },
            onDismiss = { showAccountDialog = false },
            onSave = { name ->
                scope.launch {
                    repository.addAccount(name)
                    accounts = repository.getAccounts()
                    showAccountDialog = false
                }
            }
        )
    }

    val accountToRename = renameAccountTarget
    if (accountToRename != null) {
        AccountRenameDialog(
            currentName = accountToRename.name,
            existingNames = accounts.map { it.name },
            onDismiss = { renameAccountTarget = null },
            onSave = { name ->
                scope.launch {
                    if (repository.renameAccount(accountToRename.id, name)) {
                        accounts = repository.getAccounts()
                        renameAccountTarget = null
                    }
                }
            }
        )
    }

    if (showCategoryDialog) {
        CategoryDialog(
            existingNames = categories.map { it.name },
            initialType = selectedCategoryType,
            onDismiss = { showCategoryDialog = false },
            onSave = { name, isIncome, isInvestment ->
                scope.launch {
                    repository.addCategory(name, isIncome, isInvestment)
                    categories = repository.getCategories()
                    showCategoryDialog = false
                }
            }
        )
    }

    val subcategoryTarget = subcategoryParent
    if (subcategoryTarget != null) {
        SubcategoryDialog(
            parentName = subcategoryTarget.name,
            existingNames = categories.map { it.name },
            onDismiss = { subcategoryParent = null },
            onSave = { name ->
                scope.launch {
                    repository.addCategory(
                        name,
                        subcategoryTarget.isIncome,
                        subcategoryTarget.isInvestment,
                        subcategoryTarget.name
                    )
                    categories = repository.getCategories()
                    subcategoryParent = null
                }
            }
        )
    }

    val categoryToRename = renameCategoryTarget
    if (categoryToRename != null) {
        CategoryRenameDialog(
            currentName = categoryToRename.name,
            existingNames = categories.map { it.name },
            onDismiss = { renameCategoryTarget = null },
            onSave = { name ->
                scope.launch {
                    if (repository.renameCategory(categoryToRename.name, name)) {
                        categories = repository.getCategories()
                        renameCategoryTarget = null
                    }
                }
            }
        )
    }

    if (showAssetDialog) {
        AssetDialog(
            accounts = accounts,
            onDismiss = { showAssetDialog = false },
            onSave = { name, type, unit, quantity, buyPrice, currentPrice, accountId, note ->
                scope.launch {
                    repository.addAsset(
                        AssetEntity(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            type = type,
                            unit = unit,
                            quantity = quantity,
                            buyPrice = buyPrice,
                            currentPrice = currentPrice,
                            accountId = accountId,
                            note = note
                        )
                    )
                    assets = repository.getAssets()
                    showAssetDialog = false
                }
            }
        )
    }
    if (showPatternDialog) {
        SmsPatternDialog(
            onDismiss = { showPatternDialog = false },
            onSave = { sender, keywords, isFinancial, isIncome ->
                scope.launch {
                    repository.addPattern(
                        SmsPatternEntity(
                            id = UUID.randomUUID().toString(),
                            sender = sender,
                            keywords = keywords,
                            isFinancial = isFinancial,
                            isIncome = isIncome
                        )
                    )
                    patterns = repository.getPatterns()
                    showPatternDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, dueMillis: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dueMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueMillis)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("کار جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان کار") }
                )
                Button(onClick = { showDatePicker = true }) {
                    Text("سررسید: ${JalaliDate.formatJalali(dueMillis)}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(title.trim(), dueMillis)
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لغو") }
        }
    )
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("تأیید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("لغو") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ChequeReminderDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, dueMillis: Long, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueMillis)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("یادآور جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان (مثلاً چک صیادی یا قبض برق)") }
                )
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ (تومان)") }
                )
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)") }
                )
                Button(onClick = { showDatePicker = true }) {
                    Text("سررسید: ${JalaliDate.formatJalali(dueMillis)}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = normalizeDigits(amountText).toLongOrNull() ?: 0L
                    if (title.isNotBlank() && amount > 0) {
                        onSave(title.trim(), amount, dueMillis, note.trim())
                    }
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لغو") }
        }
    )
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("تأیید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("لغو") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DebtDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (person: String, amount: Long, isReceivable: Boolean, accountId: String, dueMillis: Long) -> Unit
) {
    var person by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isReceivable by remember { mutableStateOf(true) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "main") }
    var dueMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueMillis)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("طلب یا بدهی جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("نام شخص") }
                )
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ (تومان)") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isReceivable,
                        onClick = { isReceivable = true },
                        label = { Text("طلب") }
                    )
                    FilterChip(
                        selected = !isReceivable,
                        onClick = { isReceivable = false },
                        label = { Text("بدهی") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = selectedAccount == account.id,
                            onClick = { selectedAccount = account.id },
                            label = { Text(account.name) }
                        )
                    }
                }
                Button(onClick = { showDatePicker = true }) {
                    Text("سررسید: ${JalaliDate.formatJalali(dueMillis)}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = normalizeDigits(amountText).toLongOrNull() ?: 0L
                    if (person.isNotBlank() && amount > 0) {
                        onSave(person.trim(), amount, isReceivable, selectedAccount, dueMillis)
                    }
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لغو") }
        }
    )
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("تأیید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("لغو") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun InstallmentDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, total: Long, months: Int, accountId: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var monthsText by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "main") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("قسط جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") }
                )
                TextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    label = { Text("مبلغ کل (تومان)") }
                )
                TextField(
                    value = monthsText,
                    onValueChange = { monthsText = it },
                    label = { Text("تعداد ماه") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = selectedAccount == account.id,
                            onClick = { selectedAccount = account.id },
                            label = { Text(account.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = normalizeDigits(totalText).toLongOrNull() ?: 0L
                    val months = normalizeDigits(monthsText).toIntOrNull() ?: 0
                    if (title.isNotBlank() && total > 0 && months > 0) {
                        onSave(title.trim(), total, months, selectedAccount)
                    }
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لغو") }
        }
    )
}

@Composable
private fun AccountDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val duplicate = name.isNotBlank() && existingNames.contains(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("حساب جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام حساب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (duplicate) {
                    Text("این نام قبلاً استفاده شده است.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = name.isNotBlank() && !duplicate
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )
}

@Composable
private fun AccountRenameDialog(
    currentName: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val duplicate = name.isNotBlank() && name != currentName && existingNames.contains(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("تغییر نام حساب", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام جدید") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (duplicate) {
                    Text("این نام قبلاً استفاده شده است.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = name.isNotBlank() && !duplicate
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )
}

@Composable
private fun CategoryDialog(
    existingNames: List<String>,
    initialType: CategoryType = CategoryType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (name: String, isIncome: Boolean, isInvestment: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    val duplicate = name.isNotBlank() && existingNames.contains(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("دسته جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دسته") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == CategoryType.EXPENSE,
                        onClick = { selectedType = CategoryType.EXPENSE },
                        label = { Text("هزینه") }
                    )
                    FilterChip(
                        selected = selectedType == CategoryType.INCOME,
                        onClick = { selectedType = CategoryType.INCOME },
                        label = { Text("درآمد") }
                    )
                    FilterChip(
                        selected = selectedType == CategoryType.INVESTMENT,
                        onClick = { selectedType = CategoryType.INVESTMENT },
                        label = { Text("سرمایه‌گذاری") }
                    )
                }
                if (duplicate) {
                    Text("این نام قبلاً استفاده شده است.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        selectedType == CategoryType.INCOME,
                        selectedType == CategoryType.INVESTMENT
                    )
                },
                enabled = name.isNotBlank() && !duplicate
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )
}

@Composable
private fun SubcategoryDialog(
    parentName: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val duplicate = name.isNotBlank() && existingNames.contains(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("زیردسته جدید برای $parentName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام زیردسته") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (duplicate) {
                    Text("این نام قبلاً استفاده شده است.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = name.isNotBlank() && !duplicate
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )
}

@Composable
private fun CategoryRenameDialog(
    currentName: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val duplicate = name.isNotBlank() && name != currentName && existingNames.contains(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("تغییر نام دسته", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام جدید") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (duplicate) {
                    Text("این نام قبلاً استفاده شده است.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = name.isNotBlank() && !duplicate
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )
}

@Composable
private fun MiniStat(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ManagementCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = title)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = "باز کردن")
        }
    }
}

@Composable
private fun AssetDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, unit: String, quantity: Double, buyPrice: Double, currentPrice: Double?, accountId: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("طلا") }
    var unit by remember { mutableStateOf("گرم") }
    var quantityText by remember { mutableStateOf("") }
    var buyPriceText by remember { mutableStateOf("") }
    var currentPriceText by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "main") }
    var note by remember { mutableStateOf("") }
    val types = listOf("طلا", "ارز", "ارز دیجیتال", "سهام و سایر", "سایر")
    val units = assetUnitsFor(type)
    val quickPicks = assetQuickPicksFor(type)
    val quantity = normalizeDigits(quantityText).toDoubleOrNull() ?: 0.0
    val buyPrice = normalizeDigits(buyPriceText).toDoubleOrNull()
    val currentPrice = normalizeDigits(currentPriceText).toDoubleOrNull()?.takeIf { it >= 0 }
    val valid = name.isNotBlank() && buyPrice != null &&
        isValidAssetInput(quantity, buyPrice)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("افزودن دارایی جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دارایی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (quickPicks.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickPicks.forEach { pick ->
                            FilterChip(
                                selected = name == pick,
                                onClick = { name = pick },
                                label = { Text(pick) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.take(3).forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = {
                                type = option
                                unit = assetUnitsFor(option).first()
                            },
                            label = { Text(option) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.drop(3).forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = {
                                type = option
                                unit = assetUnitsFor(option).first()
                            },
                            label = { Text(option) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    units.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unit = option },
                            label = { Text(option) }
                        )
                    }
                }
                TextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("مقدار") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = buyPriceText,
                    onValueChange = { buyPriceText = it },
                    label = { Text("قیمت خرید هر واحد (تومان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = currentPriceText,
                    onValueChange = { currentPriceText = it },
                    label = { Text("قیمت روز هر واحد (تومان، اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = selectedAccount == account.id,
                            onClick = { selectedAccount = account.id },
                            label = { Text(account.name) }
                        )
                    }
                }
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(), type, unit, quantity, buyPrice ?: 0.0,
                        currentPrice, selectedAccount, note.trim()
                    )
                },
                enabled = valid
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )
}

@Composable
private fun SmsPatternDialog(
    onDismiss: () -> Unit,
    onSave: (sender: String, keywords: String, isFinancial: Boolean, isIncome: Boolean) -> Unit
) {
    var sender by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var isFinancial by remember { mutableStateOf(true) }
    var isIncome by remember { mutableStateOf(false) }
    val valid = sender.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("تعریف الگوی جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("نام فرستنده یا سرشماره") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("کلمات کلیدی (با کاما جدا کنید)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("مثل: برداشت، خرید", style = MaterialTheme.typography.bodySmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(checked = isFinancial, onCheckedChange = { isFinancial = it })
                    Text("آیا این پیامک تراکنش مالی است؟")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("هزینه") }
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("درآمد") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(sender.trim(), keywords.trim(), isFinancial, isIncome) },
                enabled = valid
            ) {
                Text("ذخیره الگو")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

/** واحدهای هر نوع دارایی در فرم افزودن دارایی. */
private fun assetUnitsFor(type: String): List<String> = when (type) {
    "طلا" -> listOf("گرم", "سوت", "مثقال", "اونس")
    "ارز" -> listOf("دلار", "یورو", "درهم", "لیر", "یوان")
    "ارز دیجیتال" -> listOf("BTC", "ETH", "USDT", "TON", "SOL")
    "سهام و سایر" -> listOf("سهم", "واحد صندوق", "واحد")
    else -> listOf("عدد", "واحد", "فقره")
}

/** پیشنهادهای سریع نام برای هر نوع دارایی. */
private fun assetQuickPicksFor(type: String): List<String> = when (type) {
    "طلا" -> listOf("طلای ۱۸ عیار", "سکه امامی", "طلای دست‌دوم")
    "ارز" -> listOf("دلار آمریکا", "یورو", "درهم امارات")
    "ارز دیجیتال" -> listOf("بیت‌کوین", "اتریوم", "تتر")
    "سهام و سایر" -> listOf("سهام بورس", "صندوق سرمایه‌گذاری", "اوراق مشارکت")
    else -> emptyList()
}

/** نمایش مقدار دارایی بدون اعشار اضافی. */
private fun formatAssetQuantity(quantity: Double): String {
    val whole = quantity.toLong()
    return if (quantity == whole.toDouble()) whole.toString() else quantity.toString()
}
