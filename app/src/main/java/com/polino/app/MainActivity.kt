package com.polino.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.polino.app.dashboard.FinanceDashboard
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

private enum class Screen { Home, TestCenter, Reports }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PolinoApp() {
    var screen by remember { mutableStateOf(Screen.Home) }

    when (screen) {
        Screen.Home -> Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.polino_logo),
                                contentDescription = "لوگوی پولینو",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("پولینو", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    actions = {
                        Button(onClick = { screen = Screen.Reports }) {
                            Text("گزارش")
                        }
                        if (BuildConfig.DEBUG) {
                            Button(onClick = { screen = Screen.TestCenter }) {
                                Text("تست‌سنتر")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                FinanceDashboard()
            }
        }

        Screen.TestCenter -> TestCenterScreen(onBack = { screen = Screen.Home })

        Screen.Reports -> ReportsScreen(onBack = { screen = Screen.Home })
    }
}
