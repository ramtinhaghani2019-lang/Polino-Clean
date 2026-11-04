package com.polino.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.polino.app.backup.runDailyAutoBackupIfNeeded
import com.polino.app.dashboard.CalendarScreen
import com.polino.app.dashboard.CalculatorSheet
import com.polino.app.dashboard.DebtsScreen
import com.polino.app.dashboard.EntryScreen
import com.polino.app.dashboard.FinanceDashboard
import com.polino.app.dashboard.GoalsScreen
import com.polino.app.dashboard.ManagementScreen
import com.polino.app.dashboard.RemindersScreen
import com.polino.app.dashboard.ReportsScreen
import com.polino.app.dashboard.SettingsScreen
import com.polino.app.dashboard.SmartBudgetScreen
import com.polino.app.dashboard.TransactionsScreen
import com.polino.app.data.BudgetRepository
import com.polino.app.data.TransactionRepository
import com.polino.app.testcenter.TestCenterScreen
import com.polino.app.security.AppLockScreen
import com.polino.app.security.LOCK_ENABLED_KEY
import com.polino.app.security.LOCK_PREFS_NAME
import com.polino.app.security.PIN_HASH_KEY
import com.polino.app.security.PIN_SALT_KEY
import com.polino.app.security.PinLockScreen
import com.polino.app.ui.theme.PolinoTheme
import com.polino.app.ui.theme.THEME_MODE_KEY
import com.polino.app.ui.theme.THEME_PREFS_NAME

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val context = LocalContext.current
                var themeMode by remember {
                    mutableStateOf(
                        context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
                            .getString(THEME_MODE_KEY, "system") ?: "system"
                    )
                }
                PolinoTheme(themeMode = themeMode) {
                    PolinoApp(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(THEME_MODE_KEY, mode).apply()
                        }
                    )
                }
            }
        }
    }
}

private enum class Tab { Home, Calendar, Management, Reports }

