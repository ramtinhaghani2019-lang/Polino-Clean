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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.material3.TextField
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.polino.app.data.AccountEntity
import com.polino.app.data.BudgetRepository
import com.polino.app.data.CategoryEntity
import com.polino.app.data.GoalEntity
import com.polino.app.data.InstallmentEntity
import com.polino.app.data.MoneyTransaction
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
    var showInstallmentDialog by remember { mutableStateOf(false) }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1730))
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "خلاصه دارایی",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF9AA3B8)
                    )
                    Text("مجموع مانده نقدی", color = Color.White)
                    Text(
                        money(totalBalance),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color(0xFFE3B04B)
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
                shape = RoundedCornerShape(20.dp)
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
                shape = RoundedCornerShape(20.dp)
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
        item { Text("بخش‌ها", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
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
                title = "گزارش‌ها",
                subtitle = "گزارش ماه و روند مالی",
                icon = Icons.Default.BarChart,
                onClick = onNavigateToReports
            )
        }
        item { Text("اقساط", style = MaterialTheme.typography.titleMedium) }
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
                        shape = RoundedCornerShape(20.dp)
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
        title = { Text("قسط جدید") },
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
            TextButton(
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
private fun MiniStat(modifier: Modifier = Modifier, label: String, value: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium)
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
        shape = RoundedCornerShape(20.dp)
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
