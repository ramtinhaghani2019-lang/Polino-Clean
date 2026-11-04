package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polino.app.data.ChequeReminderEntity
import com.polino.app.data.InstallmentEntity
import com.polino.app.data.TransactionRepository
import com.polino.app.data.installmentProgressFraction
import com.polino.app.data.isChequeReminderOverdue
import com.polino.app.data.openChequeRemindersTotal

/**
 * صفحه «یادآورها»: نمای یک‌جای همه سررسیدها — اقساط و یادآورهای چک و قبض.
 * فقط خواندنی است؛ ثبت، پرداخت و حذف همچنان در بخش مدیریت انجام می‌شود و
 * همین داده‌های سراسری را می‌خواند.
 */
@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    var installments by remember { mutableStateOf<List<InstallmentEntity>>(emptyList()) }
    var cheques by remember { mutableStateOf<List<ChequeReminderEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching {
            installments = repository.getInstallments()
            cheques = repository.getCheques()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) { Text("بازگشت") }
            Text("یادآورها", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "یک‌جا، همه سررسیدها",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "اقساط",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
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
                                "ثبت قسط جدید در بخش مدیریت انجام می‌شود.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
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
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "یادآور چک و قبض",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            item {
                Text("مبلغ سررسید باز: ${money(openChequeRemindersTotal(cheques))}")
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
                            }
                        }
                    }
                }
            }
        }
    }
}