private val NavBarColor = Color(0xFF150F2E)
private val NavPurple = Color(0xFFA855F7)
private val NavGray = Color(0xFF9A93B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PolinoApp(
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val initialLockEnabled = remember {
        context.getSharedPreferences(LOCK_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(LOCK_ENABLED_KEY, false)
    }
    var lockEnabled by remember { mutableStateOf(initialLockEnabled) }
    // قفل پین مستقل از قفل بیومتریک است؛ اگر پین تنظیم شده باشد، در شروع
    // برنامه صفحه ورود پین نشان داده می‌شود، وگرنه قفل قبلی عین قبل رفتار می‌کند.
    val pinSet = remember {
        val prefs = context.getSharedPreferences(LOCK_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.contains(PIN_HASH_KEY) && prefs.contains(PIN_SALT_KEY)
    }
    var unlocked by remember { mutableStateOf(!initialLockEnabled && !pinSet) }

    if ((lockEnabled || pinSet) && !unlocked) {
        if (pinSet) {
            PinLockScreen(onUnlocked = { unlocked = true })
        } else {
            AppLockScreen(onUnlocked = { unlocked = true })
        }
        return
    }

    // پشتیبان‌گیری خودکار روزانه: در شروع برنامه، خارج از نخ اصلی، بی‌صدا؛
    // اگر امروز هنوز ساخته نشده، یک نسخه در Documents/PolinoBackups می‌نویسد.
    val autoBackupTransactionRepository = remember { TransactionRepository(context) }
    val autoBackupBudgetRepository = remember { BudgetRepository(context) }
    LaunchedEffect(Unit) {
        runDailyAutoBackupIfNeeded(
            context,
            autoBackupTransactionRepository,
            autoBackupBudgetRepository
        )
    }

    var tab by remember { mutableStateOf(Tab.Home) }
    var entryScreen by remember { mutableStateOf(false) }
    var showTestCenter by remember { mutableStateOf(false) }
    var showSmartBudget by remember { mutableStateOf(false) }
    var showReminders by remember { mutableStateOf(false) }
    var showTransactions by remember { mutableStateOf(false) }
    var showGoals by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDebts by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }
    var calculatorAmount by remember { mutableStateOf("") }

    if (showTestCenter) {
        TestCenterScreen(onBack = { showTestCenter = false })
        return
    }

    if (showSmartBudget) {
        SmartBudgetScreen(onBack = { showSmartBudget = false })
        return
    }

    if (showReminders) {
        RemindersScreen(onBack = { showReminders = false })
        return
    }

    if (showTransactions) {
        TransactionsScreen(onBack = { showTransactions = false })
        return
    }

    if (showGoals) {
        GoalsScreen(
            onBack = { showGoals = false },
            onNavigateToTasks = { showGoals = false; tab = Tab.Management },
            onNavigateToReminders = { showGoals = false; showReminders = true }
        )
        return
    }

    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false },
            onNavigateToManagement = { showSettings = false; tab = Tab.Management },
            lockEnabled = lockEnabled,
            onLockChange = { enabled ->
                lockEnabled = enabled
                if (enabled) unlocked = true
                context.getSharedPreferences(LOCK_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putBoolean(LOCK_ENABLED_KEY, enabled).apply()
            }
        )
        return
    }

    if (showDebts) {
        DebtsScreen(onBack = { showDebts = false })
        return
    }

    if (entryScreen) {
        EntryScreen(
            initialAmount = calculatorAmount,
            onBack = {
                entryScreen = false
                calculatorAmount = ""
            },
            onSaved = {
                entryScreen = false
                calculatorAmount = ""
                tab = Tab.Home
            }
        )
        return
    }

    if (showCalculator) {
        CalculatorSheet(
            onDismiss = { showCalculator = false },
            onUseAmount = { amount ->
                calculatorAmount = amount
                showCalculator = false
                entryScreen = true
            }
        )
    }

    Scaffold(
        // مرحله ۷۰: شفاف تا محو آبی→بنفش پس‌زمینه تم (PolinoTheme) در تم
        // تیره از پشت همه صفحه‌های اصلی دیده شود؛ در تم روشن هم همان
        // پس‌زمینه تم از لایه زیرین می‌آید.
        containerColor = Color.Transparent,
        topBar = {
            if (BuildConfig.DEBUG) {
                TopAppBar(
                    title = { Text("پولینو") },
                    actions = {
                        TextButton(onClick = { showTestCenter = true }) {
                            Text("تست‌سنتر")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (tab == Tab.Home) {
                FloatingActionButton(
                    onClick = { showCalculator = true },
                    containerColor = NavPurple,
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = "ماشین‌حساب"
                    )
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                NavigationBar(containerColor = NavBarColor) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = NavGray,
                        unselectedTextColor = NavGray,
                        indicatorColor = NavPurple
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Home,
                        onClick = { tab = Tab.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = "خانه") },
                        label = { Text("خانه") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Calendar,
                        onClick = { tab = Tab.Calendar },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "تقویم") },
                        label = { Text("تقویم") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { entryScreen = true },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(NavPurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "افزودن تراکنش",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Management,
                        onClick = { tab = Tab.Management },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "مدیریت") },
                        label = { Text("مدیریت") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Reports,
                        onClick = { tab = Tab.Reports },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "گزارش‌ها") },
                        label = { Text("گزارش‌ها") },
                        colors = itemColors
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.Home -> FinanceDashboard(
                    onNavigateToReports = { tab = Tab.Reports },
                    onNavigateToManagement = { tab = Tab.Management },
                    onNavigateToEntry = { entryScreen = true },
                    onNavigateToTransactions = { showTransactions = true },
                    onNavigateToGoals = { showGoals = true }
                )
                Tab.Calendar -> CalendarScreen()
                Tab.Management -> ManagementScreen(
                    onNavigateHome = { tab = Tab.Home },
                    onNavigateToReports = { tab = Tab.Reports },
                    onNavigateToSmartBudget = { showSmartBudget = true },
                    onNavigateToReminders = { showReminders = true },
                    onShowGoals = { showGoals = true },
                    onShowSettings = { showSettings = true },
                    onShowDebts = { showDebts = true },
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    lockEnabled = lockEnabled,
                    onLockChange = { enabled ->
                        lockEnabled = enabled
                        if (enabled) unlocked = true
                        context.getSharedPreferences(LOCK_PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(LOCK_ENABLED_KEY, enabled).apply()
                    }
                )
                Tab.Reports -> ReportsScreen(onBack = { tab = Tab.Home })
            }
        }
    }
}
