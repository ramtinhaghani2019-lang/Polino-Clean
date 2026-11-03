package com.polino.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.polino.app.dashboard.CalendarScreen
import com.polino.app.dashboard.FinanceDashboard
import com.polino.app.dashboard.ManagementScreen
import com.polino.app.dashboard.ReportsScreen
import com.polino.app.testcenter.TestCenterScreen
import com.polino.app.ui.theme.PolinoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PolinoTheme {
                    PolinoApp()
                }
            }
        }
    }
}

private enum class Tab { Home, Calendar, Management, Reports }

private val NavBarColor = Color(0xFF101830)
private val NavGold = Color(0xFFE3B04B)
private val NavGray = Color(0xFF8A91A8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PolinoApp() {
    var tab by remember { mutableStateOf(Tab.Home) }
    var addRequest by remember { mutableStateOf(0) }
    var showTestCenter by remember { mutableStateOf(false) }

    if (showTestCenter) {
        TestCenterScreen(onBack = { showTestCenter = false })
        return
    }

    Scaffold(
        topBar = {
            if (BuildConfig.DEBUG) {
                TopAppBar(
                    title = { Text("پولینو") },
                    actions = {
                        Button(onClick = { showTestCenter = true }) {
                            Text("تست‌سنتر")
                        }
                    }
                )
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
                        selectedIconColor = NavGold,
                        selectedTextColor = NavGold,
                        unselectedIconColor = NavGray,
                        unselectedTextColor = NavGray,
                        indicatorColor = NavGold.copy(alpha = 0.18f)
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
                        onClick = {
                            tab = Tab.Home
                            addRequest++
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(NavGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "افزودن تراکنش",
                                    tint = Color.Black
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
                    addRequest = addRequest,
                    onNavigateToReports = { tab = Tab.Reports }
                )
                Tab.Calendar -> CalendarScreen()
                Tab.Management -> ManagementScreen(
                    onNavigateHome = { tab = Tab.Home },
                    onNavigateToReports = { tab = Tab.Reports }
                )
                Tab.Reports -> ReportsScreen(onBack = { tab = Tab.Home })
            }
        }
    }
}
