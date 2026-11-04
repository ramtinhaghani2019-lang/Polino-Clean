package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.polino.app.data.DebtEntity
import com.polino.app.data.TransactionRepository
import com.polino.app.data.isDebtOverdue
import com.polino.app.data.unsettledTotals
import java.util.UUID
import kotlinx.coroutines.launch

/** رنگ‌های صفحه و ورق تیره طلب و بدهی، به زبان ورق‌های تیره مرحله‌های ۶۹ تا ۸۳. */
private val DebtSheetDark = Color(0xFF1E1438)
private val DebtSheetCard = Color(0xFF2A2150)
private val DebtSheetBorder = Color(0xFF3D3368)
private val DebtSheetMuted = Color(0xFFB3ACD6)
private val DebtPurple = Color(0xFFA855F7)
private val DebtGreen = Color(0xFF34C89E)
private val DebtRed = Color(0xFFF87171)
private val DebtTabReceivableBg = Color(0xFF1E3A32)
private val DebtTabPayableBg = Color(0xFF3D1F1F)
private val DebtAddReceivable = Color(0xFF16A34A)
private val DebtAddPayable = Color(0xFFDC2626)

/**
 * فیلتر طلب/بدهی‌ها برای صفحه اختصاصی: زبانه طلب/بدهی + جستجوی نام شخص
 * یا مبلغ (ارقام فارسی/عربی هم با normalizeDigits هم‌ارز می‌شوند).
 * بدهی‌های تسویه‌شده هم در فهرست می‌مانند و با برچسب «تسویه شد» مشخص‌اند.
 */
internal fun filterDebts(
    debts: List<DebtEntity>,
    query: String,
    isReceivable: Boolean
): List<DebtEntity> {
    val trimmed = query.trim()
    val digits = normalizeDigits(trimmed).filter { it.isDigit() }
    return debts.filter { debt ->
        debt.isReceivable == isReceivable &&
            (trimmed.isEmpty() ||
                debt.personName.contains(trimmed, ignoreCase = true) ||
                (digits.isNotEmpty() && debt.amount.toString().contains(digits)))
    }
}

/**
 * سررسید امروز است؟ همان قرارداد روزمحور isDebtOverdue: سررسید تاریخ
 * است نه ساعت، پس بازه روز کامل ملاک است.
 */
internal fun isDebtDueToday(dueMillis: Long?, nowMillis: Long): Boolean {
    if (dueMillis == null) return false
    val startOfToday = nowMillis - Math.floorMod(nowMillis, 86_400_000L)
    return dueMillis in startOfToday until startOfToday + 86_400_000L
}

/**
 * صفحه اختصاصی «طلب و بدهی»، عین عکس‌های پرگار: جستجو، زبانه‌های
 * «طلب از دیگران» / «بدهی به دیگران»، کارت حالت خالی با دکمه ثبت،
 * و ورق تیره ثبت/ویرایش با سررسید اجباریِ پیش‌فرض امروز.
 * تسویه و حذف از همان منطق موجود TransactionRepository استفاده می‌کنند؛
 * هیچ منطقی دوباره ساخته نشده است.
 */
@Composable
internal fun DebtsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    var debts by remember { mutableStateOf<List<DebtEntity>>(emptyList()) }
    var showReceivable by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<DebtEntity?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        debts = runCatching { repository.getDebts() }.getOrDefault(emptyList())
    }

    val (receivableTotal, payableTotal) = unsettledTotals(debts)
    val visible = filterDebts(debts, query, showReceivable)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) { Text("بازگشت") }
            Text("طلب و بدهی", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("طلب‌های باز: ${money(receivableTotal)} — بدهی‌های باز: ${money(payableTotal)}")
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجوی نام شخص یا مبلغ…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "جستجو")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DebtSheetDark),
                    border = BorderStroke(1.dp, DebtSheetBorder)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Button(
                            onClick = { showReceivable = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showReceivable) DebtTabReceivableBg else Color.Transparent,
                                contentColor = if (showReceivable) DebtGreen else DebtSheetMuted
                            )
                        ) { Text("طلب از دیگران") }
                        Button(
                            onClick = { showReceivable = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!showReceivable) DebtTabPayableBg else Color.Transparent,
                                contentColor = if (!showReceivable) DebtRed else DebtSheetMuted
                            )
                        ) { Text("بدهی به دیگران") }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        editTarget = null
                        showSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showReceivable) DebtAddReceivable else DebtAddPayable,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (showReceivable) "+ ثبت طلب" else "+ ثبت بدهی")
                }
            }
            if (visible.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DebtSheetDark),
                        border = BorderStroke(1.dp, DebtSheetBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(BorderStroke(1.dp, DebtSheetBorder), RoundedCornerShape(28.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (showReceivable) DebtGreen else DebtRed
                                )
                            }
                            Text(
                                if (showReceivable) "هیچ طلبی ثبت نشده است" else "هیچ بدهی ثبت نشده است",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                if (showReceivable) {
                                    "برای پیگیری مطالبات و تاریخ وصول، طلب جدیدی ثبت کنید."
                                } else {
                                    "برای پیگیری بدهی‌ها و تاریخ پرداخت، بدهی جدیدی ثبت کنید."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = DebtSheetMuted
                            )
                            Button(
                                onClick = {
                                    editTarget = null
                                    showSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showReceivable) DebtAddReceivable else DebtAddPayable,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(if (showReceivable) "+ ثبت طلب" else "+ ثبت بدهی")
                            }
                        }
                    }
                }
            } else {
                visible.forEach { debt ->
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
                                val overdue = isDebtOverdue(
                                    debt.dueMillis, System.currentTimeMillis(), debt.settled
                                )
                                val dueToday = isDebtDueToday(
                                    debt.dueMillis, System.currentTimeMillis()
                                )
                                Text(
                                    debt.personName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (overdue) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    "${if (debt.isReceivable) "طلب" else "بدهی"}: ${money(debt.amount)}" +
                                        if (debt.dueMillis != null) {
                                            if (dueToday) " — سررسید امروز"
                                            else " — سررسید: ${JalaliDate.formatJalali(debt.dueMillis)}"
                                        } else "" +
                                        if (overdue) " — سررسید گذشته" else "" +
                                        if (debt.settled) " — تسویه شد" else ""
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.settleDebt(debt)
                                                debts = repository.getDebts()
                                            }
                                        },
                                        enabled = !debt.settled
                                    ) {
                                        Text(if (debt.settled) "تسویه شد" else "تسویه")
                                    }
                                    TextButton(
                                        onClick = {
                                            editTarget = debt
                                            showSheet = true
                                        }
                                    ) {
                                        Text("ویرایش")
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteDebt(debt.id)
                                                debts = repository.getDebts()
                                            }
                                        }
                                    ) {
                                        Text("حذف")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        DebtSheet(
            initial = editTarget,
            onDismiss = {
                showSheet = false
                editTarget = null
            },
            onSave = { person, amount, isReceivable, dueMillis ->
                scope.launch {
                    val target = editTarget
                    repository.addDebt(
                        DebtEntity(
                            id = target?.id ?: UUID.randomUUID().toString(),
                            personName = person,
                            amount = amount,
                            isReceivable = isReceivable,
                            settled = target?.settled ?: false,
                            accountId = target?.accountId ?: "main",
                            dueMillis = dueMillis
                        )
                    )
                    debts = repository.getDebts()
                    showSheet = false
                    editTarget = null
                }
            }
        )
    }
}

