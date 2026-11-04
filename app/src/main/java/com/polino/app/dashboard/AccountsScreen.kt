package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.polino.app.data.AccountEntity
import com.polino.app.data.AccountSmsSenderEntity
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import kotlinx.coroutines.launch

private val AccountsDark = Color(0xFF1E1438)
private val AccountsBorder = Color(0xFF3D3368)
private val AccountsMuted = Color(0xFFB3ACD6)
private val AccountsGreen = Color(0xFF34C89E)
private val AccountsPurple = Color(0xFFA855F7)

/**
 * مانده یک حساب از تراکنش‌هایش: درآمدها مثبت، هزینه‌ها منفی.
 * همان فرمول کارت‌های مدیریت و ورق جزئیات حساب.
 */
internal fun accountBalance(transactions: List<MoneyTransaction>, accountId: String): Long =
    transactions.filter { it.accountId == accountId }
        .sumOf { if (it.income) it.amount else -it.amount }

/**
 * صفحه اختصاصی «حساب‌ها و کارت‌های بانکی»، عین عکس پرگار: کارت‌های تیره
 * هر حساب (نام، مانده، بانک، خط‌های پیامک با آیکن پیام)، و دکمه بنفش
 * «+ افزودن حساب یا کارت». ضربه روی کارت، ورق جزئیات موجود حساب
 * (مرحله ۷۱) را باز می‌کند؛ هیچ ورقی دوباره ساخته نشده است.
 * افزودن، تغییر نام، حذف و اتصال/حذف فرستنده پیامک از همان متدهای
 * موجود TransactionRepository استفاده می‌کنند.
 */
@Composable
internal fun AccountsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var allSenders by remember { mutableStateOf<List<AccountSmsSenderEntity>>(emptyList()) }
    var detailAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var detailSenders by remember { mutableStateOf<List<AccountSmsSenderEntity>>(emptyList()) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showSenderDialog by remember { mutableStateOf(false) }
    var renameAccountTarget by remember { mutableStateOf<AccountEntity?>(null) }

    LaunchedEffect(Unit) {
        accounts = runCatching { repository.getAccounts() }.getOrDefault(emptyList())
        transactions = runCatching { repository.getAll() }.getOrDefault(emptyList())
        allSenders = runCatching { repository.getAccountSmsLinks() }.getOrDefault(emptyList())
    }

    fun refreshAccounts() {
        scope.launch {
            accounts = repository.getAccounts()
            transactions = repository.getAll()
            allSenders = repository.getAccountSmsLinks()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) { Text("بازگشت") }
            Text("حساب‌ها و کارت‌های بانکی", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("${accounts.size} حساب")
            }
            if (accounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = AccountsDark),
                        border = BorderStroke(1.dp, AccountsBorder)
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "هنوز حسابی ثبت نشده است.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "با دکمه زیر، حساب یا کارت بانکی خود را اضافه کنید.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AccountsMuted
                            )
                        }
                    }
                }
            }
            accounts.forEach { account ->
                val senders = allSenders.filter { it.accountId == account.id }
                val balance = accountBalance(transactions, account.id)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                detailAccount = account
                                scope.launch {
                                    detailSenders = repository.getAccountSmsSenders(account.id)
                                }
                            },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = AccountsDark),
                        border = BorderStroke(1.dp, AccountsBorder)
                    ) {
                        Row(
                            Modifier.padding(20.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2150))
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(12.dp).size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    account.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    "${money(balance)} تومان",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                if (account.bankName != null) {
                                    Text(
                                        "بانک: ${account.bankName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AccountsMuted
                                    )
                                }
                                senders.forEach { link ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Message,
                                            contentDescription = null,
                                            tint = AccountsGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "پیامک: ${link.sender}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AccountsGreen
                                        )
                                    }
                                }
                                if (senders.isNotEmpty()) {
                                    Text(
                                        "متصل به پیامک ✓",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AccountsGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { showAccountDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccountsPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "+ افزودن حساب یا کارت",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    if (showAccountDialog) {
        AccountDialog(
            existingNames = accounts.map { it.name },
            onDismiss = { showAccountDialog = false },
            onSave = { name, bankName ->
                scope.launch {
                    repository.addAccount(name, bankName)
                    refreshAccounts()
                    showAccountDialog = false
                }
            }
        )
    }

    val accountToDetail = detailAccount
    if (accountToDetail != null) {
        AccountDetailSheet(
            account = accountToDetail,
            senders = detailSenders,
            transactions = transactions,
            onDismiss = { detailAccount = null },
            onRename = {
                renameAccountTarget = accountToDetail
                detailAccount = null
            },
            onDelete = {
                scope.launch {
                    repository.deleteAccount(accountToDetail.id)
                    refreshAccounts()
                    detailAccount = null
                }
            },
            onAddSender = { showSenderDialog = true },
            onRemoveSender = { sender ->
                scope.launch {
                    repository.removeAccountSmsSender(accountToDetail.id, sender)
                    detailSenders = repository.getAccountSmsSenders(accountToDetail.id)
                    allSenders = repository.getAccountSmsLinks()
                }
            }
        )
    }

    if (showSenderDialog && detailAccount != null) {
        val senderAccount = detailAccount
        if (senderAccount != null) {
            AccountSenderDialog(
                onDismiss = { showSenderDialog = false },
                onSave = { sender ->
                    scope.launch {
                        if (repository.addAccountSmsSender(senderAccount.id, sender)) {
                            detailSenders = repository.getAccountSmsSenders(senderAccount.id)
                            allSenders = repository.getAccountSmsLinks()
                            showSenderDialog = false
                        }
                    }
                }
            )
        }
    }

    val accountToRename = renameAccountTarget
    if (accountToRename != null) {
        AccountRenameDialog(
            currentName = accountToRename.name,
            currentBankName = accountToRename.bankName,
            existingNames = accounts.map { it.name },
            onDismiss = { renameAccountTarget = null },
            onSave = { name, bankName ->
                scope.launch {
                    if (repository.renameAccount(accountToRename.id, name, bankName)) {
                        accounts = repository.getAccounts()
                        renameAccountTarget = null
                    }
                }
            }
        )
    }
}
