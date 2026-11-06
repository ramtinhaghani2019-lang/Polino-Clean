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
import com.polino.app.sms.isReadSmsPermanentlyDenied
import com.polino.app.sms.openAppSettings
import com.polino.app.sms.rescanInboxSms
import com.polino.app.sms.scanInboxSms
import com.polino.app.sms.SmsPatternSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementScreen(
    onNavigateHome: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSmartBudget: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onShowGoals: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    onShowDebts: () -> Unit = {},
    onShowCategories: () -> Unit = {},
    onShowInvestments: () -> Unit = {},
    onShowAccounts: () -> Unit = {},
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
    var showPatternSheet by remember { mutableStateOf(false) }
    var showInstallmentsSheet by remember { mutableStateOf(false) }
    var showTasksSheet by remember { mutableStateOf(false) }
    var showDongiSheet by remember { mutableStateOf(false) }
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
    // مرحله ۱۱۸: عین تنظیمات — رد دائم دسترسی باعث می‌شود درخواست
    // دوباره بی‌صدا رد شود؛ این پرچم ورق الگوها را وادار می‌کند کارت
    // واضح با دکمه تنظیمات برنامه نشان دهد.
    var smsPermissionPermanentlyDenied by remember { mutableStateOf(false) }
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
            smsPermissionPermanentlyDenied = isReadSmsPermanentlyDenied(context)
            pendingInboxAction = ""
            return@rememberLauncherForActivityResult
        }
        smsPermissionPermanentlyDenied = false
        when (pendingInboxAction) {
            "detect" -> {
                pendingInboxAction = ""
                // مرحله ۱۱۸: بازخورد فوری عین پرگار، هم‌زمان با شروع
                // اسکن واقعی (نه به‌جای آن).
                smsDetectMessage = "بررسی پیامک‌های اخیر آغاز شد"
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
            // مرحله ۱۱۸: بازخورد فوری عین پرگار، هم‌زمان با شروع
            // اسکن واقعی (نه به‌جای آن).
            smsDetectMessage = "بررسی پیامک‌های اخیر آغاز شد"
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
    val remindersOpenCount = hubReminderCount(
        installments.count { it.paidCount < it.monthCount }, cheques
    )
    val remindersStatusText = if (remindersOpenCount == 0) {
        "همه تسویه‌شده"
    } else {
        remindersHubSubtitle(cheques, installments, System.currentTimeMillis())
    }

    // مرحله ۱۰۹: محو عمودی آبی→بنفش روشن در تم تیره، عین هاب پرگار؛
    // تم روشن پس‌زمینه تم را نگه می‌دارد.
    val isDarkHubPage = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hubPageGradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1D4ED8), Color(0xFF7C3AED), Color(0xFF14102E))
        )
    }
    val hubPageBackground = if (isDarkHubPage) Modifier.background(hubPageGradient) else Modifier

    LazyColumn(
        modifier = Modifier.fillMaxSize().then(hubPageBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "مدیریت مالی",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    "ابزارهای یکپارچه و هوشمند مالی",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
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
            val hubAmountColor = if (isDarkHub) Color(0xFF60A5FA) else Color(0xFF2563EB)
            val goalsStatusText = if (goals.isEmpty()) "بدون هدف" else goalsHubSubtitle(goals)
            val budgetStatusText = if (budgetCount == 0) {
                "تعریف بودجه"
            } else {
                "${persianNumber(budgetCount)} بودجه"
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = hubContainer),
                border = BorderStroke(1.dp, hubBorder)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF3B82F6),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "خلاصه دارایی",
                                tint = Color.White
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "خلاصه دارایی",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = hubNumber
                            )
                            Text(
                                "مجموع مانده نقدی",
                                style = MaterialTheme.typography.bodySmall,
                                color = hubLabel
                            )
                        }
                        Text(
                            money(totalBalance),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = hubAmountColor
                        )
                    }
                    HorizontalDivider(color = hubBorder, thickness = 1.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HubSummaryTile(
                            modifier = Modifier.weight(1f),
                            label = "اهداف پس‌انداز",
                            status = goalsStatusText,
                            dotColor = if (isDarkHub) Color(0xFF34D399) else Color(0xFF047857),
                            statusColor = if (isDarkHub) Color(0xFF34D399) else Color(0xFF047857),
                            isDark = isDarkHub
                        )
                        HubSummaryTile(
                            modifier = Modifier.weight(1f),
                            label = "یادآورها",
                            status = remindersStatusText,
                            dotColor = if (isDarkHub) Color(0xFFFBBF24) else Color(0xFFB45309),
                            statusColor = if (isDarkHub) Color(0xFFFBBF24) else Color(0xFFB45309),
                            isDark = isDarkHub
                        )
                        HubSummaryTile(
                            modifier = Modifier.weight(1f),
                            label = "بودجه‌بندی",
                            status = budgetStatusText,
                            dotColor = if (isDarkHub) Color(0xFFFB7185) else Color(0xFFDC2626),
                            statusColor = if (isDarkHub) Color(0xFFFB7185) else Color(0xFFDC2626),
                            isDark = isDarkHub
                        )
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
                    Modifier.padding(14.dp),
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
                    Modifier.padding(14.dp),
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
            ManagementCard(
                title = "امنیت و حریم خصوصی",
                subtitle = "قفل با پین‌کد، تشخیص چهره / اثر انگشت و بازنشانی کامل",
                icon = Icons.Default.Lock,
                onClick = { showSecuritySheet = true },
                statusChip = if (lockEnabled) "فعال" else "خاموش",
                statusChipPositive = lockEnabled,
                tileColor = Color(0xFF6366F1)
            )
        }
        item {
            ManagementCard(
                title = "پشتیبان‌گیری و دارایی",
                subtitle = "فهرست پشتیبان‌ها، ساخت، بازیابی و خروجی اکسل",
                icon = Icons.Default.Backup,
                onClick = { showBackupSheet = true },
                tileColor = Color(0xFF0EA5E9)
            )
        }
        item { Text("بخش‌ها", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color(0xFF22C55E),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = "خوانش پیامک", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "خوانش پیامک",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            if (smsEnabled) "فعال — تراکنش‌های بانکی به‌صورت خودکار ثبت می‌شوند"
                            else "خاموش — برای ثبت خودکار، دسترسی خواندن پیامک لازم است",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HubStatusChip(
                        text = if (smsEnabled) "فعال" else "خاموش",
                        positive = smsEnabled
                    )
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
            ManagementCard(
                title = "مدیریت الگوهای پیامک",
                subtitle = "فرستنده‌های شناسایی‌شده و قوانین ثبت پیامک",
                icon = Icons.Default.Sms,
                onClick = { showPatternSheet = true },
                statusChip = "${persianNumber(patterns.size)} الگو",
                tileColor = Color(0xFF16A34A)
            )
        }
        item {
            ManagementCard(
                title = "حساب‌ها و کارت‌ها",
                subtitle = "مدیریت موجودی، کارت‌ها و پیامک‌های بانکی",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onShowAccounts,
                statusChip = "${persianNumber(accounts.size)} حساب بانکی",
                tileColor = Color(0xFF3B82F6)
            )
        }
        item {
            ManagementCard(
                title = "دسته‌ها",
                subtitle = "سرفصل‌های درآمد، هزینه و آیکن‌ها",
                icon = Icons.Default.Category,
                onClick = onShowCategories,
                statusChip = "${persianNumber(categories.size)} دسته‌بندی",
                tileColor = Color(0xFFF97316)
            )
        }
        item {
            ManagementCard(
                title = "تکرارشونده‌ها",
                subtitle = "قوانین تکرار خودکار تراکنش‌ها",
                icon = Icons.Default.Repeat,
                onClick = onNavigateHome,
                statusChip = "${persianNumber(recurring.size)} قانون",
                tileColor = Color(0xFFF59E0B)
            )
        }
        item {
            ManagementCard(
                title = "بودجه‌ها و اهداف",
                subtitle = "بودجه ماه و هدف‌های پس‌انداز",
                icon = Icons.Default.Savings,
                onClick = onNavigateToReports,
                statusChip = "${persianNumber(budgetCount)} بودجه",
                tileColor = Color(0xFFEC4899)
            )
        }
        item {
            ManagementCard(
                title = "بودجه‌بندی هوشمند",
                subtitle = "سقف هزینه‌ها و هشدار انحراف بودجه",
                icon = Icons.Default.PieChart,
                onClick = onNavigateToSmartBudget,
                statusChip = if (budgetCount == 0) "تعریف بودجه" else "${persianNumber(budgetCount)} بودجه",
                statusChipPositive = budgetCount > 0,
                tileColor = Color(0xFFEF4444)
            )
        }
        item {
            ManagementCard(
                title = "یادآورها",
                subtitle = "یک‌جا، همه سررسیدها",
                icon = Icons.Default.Notifications,
                onClick = onNavigateToReminders,
                statusChip = remindersStatusText,
                tileColor = Color(0xFFEAB308)
            )
        }
        item {
            ManagementCard(
                title = "اهداف و برنامه‌ریزی",
                subtitle = "پس‌انداز هدفمند و نقشه‌راه مالی",
                icon = Icons.Default.Flag,
                onClick = onShowGoals,
                statusChip = if (goals.isEmpty()) "شروع هدف جدید" else "${persianNumber(goals.size)} هدف",
                tileColor = Color(0xFF10B981)
            )
        }
        item {
            ManagementCard(
                title = "گزارش‌ها",
                subtitle = "گزارش ماه و روند مالی",
                icon = Icons.Default.BarChart,
                onClick = onNavigateToReports,
                tileColor = Color(0xFF8B5CF6)
            )
        }
        item {
            ManagementCard(
                title = "اقساط",
                subtitle = "وام‌ها و خریدهای اقساطی",
                icon = Icons.Default.CreditCard,
                onClick = { showInstallmentsSheet = true },
                statusChip = "${persianNumber(installments.size)} قسط",
                tileColor = Color(0xFF06B6D4)
            )
        }
        item {
            val (receivableTotal, payableTotal) = unsettledTotals(debts)
            val openDebtCount = debts.count { !it.settled }
            ManagementCard(
                title = "طلب و بدهی",
                subtitle = "پیگیری مطالبات و تسویه‌حساب با اشخاص",
                icon = Icons.Default.People,
                onClick = onShowDebts,
                statusChip = if (openDebtCount == 0) "تسویه کامل" else "${persianNumber(openDebtCount)} مورد باز",
                statusChipPositive = openDebtCount == 0,
                tileColor = Color(0xFF7C3AED)
            )
        }
        item {
            ManagementCard(
                title = "کارها و یادآورها",
                subtitle = "کارهای روزانه و سررسیدها",
                icon = Icons.Default.EventNote,
                onClick = { showTasksSheet = true },
                statusChip = tasksHubSubtitle(tasks),
                tileColor = Color(0xFF84CC16)
            )
        }
        item {
            ManagementCard(
                title = "محاسبه دنگی",
                subtitle = "تقسیم هوشمند هزینه‌های مشترک و سه‌نفره",
                icon = Icons.Default.Group,
                onClick = { showDongiSheet = true },
                statusChip = "ماشین‌حساب دنگی",
                tileColor = Color(0xFF2563EB)
            )
        }
        item {
            ManagementCard(
                title = "سرمایه‌گذاری و دارایی‌ها",
                subtitle = "ثبت طلا، ارز، سهام، ملک و پیگیری سود",
                icon = Icons.Default.TrendingUp,
                onClick = onShowInvestments,
                statusChip = "${persianNumber(assets.size)} دارایی",
                tileColor = Color(0xFF14B8A6)
            )
        }
    }

    if (showInstallmentsSheet) {
        val installmentsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInstallmentsSheet = false },
            sheetState = installmentsSheetState
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        item { Text("اقساط", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface) }
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
            }
        }
    }

    if (showTasksSheet) {
        val tasksSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTasksSheet = false },
            sheetState = tasksSheetState
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        item { Text("کارها و یادآورها", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface) }
        item {
            Button(onClick = { showTaskDialog = true }) {
                Text("کار جدید")
            }
        }
        if (tasks.isEmpty()) {
            item { Text("هنوز کاری ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            }
        }
    }

    if (showDongiSheet) {
        val dongiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDongiSheet = false },
            sheetState = dongiSheetState
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        item { Text("محاسبه دنگی", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface) }
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
                        border = BorderStroke(1.dp, Color(0xFF3D3368))
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(48.dp)
                                            .background(Color(0xFF2A2150), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Group,
                                            contentDescription = null,
                                            tint = Color(0xFFA855F7)
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "جمع کل هزینه‌ها",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFB3ACD6)
                                        )
                                        Text(
                                            money(dongiTotal),
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF2A2150)
                                    ) {
                                        Text(
                                            dongiPersonCountText(dongiMembers.size),
                                            color = Color(0xFFC084FC),
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { dongiMembers = dongiMembers.toList() }
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "به‌روزرسانی محاسبه",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "سهم مساوی هر نفر",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFB3ACD6)
                                    )
                                    Text(
                                        dongiShareSummary(dongiSharesList).ifBlank { money(0L) },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF64C0AB)
                                    )
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        "تعداد تسویه‌ها",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFB3ACD6)
                                    )
                                    Text(
                                        dongiSettlementCountText(dongiSettlementsList.size),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Text("اعضای گروه و مبالغ پرداختی", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
                                    IconButton(
                                        onClick = {
                                            dongiMembers = dongiMembers.filterIndexed { i, _ -> i != index }
                                            dongiSaved = false
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف شخص",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
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
                                        0L to "۰",
                                        50_000L to "+۵۰ هزار",
                                        100_000L to "+۱۰۰ هزار",
                                        500_000L to "+۵۰۰ هزار"
                                    ).forEach { (quick, label) ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                dongiMembers = dongiMembers.toMutableList().also {
                                                    it[index] = member.copy(
                                                        paid = if (quick == 0L) 0L else member.paid + quick
                                                    )
                                                }
                                                dongiSaved = false
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                                val dongiBalanceText = dongiBalanceLabel(member.settled, balance)
                                if (dongiBalanceText.isNotEmpty()) {
                                    Text(dongiBalanceText, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("سهم: ${money(share)}", style = MaterialTheme.typography.bodyMedium)
                                    FilterChip(
                                        selected = member.settled,
                                        onClick = {
                                            dongiMembers = dongiMembers.toMutableList().also {
                                                it[index] = member.copy(settled = !member.settled)
                                            }
                                            dongiSaved = false
                                        },
                                        label = { Text("تسویه شده") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF16A34A),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                dongiMembers = dongiMembers + DongiMember("نفر ${dongiMembers.size + 1}", 0L)
                                dongiSaved = false
                            },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA855F7),
                                contentColor = Color.White
                            )
                        ) { Text("+ افزودن شخص") }
                    }
                    Text("تسویه‌حساب و پرداخت‌ها", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (dongiSettlementsList.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
                            border = BorderStroke(1.dp, Color(0xFF3D3368))
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF34C89E)
                                )
                                Text(
                                    "تمام حساب‌ها متوازن است و هیچ پرداختی لازم نیست.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF34C89E)
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
                            border = BorderStroke(1.dp, Color(0xFF3D3368))
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dongiSettlementsList.forEach { settlement ->
                                    Text(
                                        "${settlement.from} به ${settlement.to}: ${money(settlement.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
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
    if (showPatternSheet) {
        SmsPatternSheet(
            patterns = patterns,
            detectedSenders = detectedSenders,
            smsDetectMessage = smsDetectMessage,
            onScanSenders = { runDetectSenders() },
            onOpenAppSettings = { openAppSettings(context) },
            smsPermissionPermanentlyDenied = smsPermissionPermanentlyDenied,
            onAddPattern = { sender, keywords, isFinancial, isIncome ->
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
                }
            },
            onEditPattern = { pattern, sender, keywords, isFinancial, isIncome ->
                scope.launch {
                    repository.addPattern(
                        pattern.copy(
                            sender = sender,
                            keywords = keywords,
                            isFinancial = isFinancial,
                            isIncome = isIncome
                        )
                    )
                    patterns = repository.getPatterns()
                }
            },
            onDeletePattern = { pattern ->
                scope.launch {
                    repository.deletePattern(pattern.id)
                    patterns = repository.getPatterns()
                }
            },
            onDismiss = { showPatternSheet = false }
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
internal fun AccountDetailSheet(
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
                                // مرحله ۱۱۸: اگر اسکن رد شده یا هنوز
                                // نتیجه‌ای نیست، پیام واقعی (رد دسترسی
                                // یا شروع اسکن) نشان داده می‌شود تا
                                // «در حال اسکن…» برای همیشه گیر نکند.
                                if (smsDetectMessage.isNotEmpty()) {
                                    Text(
                                        smsDetectMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = sheetMuted
                                    )
                                } else {
                                    Text(
                                        "در حال اسکن صندوق پیامک…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = sheetMuted
                                    )
                                }
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
internal fun AccountSenderDialog(
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
internal fun AccountDialog(
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
internal fun AccountRenameDialog(
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

/**
 * کاشی وضعیت داخل کارت «خلاصه دارایی» هاب مدیریت (مرحله ۱۰۹، عین پرگار):
 * برچسب با نقطه رنگی + متن وضعیت معنادار، به‌جای عدد خشک.
 */
@Composable
private fun HubSummaryTile(
    modifier: Modifier = Modifier,
    label: String,
    status: String,
    dotColor: Color,
    statusColor: Color,
    isDark: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF2A2150) else Color(0xFFF5F3FF)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF3D3368) else Color(0xFFD8D0F5)
        )
    ) {
        Column(
            Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = dotColor, shape = CircleShape)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFB3ACD6) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                status,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = statusColor
            )
        }
    }
}

@Composable
private fun ManagementCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    statusChip: String? = null,
    statusChipPositive: Boolean = true,
    tileColor: Color? = null,
    tileTint: Color = Color.White
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = tileColor ?: MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tileTint)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            if (statusChip != null) {
                HubStatusChip(text = statusChip, positive = statusChipPositive)
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = "باز کردن")
        }
    }
}

