package com.polino.app.sms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polino.app.dashboard.persianNumber
import com.polino.app.data.SmsPatternEntity

private val PatternSheetDark = Color(0xFF1E1438)
private val PatternSheetKey = Color(0xFF2A2150)
private val PatternSheetMuted = Color(0xFFB3ACD6)
private val PatternSheetPurple = Color(0xFFA855F7)
private val PatternSheetGreen = Color(0xFF64C0AB)
private val PatternSheetRed = Color(0xFFFB7185)

/**
 * ورق «مدیریت الگوهای پیامک» عین ورق پرگار: عنوان + زیرنویس (فرستنده‌ها،
 * کلمات کلیدی و قوانین تراکنش) + دکمه بستن X، بخش «فرستنده‌های پیامک
 * شناسایی‌شده در دستگاه» با چیپ تعداد پیامک هر فرستنده (لمس هر فرستنده
 * فرم افزودن را با همان فرستنده باز می‌کند)، دکمه بنفش «+ افزودن الگوی
 * جدید»، بخش «الگوها و قوانین ثبت‌شده» با ویرایش و حذف هر الگو، یادداشت
 * الگوهای ثبت‌شده + یادداشت فعال‌بودن پارس‌های شتاب، و دکمه بنفش «بستن».
 * فرم افزودن/ویرایش همان ورق را می‌گیرد (عین فرم بازشونده پرگار): فیلد
 * فرستنده با چیپ «اجباری»، فیلد چندخطی کلمات کلیدی، سوییچ مالی‌بودن،
 * دکمه‌های قرمز هزینه / سبز درآمد، «انصراف» + «ذخیره الگو». منطق تطابق
 * الگو، پارس‌های شتاب، اتصال پیامک به حساب و موتور اسکن دست نخورده‌اند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SmsPatternSheet(
    patterns: List<SmsPatternEntity>,
    detectedSenders: List<DetectedSender>?,
    smsDetectMessage: String,
    onScanSenders: () -> Unit,
    onAddPattern: (sender: String, keywords: String, isFinancial: Boolean, isIncome: Boolean) -> Unit,
    onEditPattern: (pattern: SmsPatternEntity, sender: String, keywords: String, isFinancial: Boolean, isIncome: Boolean) -> Unit,
    onDeletePattern: (pattern: SmsPatternEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SmsPatternEntity?>(null) }
    var formSender by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PatternSheetDark,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PatternSheetPurple, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    "مدیریت الگوهای پیامک",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                }
            }
            Text(
                "تعیین فرستنده‌ها، کلمات کلیدی و قوانین تراکنش",
                style = MaterialTheme.typography.bodySmall,
                color = PatternSheetMuted
            )
            if (!showForm) {
                Text(
                    "فرستنده‌های پیامک شناسایی‌شده در دستگاه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        detectedSenders == null -> {
                            OutlinedButton(onClick = onScanSenders) {
                                Text("اسکن صندوق پیامک")
                            }
                        }
                        detectedSenders.isEmpty() -> {
                            Text(
                                "فرستنده‌ای در صندوق پیامک پیدا نشد.",
                                color = Color.White
                            )
                            OutlinedButton(onClick = onScanSenders) {
                                Text("اسکن دوباره")
                            }
                        }
                        else -> {
                            detectedSenders.forEachIndexed { index, sender ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.08f),
                                        thickness = 1.dp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editing = null
                                            formSender = sender.sender
                                            showForm = true
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        sender.sender,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = PatternSheetKey
                                    ) {
                                        Text(
                                            "${persianNumber(sender.count)} پیامک",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedButton(onClick = onScanSenders) {
                                Text("اسکن دوباره")
                            }
                        }
                    }
                    if (smsDetectMessage.isNotEmpty()) {
                        Text(
                            smsDetectMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = PatternSheetMuted
                        )
                    }
                }
                Button(
                    onClick = {
                        editing = null
                        formSender = ""
                        showForm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PatternSheetPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("+ افزودن الگوی جدید", fontWeight = FontWeight.Bold)
                }
                Text(
                    "الگوها و قوانین ثبت‌شده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (patterns.isEmpty()) {
                    Text(
                        "الگوی سفارشی هنوز ثبت نشده است.",
                        color = Color.White
                    )
                } else {
                    patterns.forEach { pattern ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PatternSheetKey),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    pattern.sender,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                if (pattern.keywords.isNotBlank()) {
                                    Text(
                                        "کلمات کلیدی: ${pattern.keywords}",
                                        color = Color.White
                                    )
                                }
                                Text(
                                    if (pattern.isIncome) "درآمد" else "هزینه",
                                    color = Color.White
                                )
                                if (!pattern.isFinancial) {
                                    Text(
                                        "غیرمالی — ثبت خودکار نمی‌شود",
                                        color = Color.White
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            editing = pattern
                                            formSender = pattern.sender
                                            showForm = true
                                        }
                                    ) {
                                        Text("ویرایش")
                                    }
                                    TextButton(onClick = { onDeletePattern(pattern) }) {
                                        Text("حذف")
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "پارس‌های بانک‌های عضو شتاب فعال می‌باشند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PatternSheetMuted
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PatternSheetPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("بستن", fontWeight = FontWeight.Bold)
                }
            } else {
                key(editing?.id ?: ("new:" + formSender)) {
                    PatternForm(
                        initialSender = editing?.sender ?: formSender,
                        initialKeywords = editing?.keywords ?: "",
                        initialIsFinancial = editing?.isFinancial ?: true,
                        initialIsIncome = editing?.isIncome ?: false,
                        editingTitle = editing?.let { "ویرایش الگوی فرستنده: ${it.sender}" }
                            ?: "تعریف الگوی جدید برای: فرستنده سفارشی",
                        onCancel = { showForm = false },
                        onSave = { sender, keywords, isFinancial, isIncome ->
                            val target = editing
                            if (target != null) {
                                onEditPattern(target, sender, keywords, isFinancial, isIncome)
                            } else {
                                onAddPattern(sender, keywords, isFinancial, isIncome)
                            }
                            showForm = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternForm(
    initialSender: String,
    initialKeywords: String,
    initialIsFinancial: Boolean,
    initialIsIncome: Boolean,
    editingTitle: String,
    onCancel: () -> Unit,
    onSave: (sender: String, keywords: String, isFinancial: Boolean, isIncome: Boolean) -> Unit
) {
    var sender by remember { mutableStateOf(initialSender) }
    var keywords by remember { mutableStateOf(initialKeywords) }
    var isFinancial by remember { mutableStateOf(initialIsFinancial) }
    var isIncome by remember { mutableStateOf(initialIsIncome) }
    val valid = sender.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            editingTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "نام فرستنده یا سرشماره",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PatternSheetRed
                ) {
                    Text(
                        "اجباری",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PatternSheetMuted,
                    unfocusedBorderColor = PatternSheetMuted,
                    cursorColor = Color.White
                )
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "کلمات کلیدی (با کاما جدا کنید)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            OutlinedTextField(
                value = keywords,
                onValueChange = { keywords = it },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PatternSheetMuted,
                    unfocusedBorderColor = PatternSheetMuted,
                    cursorColor = Color.White
                )
            )
            Text(
                "مثل: برداشت، خرید",
                style = MaterialTheme.typography.bodySmall,
                color = PatternSheetMuted
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(checked = isFinancial, onCheckedChange = { isFinancial = it })
            Text(
                "آیا این پیامک تراکنش مالی است؟",
                color = Color.White
            )
            Text(
                if (isFinancial) "بله" else "خیر",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { isIncome = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isIncome) PatternSheetRed else PatternSheetKey,
                    contentColor = if (!isIncome) Color.Black else Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text("هزینه", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { isIncome = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncome) PatternSheetGreen else PatternSheetKey,
                    contentColor = if (isIncome) Color.Black else Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text("درآمد", fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text("انصراف")
            }
            Button(
                onClick = { onSave(sender.trim(), keywords.trim(), isFinancial, isIncome) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = PatternSheetPurple),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text("ذخیره الگو", fontWeight = FontWeight.Bold)
            }
        }
    }
}