@Composable
private fun DebtMiniChip(text: String, borderColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun debtSheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = DebtSheetCard,
    unfocusedContainerColor = DebtSheetCard,
    focusedBorderColor = DebtSheetBorder,
    unfocusedBorderColor = DebtSheetBorder,
    cursorColor = Color.White
)

/**
 * ورق «ثبت طلب / بدهی»، عین فرم پرگار: ضامن X، گزینش «طلب (از دیگران)» /
 * «بدهی (به دیگران)» با رنگ سبز/قرمز، نام شخص و مبلغ و تاریخ سررسید هر
 * سه با نشان «اجباری»، نشان «کیپد فارسی» کنار مبلغ، سررسید پیش‌فرض امروز،
 * دکمه‌های انصراف و ذخیره. فرم پرگار ردیف حساب نشان نمی‌دهد؛ حساب تراکنش
 * تسویه همان حساب قبلی (ویرایش) یا «main» (ثبت تازه) می‌ماند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtSheet(
    initial: DebtEntity?,
    onDismiss: () -> Unit,
    onSave: (person: String, amount: Long, isReceivable: Boolean, dueMillis: Long) -> Unit
) {
    var person by remember { mutableStateOf(initial?.personName ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var isReceivable by remember { mutableStateOf(initial?.isReceivable ?: true) }
    var dueMillis by remember { mutableStateOf(initial?.dueMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueMillis)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DebtSheetDark
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
                        if (initial == null) "ثبت طلب / بدهی" else "ویرایش طلب / بدهی",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isReceivable = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReceivable) DebtTabReceivableBg else DebtSheetCard,
                            contentColor = if (isReceivable) DebtGreen else DebtSheetMuted
                        )
                    ) { Text("طلب (از دیگران)") }
                    Button(
                        onClick = { isReceivable = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isReceivable) DebtTabPayableBg else DebtSheetCard,
                            contentColor = if (!isReceivable) DebtRed else DebtSheetMuted
                        )
                    ) { Text("بدهی (به دیگران)") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نام شخص", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        DebtMiniChip(
                            "اجباری",
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Person, contentDescription = null, tint = DebtPurple)
                    }
                    OutlinedTextField(
                        value = person,
                        onValueChange = { person = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = debtSheetFieldColors()
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
                        DebtMiniChip(
                            "اجباری",
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error
                        )
                        DebtMiniChip("کیپد فارسی", DebtSheetBorder, DebtSheetMuted)
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("تومان", color = DebtPurple) },
                        colors = debtSheetFieldColors()
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
                        DebtMiniChip(
                            "اجباری",
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "انتخاب تاریخ سررسید",
                                tint = DebtPurple
                            )
                        }
                    }
                    Text(
                        if (isDebtDueToday(dueMillis, System.currentTimeMillis())) {
                            "امروز"
                        } else {
                            JalaliDate.formatJalali(dueMillis)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) { Text("انصراف") }
                    Button(
                        onClick = {
                            val amount = normalizeDigits(amountText).toLongOrNull() ?: 0L
                            if (person.isNotBlank() && amount > 0) {
                                onSave(person.trim(), amount, isReceivable, dueMillis)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DebtPurple,
                            contentColor = Color.White
                        )
                    ) { Text("ذخیره") }
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
                TextButton(onClick = { showDatePicker = false }) { Text("لغو") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