/** Small status pill for hub rows, e.g. «فعال» or «۳ الگو». */
@Composable
private fun HubStatusChip(text: String, positive: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (positive) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (positive) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
internal fun AssetDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, unit: String, quantity: Double, buyPrice: Double, currentPrice: Double?, accountId: String, note: String) -> Unit,
    initial: AssetEntity? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "طلا") }
    var unit by remember { mutableStateOf(initial?.unit ?: "گرم") }
    var quantityText by remember { mutableStateOf(initial?.let { formatAssetQuantity(it.quantity) } ?: "") }
    var buyPriceText by remember { mutableStateOf(initial?.let { formatAssetQuantity(it.buyPrice) } ?: "") }
    var currentPriceText by remember { mutableStateOf(initial?.currentPrice?.let { formatAssetQuantity(it) } ?: "") }
    var selectedAccount by remember { mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id ?: "main") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
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
        title = { Text(if (initial == null) "افزودن دارایی جدید" else "ویرایش دارایی", fontWeight = FontWeight.Bold) },
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPicks.forEach { pick ->
                            FilterChip(
                                selected = name == pick,
                                onClick = { name = pick },
                                label = { Text(pick, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.take(3).forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = {
                                type = option
                                unit = assetUnitsFor(option).first()
                            },
                            label = { Text(option, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.drop(3).forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = {
                                type = option
                                unit = assetUnitsFor(option).first()
                            },
                            label = { Text(option, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    units.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unit = option },
                            label = { Text(option, maxLines = 1, softWrap = false) }
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = selectedAccount == account.id,
                            onClick = { selectedAccount = account.id },
                            label = { Text(account.name, maxLines = 1, softWrap = false) }
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
internal fun formatAssetQuantity(quantity: Double): String {
    val whole = quantity.toLong()
    return if (quantity == whole.toDouble()) whole.toString() else quantity.toString()
}
