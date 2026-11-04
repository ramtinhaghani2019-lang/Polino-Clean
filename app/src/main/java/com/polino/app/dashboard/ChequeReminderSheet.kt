package com.polino.app.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.polino.app.data.ChequeReminderEntity

private val ChequeSheetDark = Color(0xFF1E1438)
private val ChequeSheetCard = Color(0xFF2A2150)
private val ChequeSheetBorder = Color(0xFF3D3368)
private val ChequeSheetMuted = Color(0xFFB3ACD6)
private val ChequePurple = Color(0xFFA855F7)

/**
 * ورق تیره افزودن/ویرایش یادآور چک و قبض، هم‌خوان با عکس پرگار:
 * دکمه بستن X، عنوان + نشان اجباری، مبلغ + نشان اجباری + نشان
 * «کیپد فارسی» (فقط برچسب، مثل ورق اقساط)، سررسید + نشان اجباری با
 * پیش‌فرض امروز و نمایش «امروز» برای سررسید امروز، توضیحات اختیاری،
 * و یک دکمه سراسری «ذخیره یادآور» (بدون انصراف؛ X می‌بندد).
 * تسویه اینجا انجام نمی‌شود؛ تسویه مسیر موجود settleCheque است که
 * تراکنشی ثبت نمی‌کند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChequeReminderSheet(
    existing: ChequeReminderEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, dueMillis: Long, note: String) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var dueMillis by remember { mutableStateOf(existing?.dueMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueMillis)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val amount = normalizeDigits(amountText).toLongOrNull() ?: 0L
    val canSave = title.isNotBlank() && amount > 0
    val dueIsToday = chequeDueStatus(dueMillis, System.currentTimeMillis(), false) ==
        ChequeDueStatus.DUE_TODAY

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ChequeSheetDark
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (existing == null) "افزودن یادآور چک / قبض" else "ویرایش یادآور",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عنوان", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                "اجباری",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("عنوان (مثلاً چک صیادی یا قبض برق)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ChequeSheetBorder,
                            unfocusedBorderColor = ChequeSheetBorder,
                            focusedPlaceholderColor = ChequeSheetMuted,
                            unfocusedPlaceholderColor = ChequeSheetMuted
                        )
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مبلغ (تومان)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                "اجباری",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ChequeSheetCard
                        ) {
                            Text(
                                "کیپد فارسی",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChequeSheetMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("مبلغ (تومان)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ChequeSheetBorder,
                            unfocusedBorderColor = ChequeSheetBorder,
                            focusedPlaceholderColor = ChequeSheetMuted,
                            unfocusedPlaceholderColor = ChequeSheetMuted
                        )
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تاریخ سررسید", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                "اجباری",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "انتخاب تاریخ سررسید",
                                tint = ChequePurple
                            )
                        }
                    }
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(
                            if (dueIsToday) "امروز" else JalaliDate.formatJalali(dueMillis),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("توضیحات (اختیاری)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ChequeSheetBorder,
                            unfocusedBorderColor = ChequeSheetBorder
                        )
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        if (canSave) onSave(title.trim(), amount, dueMillis, note.trim())
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChequePurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("ذخیره یادآور", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueMillis = it }
                        showDatePicker = false
                    }
                ) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("انصراف") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
