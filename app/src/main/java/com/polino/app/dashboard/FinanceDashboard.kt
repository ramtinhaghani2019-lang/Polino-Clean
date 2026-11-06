package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.polino.app.sms.rescanInboxSms
import androidx.compose.ui.text.font.FontWeight
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


private val PurpleHero = Color(0xFF1E1438)
private val HeroChart = Color(0xFFC084FC)
private val HeroIncome = Color(0xFF16A34A)
private val HeroExpense = Color(0xFFDC2626)
private val HeroMuted = Color(0xFFB3ACD6)
private val HeroBorder = Color(0xFF3D3368)
private val PergarCard = Color(0xFF171233)
private val PergarIncome = Color(0xFF64C0AB)
private val PergarExpense = Color(0xFFF87171)
private val PergarPurple = Color(0xFFA855F7)

private fun ymKey(yearMonth: String): Int {
    val parts = yearMonth.split("-")
    return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 12 +
        (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

/** عدد فارسی بدون «تومان» برای نمایش دوخطی مبالغ (عدد + «تومان» جدا). */
private fun moneyNumber(value: Long): String = money(value).removeSuffix(" تومان")

private fun faNum(value: Long): String =
    NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(value)

/** «امروز» / «دیروز» یا «روز ماه» شمسی، برای زیرنویس ردیف تراکنش اخیر. */
private fun relativeJalaliDay(millis: Long): String {
    if (millis == 0L) return "بدون تاریخ"
    val now = LocalDate.now()
    val todayTriple = JalaliDate.gregorianToJalali(now.year, now.monthValue, now.dayOfMonth)
    val yesterday = now.minusDays(1)
    val yesterdayTriple = JalaliDate.gregorianToJalali(yesterday.year, yesterday.monthValue, yesterday.dayOfMonth)
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val triple = JalaliDate.gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
    return when (triple) {
        todayTriple -> "امروز"
        yesterdayTriple -> "دیروز"
        else -> JalaliDate.formatJalali(millis).substringBeforeLast(" ")
    }
}

/** ساعت تراکنش با رقم‌های فارسی («۱۴:۳۲») برای زیرنویس ردیف تراکنش. */
private fun transactionTime(millis: Long): String {
    if (millis == 0L) return ""
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return "${faNum(time.hour.toLong()).padStart(2, '۰')}:${faNum(time.minute.toLong()).padStart(2, '۰')}"
}

@Composable
fun FinanceDashboard(
    onNavigateToReports: () -> Unit = {},
    onNavigateToManagement: () -> Unit = {},
    onNavigateToEntry: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val budgetRepository = remember { BudgetRepository(context) }
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MoneyTransaction?>(null) }
    var error by remember { mutableStateOf("") }
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var budgets by remember { mutableStateOf<List<BudgetEntity>>(emptyList()) }
    val homeScope = rememberCoroutineScope()
    var homeRescanMessage by remember { mutableStateOf("") }
    var homePendingRescan by remember { mutableStateOf(false) }
    val homeReadSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            homeRescanMessage = "دسترسی خواندن پیامک داده نشده است."
            homePendingRescan = false
        } else if (homePendingRescan) {
            homePendingRescan = false
            homeScope.launch {
                val inserted = rescanInboxSms(context, repository)
                transactions = repository.getAll()
                homeRescanMessage = if (inserted > 0) {
                    "$inserted تراکنش جدید ثبت شد."
                } else {
                    "پیامک تازه‌ای پیدا نشد."
                }
            }
        }
    }
    fun runHomeRescan() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            homeScope.launch {
                val inserted = rescanInboxSms(context, repository)
                transactions = repository.getAll()
                homeRescanMessage = if (inserted > 0) {
                    "$inserted تراکنش جدید ثبت شد."
                } else {
                    "پیامک تازه‌ای پیدا نشد."
                }
            }
        } else {
            homePendingRescan = true
            homeReadSmsLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            repository.migrateIfNeeded()
            // پاک‌سازی یک‌بارمصرف تراکنش‌های بانکی تکراری (مرحله ۱۱۷)؛
            // پیش از خواندن فهرست اجرا می‌شود تا تکراری‌ها از همین
            // بارگذاری اول دیده نشوند. با پرچم فقط یک‌بار اجرا می‌شود.
            repository.cleanupDuplicateSmsTransactionsOnce()
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
            budgets = budgetRepository.getBudgets(JalaliDate.currentJalaliYearMonth())
        }.onFailure {
            error = "بارگذاری اطلاعات ناموفق بود. دوباره تلاش کنید."
        }
    }

    // جمع همه حساب‌ها برای کارت قهرمان خانه (نه حساب انتخاب‌شده).
    val income = transactions.filter { it.income }.sumOf { it.amount }
    val expense = transactions.filterNot { it.income }.sumOf { it.amount }
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


    // کارت قهرمان خانه: موجودی کل همه حساب‌ها + موجودی پایان ۶ ماه اخیر
    // شمسی برای نمودار داخل کارت، بر اساس همه تراکنش‌ها (نه حساب انتخاب‌شده).
    val currentMonth = JalaliDate.currentJalaliYearMonth()
    val heroMonths = remember(currentMonth) {
        JalaliDate.lastJalaliYearMonths(6, currentMonth)
    }
    val heroBalances = remember(transactions, heroMonths) {
        heroMonths.map { month ->
            val key = ymKey(month)
            transactions
                .filter { ymKey(JalaliDate.jalaliYearMonthOf(it.dateMillis)) <= key }
                .sumOf { if (it.income) it.amount else -it.amount }
        }
    }
    // کارت‌های درآمد/هزینه ماه جاری بر اساس همه حساب‌ها (ردیف انتخاب حساب
    // از خانه حذف شده؛ تغییر حساب در صفحه مدیریت انجام می‌شود).
    val monthTotals = remember(transactions, currentMonth) {
        monthTotals(transactionsInMonth(transactions, currentMonth))
    }
    // ۵ تراکنش اخیر همه حساب‌ها برای کارت «آخرین تراکنش‌ها».
    val recentTransactions = remember(transactions) {
        transactions.sortedByDescending { it.dateMillis }.take(5)
    }

    // پس‌زمینه محو آبی→بنفش در تم تیره، عین صفحه خانه پرگار؛ تم روشن
    // پس‌زمینه تم را نگه می‌دارد و کارت‌های تیره روی آن می‌نشینند.
    // مرحله ۹۷: ایستگاه‌ها تیره‌تر شدند (عین محو سراسری تم).
    val isDarkPage = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val homeGradient = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0F2A66), Color(0xFF6D28D9), Color(0xFF100B26)),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }
    val homeBackground = if (isDarkPage) Modifier.background(homeGradient) else Modifier
    LazyColumn(
        modifier = Modifier.fillMaxSize().then(homeBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
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
                    Text(
                        "پولینو",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("نسخه ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { runHomeRescan() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                ) {
                    Text("بررسی مجدد پیامک‌ها")
                }
                if (homeRescanMessage.isNotEmpty()) {
                    Text(
                        homeRescanMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
        item {
            // کارت قهرمان تیره در هر دو تم (عین کارت مبلغ صفحه ثبت مرحله ۶۸):
            // عدد بزرگ ۳۲sp + «تومان» کوچک کنارش، خط روند نقطه‌دار سفید.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = PergarCard),
                border = BorderStroke(1.dp, HeroBorder)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            moneyNumber(income - expense),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "تومان",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HeroMuted
                        )
                    }
                    BalanceChart(
                        months = heroMonths,
                        balances = heroBalances,
                        lineColor = Color.White,
                        labelColor = HeroMuted,
                        chartHeight = 56.dp,
                        dotFill = Color.White,
                        dotRing = HeroBorder
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PergarCard),
                    border = BorderStroke(1.dp, HeroBorder)
                ) {
                    Column(
                        Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "+" + moneyNumber(monthTotals.income),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = PergarIncome
                        )
                        Text(
                            "تومان",
                            style = MaterialTheme.typography.bodySmall,
                            color = HeroMuted
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PergarCard),
                    border = BorderStroke(1.dp, HeroBorder)
                ) {
                    Column(
                        Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "−" + moneyNumber(monthTotals.expense),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = PergarExpense
                        )
                        Text(
                            "تومان",
                            style = MaterialTheme.typography.bodySmall,
                            color = HeroMuted
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
            // کارت نمودار جریان مالی روزانه ماه انتخاب‌شده، عین صفحه خانه پرگار.
            var chartMonth by remember { mutableStateOf(currentMonth) }
            var chartMonthExpanded by remember { mutableStateOf(false) }
            val todayTriple = remember {
                val now = LocalDate.now()
                JalaliDate.gregorianToJalali(now.year, now.monthValue, now.dayOfMonth)
            }
            var selectedDay by remember(chartMonth) {
                mutableStateOf(
                    if (chartMonth == currentMonth) todayTriple.third else null
                )
            }
            val chartMonthTransactions = remember(transactions, chartMonth) {
                transactionsInMonth(transactions, chartMonth)
            }
            val dayTotals = remember(chartMonthTransactions, chartMonth) {
                dailyTotals(chartMonthTransactions, chartMonth)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = PergarCard),
                border = BorderStroke(1.dp, HeroBorder)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "نمودار جریان مالی",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PergarIncome)
                                )
                                Text("درآمد", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PergarExpense)
                                )
                                Text("هزینه", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            }
                        }
                    }
                    CashFlowDayChart(
                        days = dayTotals,
                        selectedDay = selectedDay,
                        onDaySelected = { selectedDay = it }
                    )
                    selectedDay?.let { day ->
                        val totals = dayTotals.firstOrNull { it.day == day }
                        val parts = chartMonth.split("-")
                        val jy = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val jm = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF201A3E)),
                            border = BorderStroke(1.dp, HeroBorder)
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "تاریخ: ${JalaliDate.formatJalali(JalaliDate.jalaliToMillis(jy, jm, day))}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    TextButton(onClick = { selectedDay = null }) {
                                        Text("✕", color = HeroMuted)
                                    }
                                }
                                Text(
                                    "درآمد: ${money(totals?.income ?: 0L)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PergarIncome
                                )
                                Text(
                                    "هزینه: ${money(totals?.expense ?: 0L)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PergarExpense
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "نمودار ماه: ${JalaliDate.jalaliMonthDisplay(chartMonth)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Box {
                            TextButton(onClick = { chartMonthExpanded = true }) {
                                Text(
                                    "تغییر ماه ▾",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PergarPurple
                                )
                            }
                            DropdownMenu(
                                expanded = chartMonthExpanded,
                                onDismissRequest = { chartMonthExpanded = false }
                            ) {
                                JalaliDate.lastJalaliYearMonths(6, currentMonth).forEach { month ->
                                    DropdownMenuItem(
                                        text = { Text(JalaliDate.jalaliMonthDisplay(month)) },
                                        onClick = {
                                            chartMonth = month
                                            chartMonthExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "دسترسی سریع",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickTile(
                    modifier = Modifier.weight(1f),
                    label = "اهداف مالی",
                    icon = Icons.Default.Flag,
                    iconTint = Color(0xFF60A5FA),
                    onClick = onNavigateToGoals
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    label = "مدیریت حساب",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = Color(0xFF38BDF8),
                    onClick = onNavigateToManagement
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    label = "ثبت صوتی",
                    icon = Icons.Default.Mic,
                    iconTint = Color(0xFFC084FC),
                    onClick = onNavigateToEntry
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    label = "رادار مالی",
                    icon = Icons.Default.Explore,
                    iconTint = Color(0xFFC374AB),
                    onClick = onNavigateToReports
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = PergarCard),
                border = BorderStroke(1.dp, HeroBorder)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "آخرین تراکنش‌ها",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        TextButton(onClick = onNavigateToTransactions) {
                            Text(
                                "مشاهده همه ›",
                                style = MaterialTheme.typography.titleSmall,
                                color = PergarPurple
                            )
                        }
                    }
                    if (recentTransactions.isEmpty()) {
                        Text(
                            "هنوز تراکنشی ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HeroMuted
                        )
                        Text(
                            "با دکمه + پایین صفحه اولین تراکنش را ثبت کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HeroMuted
                        )
                    } else {
                        recentTransactions.forEachIndexed { index, transaction ->
                            if (index > 0) {
                                HorizontalDivider(color = HeroBorder, thickness = 1.dp)
                            }
                            TransactionRow(
                                transaction = transaction,
                                onClick = { editing = transaction; showDialog = true }
                            )
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
            initialAccountId = editing?.accountId ?: "main",
            expenseCategories = expenseCategoryNames(categories),
            incomeCategories = incomeCategoryNames(categories),
            onDismiss = { showDialog = false; editing = null; error = "" },
            onSave = { title, amount, isIncome, category, dateMillis, note, store, accountId ->
                val transaction = if (editing == null) {
                    MoneyTransaction(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = amount,
                        income = isIncome,
                        category = category,
                        dateMillis = dateMillis,
                        accountId = accountId,
                        note = note,
                        store = store
                    )
                } else {
                    editing!!.copy(
                        title = title,
                        amount = amount,
                        income = isIncome,
                        category = category,
                        dateMillis = dateMillis,
                        accountId = accountId,
                        note = note,
                        store = store
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


}

/** ردیف تراکنش عین کارت «آخرین تراکنش‌ها» خانه: چیپ فلش، عنوان، روز • دسته، مبلغ علامت‌دار. */
@Composable
private fun TransactionRow(
    transaction: MoneyTransaction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (transaction.income) Color(0xFF1E3A2F)
                    else Color(0xFF3D1F1F)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (transaction.income) "↙" else "↗",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (transaction.income) PergarIncome else PergarExpense
            )
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                transaction.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            val time = transactionTime(transaction.dateMillis)
            Text(
                if (time.isEmpty()) "${relativeJalaliDay(transaction.dateMillis)} • ${transaction.category}"
                else "${relativeJalaliDay(transaction.dateMillis)} • $time • ${transaction.category}",
                style = MaterialTheme.typography.bodySmall,
                color = HeroMuted
            )
        }
        Text(
            (if (transaction.income) "+" else "−") + money(transaction.amount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (transaction.income) PergarIncome else PergarExpense
        )
    }
}

/**
 * صفحه «تراکنش‌ها»: فهرست کامل همه تراکنش‌ها (همه حساب‌ها، جدیدترین اول)
 * با جستجو در عنوان/یادداشت/فروشگاه و فیلتر نوع و حساب — مقصد «مشاهده همه»
 * خانه. ضربه روی ردیف همان دیالوگ ویرایش خانه (TransactionDialog) را باز
 * می‌کند؛ منطق ویرایش تکراری نشده است. بدون تغییر داده و بدون مهاجرت.
 */
@Composable
fun TransactionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    // ۰ = همه، ۱ = درآمد، ۲ = هزینه.
    var typeFilter by remember { mutableStateOf(0) }
    // null = همه حساب‌ها، وگرنه شناسه حساب.
    var accountFilter by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MoneyTransaction?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            repository.migrateIfNeeded()
            transactions = repository.getAll()
            accounts = repository.getAccounts()
            categories = repository.getCategories()
        }.onFailure {
            error = "بارگذاری اطلاعات ناموفق بود. دوباره تلاش کنید."
        }
    }

    val filtered = remember(transactions, query, typeFilter, accountFilter) {
        val q = normalizeDigits(query.trim())
        transactions
            .filter { typeFilter == 0 || it.income == (typeFilter == 1) }
            .filter { accountFilter == null || it.accountId == accountFilter }
            .filter {
                q.isEmpty() ||
                    normalizeDigits(it.title).contains(q, ignoreCase = true) ||
                    normalizeDigits(it.note).contains(q, ignoreCase = true) ||
                    normalizeDigits(it.store).contains(q, ignoreCase = true)
            }
            .sortedByDescending { it.dateMillis }
    }

    // پس‌زمینه محو آبی→بنفش در تم تیره، عین صفحه خانه؛ تم روشن پس‌زمینه تم را نگه می‌دارد.
    // مرحله ۹۷: ایستگاه‌ها تیره‌تر شدند (عین محو سراسری تم).
    val isDarkPage = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageGradient = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0F2A66), Color(0xFF6D28D9), Color(0xFF100B26)),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }
    val pageBackground = if (isDarkPage) Modifier.background(pageGradient) else Modifier

    Column(modifier = Modifier.fillMaxSize().then(pageBackground)) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) { Text("بازگشت") }
            Text("تراکنش‌ها", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        if (error.isNotEmpty()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("جستجو در عنوان، یادداشت و فروشگاه") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("همه", "درآمد", "هزینه").forEachIndexed { index, label ->
                    FilterChip(
                        selected = typeFilter == index,
                        onClick = { typeFilter = index },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = accountFilter == null,
                    onClick = { accountFilter = null },
                    label = { Text("همه حساب‌ها") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary
                    )
                )
                accounts.forEach { account ->
                    FilterChip(
                        selected = accountFilter == account.id,
                        onClick = { accountFilter = account.id },
                        label = { Text(account.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = PergarCard),
            border = BorderStroke(1.dp, HeroBorder)
        ) {
            if (filtered.isEmpty()) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (transactions.isEmpty()) {
                        Text(
                            "هنوز تراکنشی ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HeroMuted
                        )
                        Text(
                            "با دکمه + پایین صفحه اولین تراکنش را ثبت کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HeroMuted
                        )
                    } else {
                        Text(
                            "تراکنشی با این جستجو و فیلترها پیدا نشد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HeroMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    itemsIndexed(filtered, key = { _, item -> item.id }) { index, transaction ->
                        if (index > 0) {
                            HorizontalDivider(color = HeroBorder, thickness = 1.dp)
                        }
                        TransactionRow(
                            transaction = transaction,
                            onClick = { editing = transaction; showDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        TransactionDialog(
            initial = editing,
            accounts = accounts,
            initialAccountId = editing?.accountId ?: "main",
            expenseCategories = expenseCategoryNames(categories),
            incomeCategories = incomeCategoryNames(categories),
            onDismiss = { showDialog = false; editing = null; error = "" },
            onSave = { title, amount, isIncome, category, dateMillis, note, store, accountId ->
                val transaction = if (editing == null) {
                    MoneyTransaction(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = amount,
                        income = isIncome,
                        category = category,
                        dateMillis = dateMillis,
                        accountId = accountId,
                        note = note,
                        store = store
                    )
                } else {
                    editing!!.copy(
                        title = title,
                        amount = amount,
                        income = isIncome,
                        category = category,
                        dateMillis = dateMillis,
                        accountId = accountId,
                        note = note,
                        store = store
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
}

private fun compactAxisLabel(value: Long): String = when {
    value >= 1_000_000L -> faNum(value / 1_000_000L) + "م"
    value >= 1_000L -> faNum(value / 1_000L) + "هزار"
    else -> faNum(value)
}

@Composable
private fun CashFlowDayChart(
    days: List<DayTotal>,
    selectedDay: Int?,
    onDaySelected: (Int) -> Unit
) {
    if (days.isEmpty()) {
        Text("داده‌ای برای این ماه نیست.", style = MaterialTheme.typography.bodyMedium, color = HeroMuted)
        return
    }
    val maxValue = maxOf(days.maxOf { it.income }, days.maxOf { it.expense }, 1L)
    val textMeasurer = rememberTextMeasurer()
    Row {
        Column(
            modifier = Modifier.height(160.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(compactAxisLabel(maxValue), style = MaterialTheme.typography.bodySmall, color = HeroMuted)
            Text(compactAxisLabel(maxValue / 2), style = MaterialTheme.typography.bodySmall, color = HeroMuted)
            Text("۰", style = MaterialTheme.typography.bodySmall, color = HeroMuted)
        }
        Spacer(Modifier.width(8.dp))
        BoxWithConstraints(Modifier.weight(1f)) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(days, widthPx) {
                        detectTapGestures { offset ->
                            val n = days.size
                            val index = ((offset.x / widthPx) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                            onDaySelected(days[index].day)
                        }
                    }
            ) {
                val labelStrip = 20.dp.toPx()
                val plotHeight = size.height - labelStrip
                val xFor: (Int) -> Float = { day ->
                    if (days.size > 1) (day - 1).toFloat() / (days.size - 1) * size.width else size.width / 2f
                }
                val yFor: (Long) -> Float = { value ->
                    plotHeight - (value.toFloat() / maxValue.toFloat()) * (plotHeight - 16f) - 8f
                }
                val incomePoints = days.map { Offset(xFor(it.day), yFor(it.income)) }
                val expensePoints = days.map { Offset(xFor(it.day), yFor(it.expense)) }
                for (index in 0 until incomePoints.size - 1) {
                    drawLine(PergarIncome, incomePoints[index], incomePoints[index + 1], strokeWidth = 3f)
                    drawLine(PergarExpense, expensePoints[index], expensePoints[index + 1], strokeWidth = 3f)
                }
                selectedDay?.let { day ->
                    val x = xFor(day)
                    drawLine(
                        color = PergarPurple,
                        start = Offset(x, 0f),
                        end = Offset(x, plotHeight),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                    days.firstOrNull { it.day == day }?.let { totals ->
                        drawCircle(PergarPurple, radius = 8f, center = Offset(x, yFor(totals.income)))
                        drawCircle(Color.White, radius = 5f, center = Offset(x, yFor(totals.income)))
                        drawCircle(PergarPurple, radius = 8f, center = Offset(x, yFor(totals.expense)))
                        drawCircle(Color.White, radius = 5f, center = Offset(x, yFor(totals.expense)))
                    }
                }
                listOf(1, 7, 14, 21, days.size).distinct().filter { it <= days.size }.forEach { day ->
                    val label = textMeasurer.measure(
                        AnnotatedString(faNum(day.toLong())),
                        style = TextStyle(fontSize = 12.sp, color = HeroMuted)
                    )
                    drawText(
                        label,
                        topLeft = Offset(
                            (xFor(day) - label.size.width / 2f).coerceIn(0f, size.width - label.size.width),
                            plotHeight + 4.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceChart(
    months: List<String>,
    balances: List<Long>,
    lineColor: Color = HeroChart,
    labelColor: Color = HeroMuted,
    chartHeight: Dp = 110.dp,
    dotFill: Color? = null,
    dotRing: Color? = null
) {
    if (balances.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
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
                drawLine(lineColor, points[index], points[index + 1], strokeWidth = 3f)
            }
            points.forEach {
                if (dotFill != null) {
                    drawCircle(dotRing ?: lineColor, radius = 8f, center = it)
                    drawCircle(dotFill, radius = 5f, center = it)
                } else {
                    drawCircle(lineColor, radius = 5f, center = it)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { month ->
                Text(
                    JalaliDate.jalaliMonthDisplay(month).substringBefore(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor
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
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PergarCard),
            border = BorderStroke(1.dp, HeroBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
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
    onSave: suspend (title: String, amount: Long, income: Boolean, category: String, dateMillis: Long, note: String, store: String, accountId: String) -> Boolean,
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
    var store by remember { mutableStateOf(initial?.store ?: "") }
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
            shape = RoundedCornerShape(28.dp),
            containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
            title = { Text("حذف تراکنش", fontWeight = FontWeight.Bold) },
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
        shape = RoundedCornerShape(28.dp),
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface,
        title = { Text(if (initial == null) "تراکنش جدید" else "ویرایش تراکنش", fontWeight = FontWeight.Bold) },
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
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it },
                    label = { Text("فروشگاه/مبدأ (اختیاری)") },
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
            Button(onClick = {
                val value = normalizeDigits(amount).toLongOrNull()
                if (title.isBlank() || value == null || value <= 0L) {
                    validation = "عنوان و مبلغ معتبر وارد کنید."
                } else {
                    scope.launch {
                        if (!onSave(title.trim(), value, isIncome, category, dateMillis, note.trim(), store.trim(), accountId)) {
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
