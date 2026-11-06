package com.polino.app.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.polino.app.data.TransactionRepository
import com.polino.app.sms.SMS_ENABLED_KEY
import com.polino.app.sms.SMS_PREFS_NAME
import com.polino.app.sms.isReadSmsPermanentlyDenied
import com.polino.app.sms.openAppSettings
import com.polino.app.sms.rescanInboxSms
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// مرحله ۱۱۸: رنگ‌های صفحه خوانش پیامک — همان مقادیر نمونه‌برداری‌شده
// از پرگار در مرحله ۱۱۴ (کارت گروهی تنظیمات) + سبز کاشی پیامک هاب.
private val SmsReadingCardBg = Color(0xFF121532)
private val SmsReadingBorder = Color(0xFF292147)
private val SmsReadingMuted = Color(0xFFC0BDDD)
private val SmsReadingPurple = Color(0xFFA855F7)
private val SmsReadingGreen = Color(0xFF64C0AB)
private val SmsReadingRed = Color(0xFFFB7185)
private val SmsReadingTileGreen = Color(0xFF22C55E)

/**
 * صفحه اختصاصی «خوانش پیامک‌های بانکی» عین صفحه پرگار: عنوان و توضیح،
 * کارت وضعیت (فعال/غیرفعال)، کارت حفظ حریم خصوصی (متن خام پیامک
 * هرگز ذخیره نمی‌شود؛ پردازش کاملاً آفلاین درون گوشی)، دکمه «بررسی
 * مجدد پیامک‌ها» و دکمه فعال/غیرفعال کردن خوانش. وضعیت فعال بودن با
 * همان کلید SMS_ENABLED_KEY با کارت پیامک هاب مدیریت مشترک است.
 *
 * علت خرابی قبلی اسکن: سوییچ هاب فقط دسترسی RECEIVE_SMS را می‌خواست،
 * پس خوانش می‌توانست «فعال» باشد در حالی که READ_SMS هنوز داده نشده
 * بود؛ و اگر READ_SMS پیش‌تر به‌طور دائم رد شده بود، درخواست دوباره
 * بی‌صدا رد می‌شد و تنها بازخورد یک خط کوچک کم‌رنگ بود — کاربر
 * «هیچ اتفاقی نمی‌افتد» می‌دید. این صفحه هر دو را درست می‌کند: وقتی
 * دسترسی دائماً رد شده، کارت واضح با دکمه باز کردن تنظیمات برنامه
 * نشان می‌دهد؛ و اسکن بدون بازخورد و بدون مهلت نمی‌ماند.
 *
 * بازخورد «بررسی پیامک‌های اخیر آغاز شد» فقط خبر شروع یک اسکن واقعی
 * است، نه جای آن: همان لحظه توست نشان داده می‌شود و اسکن واقعاً در
 * کرون زیر اجرا می‌شود؛ اگر اسکن در ۳۰ ثانیه تمام نشد، به‌جای سکوت
 * خطای واقعی نشان داده می‌شود. (حلقه خواندن صندوق نقطه تعلیق ندارد،
 * پس پس از مهلت ممکن است در پس‌زمینه تمام شود؛ نتیجه‌اش دور انداخته
 * می‌شود و تلاش دوباره کار می‌کند.)
 */
