package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polino.app.data.ChequeReminderEntity
import com.polino.app.data.InstallmentEntity
import com.polino.app.data.TransactionRepository
import com.polino.app.data.installmentProgressFraction
import com.polino.app.data.openChequeRemindersTotal
import java.util.UUID
import kotlinx.coroutines.launch

private enum class RemindersSection { HUB, INSTALLMENTS, CHEQUES }

/**
 * صفحه «یادآورها» — هاب عکس پرگار: سرخط «یک‌جا، همه سررسیدها» و دو
 * کارت ناوبری بزرگ اقساط و یادآور چک‌ها با آیکن، زیرعنوان و نشان
 * وضعیت؛ هر کارت به بخش خودش می‌رود. بخش چک‌ها کارت سرخط با جمع
 * مبلغ سررسید باز، کارت خالی با آیکن و توضیح و دکمه ثبت، و ورق تیره
 * افزودن/ویرایش دارد. تسویه یادآور همچنان مسیر موجود settleCheque
 * است و تراکنشی ثبت نمی‌کند؛ افزودن قسط همچنان در بخش مدیریت است.
 */
@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val scope = rememberCoroutineScope()
    var installments by remember { mutableStateOf<List<InstallmentEntity>>(emptyList()) }
    var cheques by remember { mutableStateOf<List<ChequeReminderEntity>>(emptyList()) }
    var section by remember { mutableStateOf(RemindersSection.HUB) }
    var showChequeSheet by remember { mutableStateOf(false) }
    var editingCheque by remember { mutableStateOf<ChequeReminderEntity?>(null) }

    fun refreshCheques() {
        scope.launch {
            cheques = runCatching { repository.getCheques() }.getOrDefault(emptyList())
        }
    }

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
        when (section) {
            RemindersSection.HUB -> {
                Text(
                    "یک‌جا، همه سررسیدها",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
                )
                Text(
                    "برای ورود به هر بخش، یکی از گزینه‌های زیر را انتخاب کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReminderNavCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalance,
                        iconTint = Color(0xFFD97706),
                        title = "اقساط",
                        subtitle = "وام‌ها و خریدهای اقساطی",
                        chipText = if (installments.isEmpty()) "شروع ثبت"
                            else "${installments.size} قسط",
                        chipColor = Color(0xFFD97706),
                        onClick = { section = RemindersSection.INSTALLMENTS }
                    )
                    ReminderNavCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccessTime,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "یادآور چک‌ها",
                        subtitle = "چک‌ها، قبوض و سررسیدها",
                        chipText = if (cheques.none { !it.settled }) "بدون یادآور"
                            else "${cheques.count { !it.settled }} یادآور باز",
                        chipColor = MaterialTheme.colorScheme.primary,
                        onClick = { section = RemindersSection.CHEQUES }
                    )
                }
            }
            RemindersSection.INSTALLMENTS -> {
                TextButton(onClick = { section = RemindersSection.HUB }) {
                    Text("بازگشت به یادآورها")
                }
                Text(
                    "اقساط",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                }
            }
            RemindersSection.CHEQUES -> {
                TextButton(onClick = { section = RemindersSection.HUB }) {
                    Text("بازگشت به یادآورها")
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "یادآوری چک و قبض",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "مبلغ سررسید",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    "مدیریت سررسید چک‌ها، قبوض و تعهدات مالی",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    money(openChequeRemindersTotal(cheques)),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { editingCheque = null; showChequeSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA855F7),
                                contentColor = Color.White
                            )
                        ) { Text("+ ثبت یادآور جدید", style = MaterialTheme.typography.titleMedium) }
                    }
                    if (cheques.isEmpty()) {
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
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        "هیچ یادآوری چک یا قبضی ثبت نشده است",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "برای سررسید چک‌های دریافتی یا پرداختی، قبوض، اجاره‌بها و موعد پرداخت‌ها یادآور ثبت کنید تا به موقع به شما اطلاع داده شود.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { editingCheque = null; showChequeSheet = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFA855F7),
                                            contentColor = Color.White
                                        )
                                    ) { Text("+ ثبت یادآور جدید") }
                                }
                            }
                        }
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
                                        val status = chequeDueStatus(
                                            cheque.dueMillis, System.currentTimeMillis(), cheque.settled
                                        )
                                        Text(
                                            cheque.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (status == ChequeDueStatus.OVERDUE) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            "مبلغ: ${money(cheque.amount)} — " +
                                                when (status) {
                                                    ChequeDueStatus.SETTLED -> "پرداخت شد"
                                                    ChequeDueStatus.OVERDUE -> "سررسید گذشته"
                                                    ChequeDueStatus.DUE_TODAY -> "سررسید امروز"
                                                    ChequeDueStatus.UPCOMING ->
                                                        "سررسید: ${JalaliDate.formatJalali(cheque.dueMillis)}"
                                                } +
                                                if (cheque.note.isNotBlank()) " — ${cheque.note}" else ""
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                enabled = !cheque.settled,
                                                onClick = {
                                                    scope.launch {
                                                        runCatching { repository.settleCheque(cheque) }
                                                        refreshCheques()
                                                    }
                                                }
                                            ) { Text("تسویه") }
                                            TextButton(
                                                onClick = { editingCheque = cheque; showChequeSheet = true }
                                            ) { Text("ویرایش") }
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        runCatching { repository.deleteCheque(cheque.id) }
                                                        refreshCheques()
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    "حذف",
                                                    color = MaterialTheme.colorScheme.error
                                                )
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
    }
    if (showChequeSheet) {
        ChequeReminderSheet(
            existing = editingCheque,
            onDismiss = { showChequeSheet = false },
            onSave = { title, amount, dueMillis, note ->
                scope.launch {
                    runCatching {
                        repository.addCheque(
                            editingCheque?.copy(
                                title = title,
                                amount = amount,
                                dueMillis = dueMillis,
                                note = note
                            ) ?: ChequeReminderEntity(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                amount = amount,
                                dueMillis = dueMillis,
                                settled = false,
                                note = note
                            )
                        )
                    }
                    refreshCheques()
                    showChequeSheet = false
                }
            }
        )
    }
}

@Composable
private fun ReminderNavCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    chipText: String,
    chipColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = chipColor.copy(alpha = 0.15f)
            ) {
                Text(
                    chipText,
                    style = MaterialTheme.typography.labelMedium,
                    color = chipColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
