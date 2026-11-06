package com.polino.app.dashboard

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polino.app.BuildConfig
import com.polino.app.backup.BackupSheet
import com.polino.app.data.BudgetRepository
import com.polino.app.data.SmsPatternEntity
import com.polino.app.data.TransactionRepository
import com.polino.app.security.SecuritySheet
import com.polino.app.sms.DetectedSender
import com.polino.app.sms.SMS_ENABLED_KEY
import com.polino.app.sms.SMS_PREFS_NAME
import com.polino.app.sms.SmsPatternSheet
import com.polino.app.sms.detectSenders
import com.polino.app.sms.isReadSmsPermanentlyDenied
import com.polino.app.sms.openAppSettings
import com.polino.app.sms.scanInboxSms
import java.util.UUID
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private val SettingsDark = Color(0xFF1E1438)
private val SettingsMuted = Color(0xFFB3ACD6)
private val SettingsPurple = Color(0xFFA855F7)
private val SettingsGreen = Color(0xFF64C0AB)
private val SettingsRed = Color(0xFFFB7185)

// مرحله ۱۱۴: رنگ‌های نمونه‌برداری‌شده از فریم تنظیمات پرگار (f_07):
// پس‌زمینه کارت گروهی، خط جداکننده ردیف‌ها و رنگ متن فرعی ردیف‌ها.
private val SettingsGroupBg = Color(0xFF121532)
private val SettingsDivider = Color(0xFF292147)
private val SettingsRowMuted = Color(0xFFC0BDDD)

