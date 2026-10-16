package com.polino.app.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Transaction(val title: String, val amount: String)

@Composable
fun FinanceDashboard() {
    val transactions = listOf(
        Transaction("خرید روزانه", "−۲۵۰٬۰۰۰ تومان"),
        Transaction("دریافت حقوق", "+۱۸٬۰۰۰٬۰۰۰ تومان"),
        Transaction("پرداخت قبض", "−۱۸۰٬۰۰۰ تومان")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("داشبورد مالی پولینو", style = MaterialTheme.typography.headlineSmall)
            Text("اطلاعات آزمایشی", style = MaterialTheme.typography.labelMedium)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("موجودی کل")
                    Text("۲۵٬۰۰۰٬۰۰۰ تومان", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("درآمد", "۱۸٬۰۰۰٬۰۰۰ تومان", Modifier.weight(1f))
                SummaryCard("هزینه", "۴۳۰٬۰۰۰ تومان", Modifier.weight(1f))
            }
        }
        item { Text("آخرین تراکنش‌ها", style = MaterialTheme.typography.titleLarge) }
        items(transactions) { transaction ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(transaction.title)
                    Text(transaction.amount, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("افزودن تراکنش — به‌زودی")
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, amount: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title)
            Text(amount, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
