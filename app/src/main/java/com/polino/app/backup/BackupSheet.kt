package com.polino.app.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.core.content.FileProvider
import com.polino.app.dashboard.ExportContent
import com.polino.app.dashboard.ExportRange
import com.polino.app.dashboard.ExportSheet
import com.polino.app.dashboard.JalaliDate
import com.polino.app.dashboard.buildExportSections
import com.polino.app.dashboard.exportRangeBounds
import com.polino.app.dashboard.sectionsToXlsx
import com.polino.app.data.BackupData
import com.polino.app.data.BudgetRepository
import com.polino.app.data.TransactionRepository
import com.polino.app.data.fullBackupToJson
import com.polino.app.data.parseBackup
import com.polino.app.data.parseLegacyTransactions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val SheetDark = Color(0xFF1E1438)
private val SheetMuted = Color(0xFFB3ACD6)
private val SheetPurple = Color(0xFFA855F7)
private val SheetGreen = Color(0xFF64C0AB)

/**
 * ورق «پشتیبان‌گیری و بازیابی» عین ورق پرگار: فهرست یکپارچه تاریخ‌دار
 * پشتیبان‌ها (دستی + خودکار؛ تاریخ + ساعت + اندازه + اشتراک + دکمه سبز
 * بازیابی برای هر فایل)، دکمه بنفش «ایجاد پشتیبان جدید»، دکمه «انتخاب
 * فایل پشتیبان از حافظه»، ردیف «خروج گزارش مالی اکسل» و دکمه بنفش بستن.
 * پشتیبان‌گیری خودکار روزانه در پوشه Documents/PolinoBackups فعال است و
 * پشتیبان‌های خودکار قدیمی‌تر از ۷ روز در هر اسکن حذف می‌شوند؛ پشتیبان‌های
 * دستی در حافظه داخلی برنامه‌اند و هرگز خودکار حذف نمی‌شوند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupSheet(
    transactionRepository: TransactionRepository,
    budgetRepository: BudgetRepository,
    onRestored: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
    var files by remember { mutableStateOf<List<ListedBackup>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var pendingRestoreText by remember { mutableStateOf<String?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }
    var pendingExport by remember {
        mutableStateOf<Pair<ExportRange, ExportContent>?>(null)
    }

    fun refreshFiles() {
        scope.launch {
            runCatching { pruneDocumentsAutoBackups(context, System.currentTimeMillis()) }
            val manual = runCatching { listBackupFiles(context) }.getOrDefault(emptyList())
            val auto = listDocumentsAutoBackups(context)
            files = mergeBackupLists(manual, auto)
        }
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

    fun shareFile(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک پشتیبان"))
        }.onFailure {
            error = "اشتراک‌گذاری ناموفق بود. دوباره تلاش کنید."
        }
    }

    fun shareUri(uri: Uri) {
        runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک پشتیبان"))
        }.onFailure {
            error = "اشتراک‌گذاری ناموفق بود. دوباره تلاش کنید."
        }
    }

    fun createBackup() {
        scope.launch {
            runCatching {
                val data = BackupData(
                    transactions = transactionRepository.getAll(),
                    budgets = budgetRepository.getAllBudgets(),
                    categories = transactionRepository.getCategories(),
                    accounts = transactionRepository.getAccounts(),
                    recurring = transactionRepository.getRecurring(),
                    goals = transactionRepository.getGoals(),
                    accountSmsSenders = transactionRepository.getAccountSmsLinks(),
                    installments = transactionRepository.getInstallments(),
                    plans = transactionRepository.getPlans(),
                    notes = transactionRepository.getNotes()
                )
                writeBackupFile(context, fullBackupToJson(data), System.currentTimeMillis())
            }.onSuccess {
                refreshFiles()
                message = "پشتیبان جدید ساخته شد."
            }.onFailure {
                error = "ساخت پشتیبان ناموفق بود. دوباره تلاش کنید."
            }
        }
    }

    fun applyRestore(text: String) {
        val backup = parseBackup(text)
        val legacy = if (backup == null) parseLegacyTransactions(text) else emptyList()
        if (backup == null && legacy.isEmpty()) {
            error = "فایلی برای بازیابی پیدا نشد یا خراب است."
            return
        }
        scope.launch {
            runCatching {
                if (backup != null) {
                    backup.accounts.forEach { transactionRepository.upsertAccount(it) }
                    backup.accountSmsSenders.forEach {
                        transactionRepository.upsertAccountSmsSender(it)
                    }
                    backup.transactions.forEach { transactionRepository.upsert(it) }
                    backup.categories.forEach {
                        transactionRepository.addCategory(it.name, it.isIncome)
                    }
                    backup.recurring.forEach { transactionRepository.addRecurring(it) }
                    backup.goals.forEach { transactionRepository.addGoal(it) }
                    backup.installments.forEach { transactionRepository.addInstallment(it) }
                    backup.plans.forEach { transactionRepository.addPlan(it) }
                    backup.notes.forEach { transactionRepository.addNote(it) }
                    backup.budgets.forEach {
                        budgetRepository.setBudget(it.yearMonth, it.category, it.limitAmount)
                    }
                } else {
                    legacy.forEach { transactionRepository.upsert(it) }
                }
            }.onSuccess {
                onRestored()
                message = if (backup != null) {
                    "بازیابی کامل انجام شد: ${backup.transactions.size} تراکنش."
                } else {
                    "بازیابی انجام شد: ${legacy.size} تراکنش."
                }
            }.onFailure {
                error = "بازیابی ناموفق بود. دوباره تلاش کنید."
            }
        }
    }

    fun queueRestore(text: String) {
        if (parseBackup(text) == null && parseLegacyTransactions(text).isEmpty()) {
            error = "فایلی برای بازیابی پیدا نشد یا خراب است."
        } else {
            pendingRestoreText = text
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(Charsets.UTF_8)
                    } ?: ""
                }.getOrDefault("")
                queueRestore(text)
            }
        }
    }

    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val (range, content) = pendingExport
                        ?: (ExportRange.THIS_MONTH to ExportContent())
                    val bounds = exportRangeBounds(range, JalaliDate.currentJalaliYearMonth())
                    val all = transactionRepository.getAll()
                    val viewed = all.filter {
                        it.dateMillis in bounds.startMillis until bounds.endExclusiveMillis
                    }
                    val debts = transactionRepository.getDebts()
                    val sections = buildExportSections(viewed, debts, content)
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(sectionsToXlsx(sections))
                    } ?: throw IllegalStateException("no output stream")
                }.onSuccess {
                    message = "خروجی اکسل انجام شد."
                }.onFailure {
                    error = "خروجی اکسل ناموفق بود. دوباره تلاش کنید."
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "پشتیبان‌گیری و بازیابی",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "پشتیبان‌گیری خودکار روزانه در پوشه Documents/PolinoBackups فعال است؛ " +
                    "پشتیبان‌های خودکار قدیمی‌تر از ۷ روز خودکار حذف می‌شوند. " +
                    "پشتیبان‌های دستی در حافظه داخلی برنامه‌اند و هرگز خودکار حذف نمی‌شوند.",
                style = MaterialTheme.typography.bodySmall,
                color = SheetMuted
            )
            if (message.isNotEmpty()) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = SheetGreen)
            }
            if (error.isNotEmpty()) {
                Text(error, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFB7185))
            }
            Text(
                "پشتیبان‌های اخیر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (files.isEmpty()) {
                Text(
                    "هنوز پشتیبانی ساخته نشده است؛ با دکمه زیر اولین پشتیبان را بسازید " +
                        "یا منتظر پشتیبان خودکار روزانه بمانید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SheetMuted
                )
            } else {
                files.forEach { listed ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                JalaliDate.formatJalali(listed.info.createdMillis) +
                                    " • " + timeFormat.format(Date(listed.info.createdMillis)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                backupDisplaySize(listed.info.sizeBytes) +
                                    if (listed.isAuto) " • خودکار" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = SheetMuted
                            )
                        }
                        IconButton(onClick = {
                            val uri = listed.documentUri
                            if (uri != null) {
                                shareUri(uri)
                            } else {
                                shareFile(File(backupDir(context), listed.info.name))
                            }
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "اشتراک پشتیبان")
                        }
                        Button(
                            onClick = {
                                val uri = listed.documentUri
                                if (uri != null) {
                                    scope.launch {
                                        queueRestore(readDocumentsBackupText(context, uri))
                                    }
                                } else {
                                    val file = File(backupDir(context), listed.info.name)
                                    val text = runCatching {
                                        file.readText(Charsets.UTF_8)
                                    }.getOrDefault("")
                                    queueRestore(text)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SheetGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("بازیابی")
                        }
                    }
                }
            }
            Button(
                onClick = { createBackup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SheetPurple)
            ) {
                Text("ایجاد پشتیبان جدید (Export)", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { pickFileLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("انتخاب فایل پشتیبان از حافظه (Import)", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { showExportSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SheetGreen)
            ) {
                Text("خروج گزارش مالی اکسل", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SheetPurple)
            ) {
                Text("بستن", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (pendingRestoreText != null) {
        val text = pendingRestoreText!!
        val backup = parseBackup(text)
        AlertDialog(
            onDismissRequest = { pendingRestoreText = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = SheetDark,
            title = { Text("بازیابی از فایل", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (backup != null) {
                        "بازیابی شود؟ ${backup.transactions.size} تراکنش، " +
                            "${backup.budgets.size} بودجه، ${backup.categories.size} دسته، " +
                            "${backup.accounts.size} حساب، ${backup.recurring.size} تکرارشونده، " +
                            "${backup.installments.size} قسط، ${backup.goals.size} هدف و " +
                            "${backup.plans.size} برنامه و ${backup.notes.size} یادداشت. " +
                            "داده‌های هم‌شناسه با نسخه فایل جایگزین می‌شوند."
                    } else {
                        val imported = parseLegacyTransactions(text)
                        "بازیابی شود؟ ${imported.size} تراکنش. " +
                            "تراکنش‌های هم‌شناسه جایگزین می‌شوند."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val confirmedText = pendingRestoreText
                    pendingRestoreText = null
                    if (confirmedText != null) {
                        applyRestore(confirmedText)
                    }
                }) {
                    Text("بازیابی")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingRestoreText = null
                }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showExportSheet) {
        ExportSheet(
            onDismiss = { showExportSheet = false },
            onConfirm = { range, content ->
                pendingExport = range to content
                showExportSheet = false
                xlsxLauncher.launch("polino-report.xlsx")
            }
        )
    }
}