@Composable
internal fun SmsReadingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val scope = rememberCoroutineScope()
    var smsEnabled by remember { mutableStateOf(false) }
    var readSmsGranted by remember { mutableStateOf(false) }
    var smsPermissionPermanentlyDenied by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var pendingRescan by remember { mutableStateOf(false) }

    fun refreshSmsState() {
        smsEnabled = context.getSharedPreferences(SMS_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SMS_ENABLED_KEY, false)
        readSmsGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        smsPermissionPermanentlyDenied = isReadSmsPermanentlyDenied(context)
    }

    // اسکن واقعی را شروع می‌کند؛ فقط وقتی صدا زده می‌شود که دسترسی
    // داده شده باشد. بازخورد فوری همان لحظه نشان داده می‌شود و نتیجه
    // واقعی اسکن جای آن می‌نشیند.
    fun startRescan() {
        message = "بررسی پیامک‌های اخیر آغاز شد"
        error = ""
        scanning = true
        Toast.makeText(context, "بررسی پیامک‌های اخیر آغاز شد", Toast.LENGTH_SHORT).show()
        scope.launch {
            val inserted = withTimeoutOrNull(30_000L) {
                rescanInboxSms(context, repository)
            }
            scanning = false
            if (inserted == null) {
                message = ""
                error = "بررسی پیامک طول کشید؛ دوباره تلاش کنید."
            } else if (inserted > 0) {
                message = "${persianNumber(inserted)} تراکنش جدید ثبت شد."
            } else {
                message = "پیامک تازه‌ای پیدا نشد."
            }
        }
    }

    val readSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            pendingRescan = false
            error = "دسترسی خواندن پیامک داده نشده است."
            refreshSmsState()
            return@rememberLauncherForActivityResult
        }
        readSmsGranted = true
        smsPermissionPermanentlyDenied = false
        if (pendingRescan) {
            pendingRescan = false
            startRescan()
        }
    }
    val receiveSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            context.getSharedPreferences(SMS_PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(SMS_ENABLED_KEY, true).apply()
            smsEnabled = true
        } else {
            error = "برای فعال کردن خوانش پیامک، دسترسی دریافت پیامک لازم است."
        }
    }

    fun runRealRescan() {
        // دسترسی در لحظه ضربه خوانده می‌شود، نه از حالت ذخیره‌شده؛
        // اگر کاربر در تنظیمات سیستم دسترسی را داده و برگشته باشد،
        // اسکن همان‌جا واقعاً اجرا می‌شود.
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            readSmsGranted = true
            smsPermissionPermanentlyDenied = false
            startRescan()
        } else {
            readSmsGranted = false
            pendingRescan = true
            readSmsLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    fun setSmsEnabled(checked: Boolean) {
        val prefs = context.getSharedPreferences(SMS_PREFS_NAME, Context.MODE_PRIVATE)
        if (checked) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                prefs.edit().putBoolean(SMS_ENABLED_KEY, true).apply()
                smsEnabled = true
            } else {
                receiveSmsLauncher.launch(Manifest.permission.RECEIVE_SMS)
            }
        } else {
            prefs.edit().putBoolean(SMS_ENABLED_KEY, false).apply()
            smsEnabled = false
        }
    }

    LaunchedEffect(Unit) {
        refreshSmsState()
    }

    // مرحله ۱۱۴: بازگشت با دکمه سیستمی؛ این صفحه هم مثل تنظیمات یک
    // پوشش تمام‌صفحه است و بدون BackHandler کاربر در آن گیر می‌افتد.
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(SmsReadingTileGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    "خوانش پیامک‌های بانکی",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    "اگر این قابلیت را فعال کنید، پولینو می‌تواند پیامک‌های بانکی را بررسی کند و اطلاعات تراکنش‌های شما را به‌صورت خودکار استخراج و ثبت کند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SmsReadingCardBg),
                border = BorderStroke(1.dp, SmsReadingBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (smsEnabled) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                        contentDescription = null,
                        tint = if (smsEnabled) SmsReadingGreen else SmsReadingMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (smsEnabled) "وضعیت: فعال" else "وضعیت: غیرفعال",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (smsEnabled) SmsReadingGreen else Color.White
                        )
                        Text(
                            if (smsEnabled) "خوانش پیامک فعال است و تراکنش‌های بانکی به‌طور خودکار ثبت می‌شوند."
                            else "خوانش پیامک غیرفعال است؛ تراکنش‌های بانکی به‌صورت خودکار ثبت نمی‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmsReadingMuted
                        )
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SmsReadingCardBg),
                border = BorderStroke(1.dp, SmsReadingBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = SmsReadingPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "حفظ حریم خصوصی شما در پیامک",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        "متن خام پیامک‌ها در پولینو ذخیره نمی‌شود؛ پیامک فقط برای شناسایی اطلاعات تراکنش پردازش می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmsReadingMuted
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = SmsReadingGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "پردازش کاملاً آفلاین درون گوشی، بدون ارسال به هیچ سرور خارجی",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmsReadingMuted
                        )
                    }
                }
            }
        }
        if (!readSmsGranted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SmsReadingCardBg),
                    border = BorderStroke(1.dp, SmsReadingRed)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = SmsReadingRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                if (smsPermissionPermanentlyDenied) "دسترسی خواندن پیامک رد شده است"
                                else "دسترسی خواندن پیامک لازم است",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            if (smsPermissionPermanentlyDenied) "سیستم دیگر پنجره درخواست نشان نمی‌دهد؛ برای بررسی پیامک‌ها، دسترسی را از تنظیمات برنامه فعال کنید."
                            else "برای بررسی پیامک‌های اخیر، دسترسی خواندن پیامک لازم است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SmsReadingMuted
                        )
                        if (smsPermissionPermanentlyDenied) {
                            OutlinedButton(
                                onClick = { openAppSettings(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("باز کردن تنظیمات برنامه")
                            }
                        } else {
                            Button(
                                onClick = { runRealRescan() },
                                colors = ButtonDefaults.buttonColors(containerColor = SmsReadingPurple),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text("دادن دسترسی خواندن پیامک", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (message.isNotEmpty()) {
            item {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmsReadingGreen
                )
            }
        }
        if (error.isNotEmpty()) {
            item {
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmsReadingRed
                )
            }
        }
        item {
            Button(
                onClick = { runRealRescan() },
                enabled = !scanning,
                colors = ButtonDefaults.buttonColors(containerColor = SmsReadingPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    if (scanning) "در حال بررسی…" else "بررسی مجدد پیامک‌ها",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            if (smsEnabled) {
                OutlinedButton(
                    onClick = { setSmsEnabled(false) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SmsReadingRed),
                    border = BorderStroke(1.dp, SmsReadingRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = SmsReadingRed,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("غیرفعال کردن خوانش پیامک", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { setSmsEnabled(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = SmsReadingPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("فعال کردن خوانش پیامک", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
