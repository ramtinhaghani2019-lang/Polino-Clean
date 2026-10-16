package com.polino.app.testcenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TestCenterScreen(onBack: () -> Unit) {
    var results by remember { mutableStateOf(PolinoSelfTests.runAll()) }
    val passed = results.count { it.passed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مرکز تست پولینو") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$passed از ${results.size} تست سبز است",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(onClick = { results = PolinoSelfTests.runAll() }) {
                        Text("اجرای همه تست‌ها")
                    }
                }
            }

            itemsIndexed(results) { index, result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = if (result.passed) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                                contentDescription = if (result.passed) "موفق" else "ناموفق"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(result.title, style = MaterialTheme.typography.titleMedium)
                                Text(result.description, style = MaterialTheme.typography.bodyMedium)
                                Text(result.details, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val updated = results.toMutableList()
                                updated[index] = PolinoSelfTests.runOne(index)
                                results = updated
                            }
                        ) {
                            Text("تست مجدد همین مورد")
                        }
                    }
                }
            }

            item { Text(" ") }
        }
    }
}
