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
import com.polino.app.sms.DetectedSender
import com.polino.app.sms.detectSenders
import com.polino.app.sms.rescanInboxSms
import com.polino.app.sms.scanInboxSms
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polino.app.data.AccountEntity
import com.polino.app.data.AccountSmsSenderEntity
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
import com.polino.app.data.installmentProgressFraction
import com.polino.app.data.RecurringEntity
import java.util.UUID
import kotlinx.coroutines.launch
import com.polino.app.data.TransactionRepository
import com.polino.app.backup.BackupSheet
import com.polino.app.security.SecuritySheet

@Composable
fun ManagementScreen(
    onNavigateHome: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSmartBudget: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onShowGoals: () -> Unit = {},
    onShowSettings: () -> Unit = {},
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
    var detailAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var detailSenders by remember { mutableStateOf<List<AccountSmsSenderEntity>>(emptyList()) }
    var allSenders by remember { mutableStateOf<List<AccountSmsSenderEntity>>(emptyList()) }
    var showSenderDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showAssetDialog by remember { mutableStateOf(false) }
    var showSecuritySheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
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
    var detectedSenders by remember { mutableStateOf<List<DetectedSender>?>(null) }
    var smsRescanMessage by remember { mutableStateOf("") }
    var smsDetectMessage by remember { mutableStateOf("") }
    var pendingInboxAction by remember { mutableStateOf("") }
    val readSmsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            if (pendingInboxAction == "detect") {
                smsDetectMessage = "دسترسی خواندن پیامک داده نشده است."
            } else if (pendingInboxAction == "rescan") {
                smsRescanMessage = "دسترسی خواندن پیامک داده نشده است."
            }
            pendingInboxAction = ""
            return@rememberLauncherForActivityResult
        }
        when (pendingInboxAction) {
            "detect" -> {
                pendingInboxAction = ""
                scope.launch {
                    detectedSenders = detectSenders(scanInboxSms(context))
                    smsDetectMessage = if (detectedSenders.isNullOrEmpty()) {
                        "فرستنده‌ای در صندوق پیامک پیدا نشد."
                    } else {
                        ""
                    }
                }
            }
            "rescan" -> {
                pendingInboxAction = ""
                scope.launch {
                    val inserted = rescanInboxSms(context, repository)
                    transactions = repository.getAll()
                    smsRescanMessage = if (inserted > 0) {
                        "$inserted تراکنش جدید ثبت شد."
                    } else {
                        "پیامک تازه‌ای پیدا نشد."
                    }
                }
            }
        }
    }
    fun runDetectSenders() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scope.launch {
                detectedSenders = detectSenders(scanInboxSms(context))
                smsDetectMessage = if (detectedSenders.isNullOrEmpty()) {
                    "فرستنده‌ای در صندوق پیامک پیدا نشد."
                } else {
                    ""
                }
            }
        } else {
            pendingInboxAction = "detect"
            readSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }
    fun runRescanSms() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scope.launch {
                val inserted = rescanInboxSms(context, repository)
                transactions = repository.getAll()
                smsRescanMessage = if (inserted > 0) {
                    "$inserted تراکنش جدید ثبت شد."
                } else {
                    "پیامک تازه‌ای پیدا نشد."
                }
            }
        } else {
            pendingInboxAction = "rescan"
            readSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
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
            allSenders = repository.getAccountSmsLinks()
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
            ManagementCard(
                title = "تنظیمات",
                subtitle = "امنیت، پشتیبان‌گیری، خروجی، پیامک و درباره برنامه",
                icon = Icons.Default.Settings,
                onClick = onShowSettings
            )
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
        item {
            Card(
                onClick = { showSecuritySheet = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "امنیت و حریم خصوصی")
                    Column(Modifier.weight(1f)) {
                        Text("امنیت و حریم خصوصی", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "قفل با پین‌کد، تشخیص چهره / اثر انگشت و بازنشانی کامل",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            Card(
                onClick = { showBackupSheet = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = "پشتیبان‌گیری و دارایی")
                    Column(Modifier.weight(1f)) {
                        Text("پشتیبان‌گیری و دارایی", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "فهرست پشتیبان‌ها، ساخت، بازیابی و خروجی اکسل",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { runRescanSms() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بررسی مجدد پیامک‌ها")
                }
                if (smsRescanMessage.isNotEmpty()) {
                    Text(smsRescanMessage, style = MaterialTheme.typography.bodySmall)
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
        item { Text("فرستنده‌های پیامک شناسایی‌شده در دستگاه", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val detected = detectedSenders
                if (detected == null) {
                    OutlinedButton(onClick = { runDetectSenders() }) {
                        Text("اسکن صندوق پیامک")
                    }
                } else if (detected.isEmpty()) {
                    Text("فرستنده‌ای در صندوق پیامک پیدا نشد.")
                    OutlinedButton(onClick = { runDetectSenders() }) {
                        Text("اسکن دوباره")
                    }
                } else {
                    detected.forEach { sender ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                sender.sender,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "${sender.count} پیامک",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    OutlinedButton(onClick = { runDetectSenders() }) {
                        Text("اسکن دوباره")
                    }
                }
                if (smsDetectMessage.isNotEmpty()) {
                    Text(smsDetectMessage, style = MaterialTheme.typography.bodySmall)
                }
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
            Text(
                "پارس‌های بانک‌های عضو شتاب فعال می‌باشند.",
                style = MaterialTheme.typography.bodySmall
            )
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
                title = "یادآورها",
                subtitle = "یک‌جا، همه سررسیدها",
                icon = Icons.Default.Notifications,
                onClick = onNavigateToReminders
            )
        }
        item {
            ManagementCard(
                title = "اهداف و برنامه‌ریزی",
                subtitle = "${goals.size} هدف",
                icon = Icons.Default.Flag,
                onClick = onShowGoals
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "هیچ قسط یا تسهیلاتی ثبت نشده است",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "وام‌ها و خریدهای اقساطی خود را ثبت کنید تا سررسید و مبلغ ماهانه هر قسط به صورت منظم مدیریت شود.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showInstallmentDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.White
                            )
                        ) {
                            Text("+ ثبت اولین قسط")
                        }
                    }
                }
            }
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
                            if (!installment.bankName.isNullOrBlank()) {
                                Text(
                                    "بانک: ${installment.bankName}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                "مبلغ هر قسط: ${money(installment.installmentAmount)} — " +
                                    "پرداخت‌شده: ${installment.paidCount} از ${installment.monthCount}"
                            )
                            if (installment.startMillis != null) {
                                Text(
                                    "شروع: ${JalaliDate.formatJalali(installment.startMillis)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
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
                val linked = allSenders.any { it.accountId == account.id }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        detailAccount = account
                        scope.launch {
                            detailSenders = repository.getAccountSmsSenders(account.id)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        if (account.bankName != null) {
                            Text(
                                "بانک: ${account.bankName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val balance = transactions.filter { it.accountId == account.id }
                            .sumOf { if (it.income) it.amount else -it.amount }
                        Text("مانده: ${money(balance)}")
                        if (linked) {
                            Text(
                                "متصل به پیامک ✓",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E9E6B)
                            )
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
        InstallmentSheet(
            accounts = accounts,
            onDismiss = { showInstallmentDialog = false },
            onSave = { title, bankName, installmentAmount, totalCount, paidCount, startMillis, accountId ->
                scope.launch {
                    repository.addInstallment(
                        InstallmentEntity(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            totalAmount = installmentAmount * totalCount,
                            monthCount = totalCount,
                            paidCount = paidCount,
                            bankName = bankName,
                            installmentAmount = installmentAmount,
                            startMillis = startMillis,
                            accountId = accountId
                        )
                    )
                    installments = repository.getInstallments()
                    showInstallmentDialog = false
                }
            }
        )
    }

    if (showSecuritySheet) {
        SecuritySheet(
            biometricEnabled = lockEnabled,
            onBiometricChange = onLockChange,
            onDismiss = { showSecuritySheet = false }
        )
    }

    if (showBackupSheet) {
        BackupSheet(
            transactionRepository = repository,
            budgetRepository = budgetRepository,
            onRestored = {
                scope.launch {
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
                        allSenders = repository.getAccountSmsLinks()
                        budgetCount = budgetRepository.getBudgets(
                            JalaliDate.currentJalaliYearMonth()
                        ).size
                    }
                }
            },
            onDismiss = { showBackupSheet = false }
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
            onSave = { name, bankName ->
                scope.launch {
                    repository.addAccount(name, bankName)
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
            currentBankName = accountToRename.bankName,
            existingNames = accounts.map { it.name },
            onDismiss = { renameAccountTarget = null },
            onSave = { name, bankName ->
                scope.launch {
                    if (repository.renameAccount(accountToRename.id, name, bankName)) {
                        accounts = repository.getAccounts()
                        renameAccountTarget = null
                    }
                }
            }
        )
    }

    val accountToDetail = detailAccount
    if (accountToDetail != null) {
        AccountDetailSheet(
            account = accountToDetail,
            senders = detailSenders,
            transactions = transactions,
            onDismiss = { detailAccount = null },
            onRename = {
                renameAccountTarget = accountToDetail
                detailAccount = null
            },
            onDelete = {
                scope.launch {
                    repository.deleteAccount(accountToDetail.id)
                    accounts = repository.getAccounts()
                    transactions = repository.getAll()
                    allSenders = repository.getAccountSmsLinks()
                    detailAccount = null
                }
            },
            onAddSender = { showSenderDialog = true },
            detectedSenders = detectedSenders,
            onScanSenders = { runDetectSenders() },
            smsDetectMessage = smsDetectMessage,
            onLinkDetectedSender = { sender ->
                scope.launch {
                    if (repository.addAccountSmsSender(accountToDetail.id, sender)) {
                        detailSenders = repository.getAccountSmsSenders(accountToDetail.id)
                        allSenders = repository.getAccountSmsLinks()
                    }
                }
            },
            onRemoveSender = { sender ->
                scope.launch {
                    repository.removeAccountSmsSender(accountToDetail.id, sender)
                    detailSenders = repository.getAccountSmsSenders(accountToDetail.id)
                    allSenders = repository.getAccountSmsLinks()
                }
            }
        )
    }

    if (showSenderDialog && detailAccount != null) {
        val senderAccount = detailAccount
        if (senderAccount != null) {
            AccountSenderDialog(
                onDismiss = { showSenderDialog = false },
                onSave = { sender ->
                    scope.launch {
                        if (repository.addAccountSmsSender(senderAccount.id, sender)) {
                            detailSenders = repository.getAccountSmsSenders(senderAccount.id)
                            allSenders = repository.getAccountSmsLinks()
                            showSenderDialog = false
                        }
                    }
                }
            )
        }
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

private val InstallmentSheetDark = Color(0xFF1E1438)
private val InstallmentSheetCard = Color(0xFF2A2150)
private val InstallmentSheetBorder = Color(0xFF3D3368)
private val InstallmentSheetMuted = Color(0xFFB3ACD6)
private val InstallmentSheetPurple = Color(0xFFA855F7)

/** فقط ارقام لاتین، فارسی و عربی می‌مانند. */
private fun String.filterInstallmentDigits(): String = filter { char ->
    char.isDigit() || char in "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩"
}

/**
 * ورق «قسط جدید»، عین فرم پرگار: عنوان و مبلغ هر قسط و تعداد کل و تاریخ
 * شروع اجباری، نام بانک و تعداد پرداخت‌شده اختیاری، دکمه‌های انصراف و
 * ذخیره قسط. نشان «کیپد فارسی» فقط برچسب است؛ فیلد مبلغ از پیش ارقام
 * فارسی و عربی را هم می‌پذیرد. انتخاب حساب برای تراکنش پرداخت نگه داشته
 * شده است (فرم پرگار حسابی نشان نمی‌دهد).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallmentSheet(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        bankName: String?,
        installmentAmount: Long,
        totalCount: Int,
        paidCount: Int,
        startMillis: Long,
        accountId: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var paidText by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "main") }
    var validation by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startMillis)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = InstallmentSheetDark
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "قسط جدید",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                }
            }
            item {
                InstallmentSheetField(
                    label = "عنوان قسط",
                    required = true,
                    numeric = false,
                    placeholder = "عنوان قسط (مثلا وام مسکن)",
                    value = title,
                    onValueChange = { title = it }
                )
            }
            item {
                InstallmentSheetField(
                    label = "نام بانک / موسسه",
                    required = false,
                    numeric = false,
                    placeholder = null,
                    value = bankName,
                    onValueChange = { bankName = it }
                )
            }
            item {
                InstallmentSheetField(
                    label = "مبلغ هر قسط (تومان)",
                    required = true,
                    numeric = true,
                    placeholder = null,
                    value = amountText,
                    onValueChange = { amountText = it.filterInstallmentDigits() }
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InstallmentSheetField(
                        label = "تعداد کل اقساط",
                        required = true,
                        numeric = false,
                        placeholder = null,
                        value = totalText,
                        onValueChange = { totalText = it.filterInstallmentDigits() },
                        modifier = Modifier.weight(1f)
                    )
                    InstallmentSheetField(
                        label = "پرداخت شده",
                        required = false,
                        numeric = false,
                        placeholder = null,
                        value = paidText,
                        onValueChange = { paidText = it.filterInstallmentDigits() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "تاریخ شروع قسط",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        InstallmentMiniChip("اجباری", MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InstallmentSheetCard,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Text(
                                if (JalaliDate.formatJalali(startMillis) ==
                                    JalaliDate.formatJalali(System.currentTimeMillis())
                                ) {
                                    "امروز"
                                } else {
                                    JalaliDate.formatJalali(startMillis)
                                }
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "حساب پرداخت",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { account ->
                            item {
                                FilterChip(
                                    selected = selectedAccount == account.id,
                                    onClick = { selectedAccount = account.id },
                                    label = { Text(account.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = InstallmentSheetCard,
                                        labelColor = Color.White,
                                        selectedContainerColor = InstallmentSheetPurple,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
            if (validation.isNotEmpty()) {
                item {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, InstallmentSheetBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            val parsedAmount = normalizeDigits(amountText).toLongOrNull()
                            val parsedTotal = normalizeDigits(totalText).toIntOrNull()
                            val parsedPaid = if (paidText.isBlank()) {
                                0
                            } else {
                                normalizeDigits(paidText).toIntOrNull()
                            }
                            val error = when {
                                title.isBlank() -> "عنوان قسط را وارد کنید."
                                parsedAmount == null || parsedAmount <= 0 -> "مبلغ هر قسط معتبر نیست."
                                parsedTotal == null || parsedTotal <= 0 -> "تعداد کل اقساط معتبر نیست."
                                parsedPaid == null || parsedPaid < 0 || parsedPaid > (parsedTotal ?: 0) ->
                                    "تعداد پرداخت‌شده معتبر نیست."
                                else -> ""
                            }
                            validation = error
                            if (error.isEmpty() && parsedAmount != null &&
                                parsedTotal != null && parsedPaid != null
                            ) {
                                onSave(
                                    title.trim(),
                                    bankName.trim().ifBlank { null },
                                    parsedAmount,
                                    parsedTotal,
                                    parsedPaid,
                                    startMillis,
                                    selectedAccount
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InstallmentSheetPurple
                        )
                    ) {
                        Text("ذخیره قسط")
                    }
                }
            }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { startMillis = it }
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
private fun InstallmentSheetField(
    label: String,
    required: Boolean,
    numeric: Boolean,
    placeholder: String?,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
            if (required) {
                InstallmentMiniChip("اجباری", MaterialTheme.colorScheme.error)
            }
            if (numeric) {
                InstallmentMiniChip("کیپد فارسی", InstallmentSheetMuted)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = placeholder?.let { { Text(it, color = InstallmentSheetMuted) } },
            suffix = if (numeric) {
                { Text("تومان", color = InstallmentSheetPurple) }
            } else {
                null
            },
            colors = installmentSheetFieldColors()
        )
    }
}

@Composable
private fun InstallmentMiniChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .border(BorderStroke(1.dp, color), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun installmentSheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = InstallmentSheetCard,
    unfocusedContainerColor = InstallmentSheetCard,
    focusedBorderColor = InstallmentSheetBorder,
    unfocusedBorderColor = InstallmentSheetBorder,
    cursorColor = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailSheet(
    account: AccountEntity,
    senders: List<AccountSmsSenderEntity>,
    transactions: List<MoneyTransaction>,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddSender: () -> Unit,
    onRemoveSender: (String) -> Unit,
    detectedSenders: List<DetectedSender>? = null,
    onScanSenders: () -> Unit = {},
    smsDetectMessage: String = "",
    onLinkDetectedSender: (String) -> Unit = {}
) {
    val sheetDark = Color(0xFF1E1438)
    val sheetCard = Color(0xFF2A2150)
    val sheetBorder = Color(0xFF3D3368)
    val sheetMuted = Color(0xFFB3ACD6)
    val sheetGreen = Color(0xFF34C89E)
    val sheetGreenBg = Color(0xFF1E4436)
    val sheetRed = Color(0xFFFF6B6B)
    val sheetRedBg = Color(0xFF4A2020)
    var showMenu by remember { mutableStateOf(false) }
    var showDetectedPicker by remember { mutableStateOf(false) }
    val accountTransactions = transactions.filter { it.accountId == account.id }
    val balance = accountTransactions.sumOf { if (it.income) it.amount else -it.amount }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetDark
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (account.bankName != null) {
                            Text(
                                "بانک: ${account.bankName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = sheetMuted
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌ها", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تغییر نام") },
                                onClick = {
                                    showMenu = false
                                    onRename()
                                }
                            )
                            if (account.id != "main") {
                                DropdownMenuItem(
                                    text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (senders.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = sheetGreenBg
                    ) {
                        Text(
                            "متصل به پیامک ✓",
                            style = MaterialTheme.typography.labelLarge,
                            color = sheetGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = sheetCard),
                    border = BorderStroke(1.dp, sheetBorder)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("موجودی فعلی", style = MaterialTheme.typography.labelLarge, color = sheetMuted)
                        Text(
                            "${money(balance)} تومان",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (balance >= 0) sheetGreen else sheetRed
                        )
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = sheetCard),
                    border = BorderStroke(1.dp, sheetBorder)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "اتصال شماره پیامک بانکی",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "پیامک‌های واریز و برداشت این سرشماره‌ها به‌طور خودکار به این حساب متصل می‌شوند.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = sheetMuted
                        )
                        senders.forEach { link ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = sheetGreen)
                                Text(
                                    link.sender,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                                IconButton(onClick = { onRemoveSender(link.sender) }) {
                                    Icon(Icons.Default.Close, contentDescription = "قطع اتصال", tint = sheetRed)
                                }
                            }
                        }
                        OutlinedButton(onClick = onAddSender) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("افزودن شماره پیامک", modifier = Modifier.padding(start = 4.dp))
                        }
                        Button(
                            onClick = {
                                showDetectedPicker = true
                                onScanSenders()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("اتصال از پیامک‌ها")
                        }
                        if (showDetectedPicker) {
                            val detected = detectedSenders
                            if (detected == null) {
                                Text(
                                    "در حال اسکن صندوق پیامک…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = sheetMuted
                                )
                            } else if (detected.isEmpty()) {
                                Text(
                                    "فرستنده‌ای در صندوق پیامک پیدا نشد.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = sheetMuted
                                )
                            } else {
                                detected.forEach { sender ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            onLinkDetectedSender(sender.sender)
                                            showDetectedPicker = false
                                        }
                                    ) {
                                        Text(
                                            sender.sender,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFF372A63)
                                        ) {
                                            Text(
                                                "${sender.count} پیامک",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color(0xFFD8D0F5),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (smsDetectMessage.isNotEmpty()) {
                                Text(
                                    smsDetectMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = sheetMuted
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "تراکنش‌های این حساب",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF372A63)
                    ) {
                        Text(
                            "${accountTransactions.size} تراکنش",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFD8D0F5),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            if (accountTransactions.isEmpty()) {
                item {
                    Text(
                        "برای این حساب تراکنشی ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = sheetMuted
                    )
                }
            }
            accountTransactions.forEach { tx ->
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(
                                if (tx.income) sheetGreenBg else sheetRedBg,
                                CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (tx.income) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (tx.income) sheetGreen else sheetRed
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(tx.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "${tx.category} • ${JalaliDate.formatJalali(tx.dateMillis)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = sheetMuted
                            )
                        }
                        Text(
                            (if (tx.income) "+" else "−") + money(tx.amount) + " تومان",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (tx.income) sheetGreen else sheetRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSenderDialog(
    onDismiss: () -> Unit,
    onSave: (sender: String) -> Unit
) {
    var sender by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text("افزودن شماره پیامک", fontWeight = FontWeight.Bold) },
        text = {
            TextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("سرشماره یا نام فرستنده") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(sender.trim()) },
                enabled = sender.isNotBlank()
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
private fun AccountDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, bankName: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
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
                TextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("نام بانک یا مؤسسه (اختیاری)") },
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
                onClick = { onSave(name, bank.ifBlank { null }) },
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
    currentBankName: String?,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, bankName: String?) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bank by remember { mutableStateOf(currentBankName ?: "") }
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
                TextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("نام بانک یا مؤسسه (اختیاری)") },
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
                onClick = { onSave(name, bank.ifBlank { null }) },
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
