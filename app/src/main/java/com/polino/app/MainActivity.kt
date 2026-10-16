package com.polino.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.polino.app.testcenter.TestCenterScreen
import com.polino.app.ui.theme.PolinoTheme
import com.polino.app.dashboard.FinanceDashboard

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

private enum class Screen { Home, TestCenter }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PolinoApp() {
    var screen by remember { mutableStateOf(Screen.Home) }

    when (screen) {
        Screen.Home -> Scaffold(
    topBar = { TopAppBar(title = { Text("پولینو") }) }
) { padding ->
    Column(modifier = Modifier.padding(padding)) {
        FinanceDashboard()
        if (BuildConfig.DEBUG) {
            Button(onClick = { screen = Screen.TestCenter }) {
                Text("مرکز تست")
            }
        }
    }
}

Screen.TestCenter -> TestCenterScreen(onBack = { screen = Screen.Home })
    }
}