/**
 * صفحه اختصاصی تنظیمات عین منوی تنظیمات پرگار: ردیف‌های امنیت و حریم
 * خصوصی، پشتیبان‌گیری و دارایی، خروجی اطلاعات، خوانش پیامک، مدیریت
 * الگوهای پیامک، درباره پولینو، ارتباط با ما و تست‌سنتر (فقط در نسخه
 * دیباگ). هر ردیف قابلیتِ موجود را
 * باز می‌کند و چیزی را دوباره نمی‌سازد: قفل و پشتیبان ورق‌های خودشان را
 * دارند، خوانش پیامک صفحه اختصاصی خودش را دارد، الگوها ورق خودشان را،
 * خروجی اکسل ورق انتخاب بازه و محتوا را باز می‌کند و فایل چندبرگی می‌سازد. کارت حساب کاربری و بنر ویژه پرگار عمداً
 * ساخته نشده‌اند (پولینو حساب کاربری و پرداخت درون‌برنامه‌ای ندارد و همه
 * امکاناتش باز است) و هیچ کانال پشتیبانی‌ای اختراع نشده است.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToSmsReading: () -> Unit,
    onNavigateToTestCenter: () -> Unit,
    lockEnabled: Boolean,
    onLockChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val budgetRepository = remember { BudgetRepository(context) }
    val scope = rememberCoroutineScope()
    var showSecuritySheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showContactSheet by remember { mutableStateOf(false) }
    var showPatternSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var pendingExport by remember {
        mutableStateOf<Pair<ExportRange, ExportContent>?>(null)
    }
    var patterns by remember { mutableStateOf<List<SmsPatternEntity>>(emptyList()) }
    var detectedSenders by remember { mutableStateOf<List<DetectedSender>?>(null) }
    var smsDetectMessage by remember { mutableStateOf("") }
    // مرحله ۱۱۸: وقتی دسترسی به‌طور دائم رد شده باشد، درخواست دوباره
    // بی‌صدا رد می‌شود و کاربر «هیچ اتفاقی نمی‌افتد» می‌بیند؛ این
    // پرچم ورق الگوها را وادار می‌کند به‌جای خط کوچک کم‌رنگ، یک کارت
    // واضح با دکمه باز کردن تنظیمات برنامه نشان دهد.
    var smsPermissionPermanentlyDenied by remember { mutableStateOf(false) }
    var pendingInboxAction by remember { mutableStateOf("") }
    val readSmsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            smsDetectMessage = "دسترسی خواندن پیامک داده نشده است."
            smsPermissionPermanentlyDenied = isReadSmsPermanentlyDenied(context)
            pendingInboxAction = ""
            return@rememberLauncherForActivityResult
        }
        smsPermissionPermanentlyDenied = false
        if (pendingInboxAction == "detect") {
            pendingInboxAction = ""
            // مرحله ۱۱۸: بازخورد فوری عین پرگار — پیام همان لحظه
            // نشان داده می‌شود، ولی فقط خبر شروع اسکن واقعی است؛
            // اسکن واقعاً در کرون زیر اجرا می‌شود و نتیجه‌اش جای
            // همین پیام می‌نشیند.
            smsDetectMessage = "بررسی پیامک‌های اخیر آغاز شد"
            scope.launch {
                detectedSenders = detectSenders(scanInboxSms(context))
                smsDetectMessage = if (detectedSenders.isNullOrEmpty()) {
                    "فرستنده‌ای در صندوق پیامک پیدا نشد."
                } else {
                    ""
                }
            }
        }
    }
    fun runDetectSenders() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // مرحله ۱۱۸: بازخورد فوری عین پرگار، هم‌زمان با شروع
            // اسکن واقعی (نه به‌جای آن).
            smsDetectMessage = "بررسی پیامک‌های اخیر آغاز شد"
            scope.launch {
                detectedSenders = detectSenders(scanInboxSms(context))
                smsDetectMessage = if (detectedSenders.isNullOrEmpty()) {
                    "فرستنده‌ای در صندوق پیامک پیدا نشد."
                } else {
                    ""
                }
            }
        } else {
            pendingInboxAction = "detect"
            readSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }
    var smsEnabled by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        smsEnabled = context.getSharedPreferences(SMS_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SMS_ENABLED_KEY, false)
        patterns = repository.getPatterns()
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
                    val all = repository.getAll()
                    val viewed = all.filter {
                        it.dateMillis in bounds.startMillis until bounds.endExclusiveMillis
                    }
                    val debts = repository.getDebts()
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

    // مرحله ۱۱۴: دکمه «بازگشت» داخل صفحه حذف شد (عین پرگار)؛ بازگشت با
    // دکمه سیستمی انجام می‌شود. بدون این BackHandler حذف قرص بازگشت
    // کاربر را در صفحه تنظیمات گیر می‌انداخت، چون MainActivity برای
    // این صفحه هیچ BackHandlerای ندارد.
    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "تنظیمات",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp)
            )
            Text(
                "امنیت، پشتیبان‌گیری، خروجی و پیامک در یک جا",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 16.dp,
                    end = 20.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (message.isNotEmpty()) {
                    item {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SettingsGreen
                        )
                    }
                }
                if (error.isNotEmpty()) {
                    item {
                        Text(
                            error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SettingsRed
                        )
                    }
                }
                item {
                    // مرحله ۱۱۴: همه ردیف‌ها در یک کارت گروهی واحد با
                    // خط‌های جداکننده نازک، عین پرگار (f_07)؛ پیش از این
                    // هر ردیف یک کارت جداگانه بود.
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SettingsGroupBg)
                    ) {
                        Column {
                            SettingsRow(
                                title = "امنیت و حریم خصوصی",
                                subtitle = "قفل با پین‌کد، تشخیص چهره / اثر انگشت و بازنشانی کامل",
                                icon = Icons.Default.Lock,
                                onClick = { showSecuritySheet = true }
                            )
                            HorizontalDivider(color = SettingsDivider, thickness = 1.dp)
                            SettingsRow(
                                title = "پشتیبان‌گیری و دارایی",
                                subtitle = "فهرست پشتیبان‌ها، ساخت، بازیابی و خروجی اکسل",
                                icon = Icons.Default.Backup,
                                onClick = { showBackupSheet = true }
                            )
                            HorizontalDivider(color = SettingsDivider, thickness = 1.dp)
                            SettingsRow(
                                title = "خروجی اطلاعات",
                                subtitle = "انتخاب بازه و محتوای فایل اکسل",
                                icon = Icons.Default.FileDownload,
                                onClick = { showExportSheet = true }
                            )
                            HorizontalDivider(color = SettingsDivider, thickness = 1.dp)
                            SettingsRow(
                                title = "خوانش پیامک",
                                subtitle = "ثبت خودکار تراکنش‌ها از پیامک بانکی",
                                icon = Icons.Default.Sms,
                                chip = if (smsEnabled) "فعال" else "غیرفعال",
                                chipColor = if (smsEnabled) SettingsGreen else SettingsMuted,
                                onClick = onNavigateToSmsReading
                            )
                            HorizontalDivider(color = SettingsDivider, thickness = 1.dp)
                            SettingsRow(
                                title = "مدیریت الگوهای پیامک",
                                subtitle = "فرستنده‌های شناسایی‌شده و قوانین ثبت پیامک",
                                icon = Icons.Default.List,
                                onClick = { showPatternSheet = true }
                            )
                            HorizontalDivider(color = SettingsDivider, thickness = 1.dp)
                            SettingsRow(
                                title = "درباره پولینو",
                                subtitle = "نسخه برنامه و معرفی پولینو",
                                icon = Icons.Default.Info,
                                onClick = { showAboutSheet = true }
                            )
                            HorizontalDivider(color = SettingsDivider, thickness = 1.dp)
                            SettingsRow(
                                title = "ارتباط با ما",
                                subtitle = "کانال پشتیبانی پولینو",
                                icon = Icons.Default.Email,
                                onClick = { showContactSheet = true }
                            )
                            // مرحله ۱۲۱: تست‌سنتر از نوار بالا به منوی تنظیمات آمد؛
                            // مثل قبل فقط در نسخه دیباگ دیده می‌شود.
                            if (BuildConfig.DEBUG) {
                                HorizontalDivider(color = SettingsDivider, thickness = 1.dp)
                                SettingsRow(
                                    title = "تست‌سنتر",
                                    subtitle = "خودآزمون محاسبات و بررسی سلامت برنامه",
                                    icon = Icons.Default.Science,
                                    onClick = onNavigateToTestCenter
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSecuritySheet) {
        SecuritySheet(
            biometricEnabled = lockEnabled,
            onBiometricChange = onLockChange,
            onDismiss = { showSecuritySheet = false }
        )
    }
    if (showBackupSheet) {
        BackupSheet(
            transactionRepository = repository,
            budgetRepository = budgetRepository,
            onRestored = { message = "بازیابی انجام شد." },
            onDismiss = { showBackupSheet = false }
        )
    }
    if (showAboutSheet) {
        AboutSheet(onDismiss = { showAboutSheet = false })
    }
    if (showContactSheet) {
        ContactSheet(onDismiss = { showContactSheet = false })
    }
    if (showPatternSheet) {
        SmsPatternSheet(
            patterns = patterns,
            detectedSenders = detectedSenders,
            smsDetectMessage = smsDetectMessage,
            onScanSenders = { runDetectSenders() },
            onOpenAppSettings = { openAppSettings(context) },
            smsPermissionPermanentlyDenied = smsPermissionPermanentlyDenied,
            onAddPattern = { sender, keywords, isFinancial, isIncome ->
                scope.launch {
                    repository.addPattern(
                        SmsPatternEntity(
                            id = UUID.randomUUID().toString(),
                            sender = sender,
                            keywords = keywords,
                            isFinancial = isFinancial,
                            isIncome = isIncome
                        )
                    )
                    patterns = repository.getPatterns()
                }
            },
            onEditPattern = { pattern, sender, keywords, isFinancial, isIncome ->
                scope.launch {
                    repository.addPattern(
                        pattern.copy(
                            sender = sender,
                            keywords = keywords,
                            isFinancial = isFinancial,
                            isIncome = isIncome
                        )
                    )
                    patterns = repository.getPatterns()
                }
            },
            onDeletePattern = { pattern ->
                scope.launch {
                    repository.deletePattern(pattern.id)
                    patterns = repository.getPatterns()
                }
            },
            onDismiss = { showPatternSheet = false }
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

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    chip: String? = null,
    chipColor: Color = SettingsMuted,
    onClick: () -> Unit
) {
    // مرحله ۱۱۴: ردیف داخل کارت گروهی — بدون کارت و حاشیه خودش؛ آیکون
    // خطی سفید بدون کاشی، عین پرگار (f_07).
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = title, tint = Color.White)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SettingsRowMuted)
        }
        if (chip != null) {
            Box(
                modifier = Modifier
                    .background(
                        chipColor.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    chip,
                    color = chipColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Icon(Icons.Default.ChevronLeft, contentDescription = "باز کردن", tint = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SettingsDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "درباره پولینو",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "پولینو",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "نسخه ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = SettingsMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "«پولت را بشناس، آینده‌ات را بساز»",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SettingsPurple)
            ) {
                Text("بستن", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SettingsDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "ارتباط با ما",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "برای پولینو هنوز کانال پشتیبانی درون‌برنامه‌ای تنظیم نشده است. " +
                    "هر وقت کانالی مشخص شد، همین‌جا نمایش داده می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = SettingsMuted
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SettingsPurple)
            ) {
                Text("بستن", fontWeight = FontWeight.Bold)
            }
        }
    }
}
