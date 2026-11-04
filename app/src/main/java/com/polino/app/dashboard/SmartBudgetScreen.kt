package com.polino.app.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.polino.app.data.BudgetEntity
import com.polino.app.data.BudgetRepository
import com.polino.app.data.CategoryEntity
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.polino.app.data.nextJalaliYearMonth
import kotlinx.coroutines.launch

/**
 * صفحه اختصاصی بودجه‌بندی هوشمند: نوار ماه تک‌قرصی با نشان «ماه جاری»،
 * فهرست بودجه‌های ماه با پیشرفت خرج در برابر سقف، کارت خالی آیکن‌دار، و
 * ورق تیره «بودجه جدید» با دسته (اجباری، بدون مقدار پیش‌فرض) و سقف ماهانه
 * (اجباری) و ورق انتخاب دسته با همه انواع مخلوط.
 * داده‌ها همان BudgetRepository سراسری‌اند؛ فروشگاه دومی ساخته نمی‌شود.
 */
@Composable
fun SmartBudgetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val transactionRepository = remember { TransactionRepository(context) }
    val budgetRepository = remember { BudgetRepository(context) }
    val scope = rememberCoroutineScope()
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var budgets by remember { mutableStateOf<List<BudgetEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var selectedYearMonth by remember { mutableStateOf(JalaliDate.currentJalaliYearMonth()) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            transactions = transactionRepository.getAll()
            categories = transactionRepository.getCategories()
        }
    }

    LaunchedEffect(selectedYearMonth) {
        budgets = budgetRepository.getBudgets(selectedYearMonth)
    }

    /** پیشرفت بودجه همیشه نسبت به کل ماه سنجیده می‌شود، نه حساب فیلترشده. */
    val spentAll = spentByCategory(transactionsInMonth(transactions, selectedYearMonth))
    val isCurrentMonth = selectedYearMonth == JalaliDate.currentJalaliYearMonth()
    val totalCap = budgets.sumOf { it.limitAmount }
    val totalSpent = budgets.sumOf { budget ->
        spentAll.firstOrNull { it.first == budget.category }?.second ?: 0L
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) { Text("بازگشت") }
            Text("بودجه‌بندی هوشمند", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BudgetMonthPillBar(
                    monthDisplay = JalaliDate.jalaliMonthDisplay(selectedYearMonth),
                    isCurrentMonth = isCurrentMonth,
                    onPrevious = {
                        selectedYearMonth = JalaliDate.previousJalaliYearMonth(selectedYearMonth)
                    },
                    onNext = { selectedYearMonth = nextJalaliYearMonth(selectedYearMonth) }
                )
            }
            if (budgets.isNotEmpty()) {
                item {
                    Text(
                        "مجموع سقف‌ها: ${money(totalCap)} — خرج شده: ${money(totalSpent)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (budgets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
                        border = BorderStroke(1.dp, Color(0xFF3D3368))
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BudgetDonutIcon()
                            Text(
                                "هنوز برای ${JalaliDate.jalaliMonthDisplay(selectedYearMonth)} بودجه‌ای تعریف نشده است.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "می‌توانید برای دسته‌های مختلف (مانند خوراک، حمل و نقل یا تفریح) سقف مشخصی برای این ماه تعیین کنید.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB3ACD6),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            budgets.forEach { budget ->
                val spentAmount = spentAll.firstOrNull { it.first == budget.category }?.second ?: 0L
                val remaining = budget.limitAmount - spentAmount
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(budget.category, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    scope.launch {
                                        budgetRepository.deleteBudget(selectedYearMonth, budget.category)
                                        budgets = budgetRepository.getBudgets(selectedYearMonth)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف بودجه")
                                }
                            }
                            Text("خرج شده: ${money(spentAmount)} از ${money(budget.limitAmount)}")
                            LinearProgressIndicator(
                                progress = {
                                    if (budget.limitAmount > 0L) {
                                        (spentAmount.toFloat() / budget.limitAmount.toFloat())
                                            .coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                if (remaining >= 0) {
                                    "باقی‌مانده: ${money(remaining)}"
                                } else {
                                    "بیشتر از بودجه: ${money(-remaining)}"
                                },
                                color = if (remaining >= 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA855F7),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "+ بودجه جدید برای ${JalaliDate.jalaliMonthDisplay(selectedYearMonth)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    if (showDialog) {
        SmartBudgetSheet(
            allCategories = allBudgetCategoryNames(categories),
            monthDisplay = JalaliDate.jalaliMonthDisplay(selectedYearMonth),
            onDismiss = { showDialog = false },
            onSave = { category, limit ->
                scope.launch {
                    budgetRepository.setBudget(selectedYearMonth, category, limit)
                    budgets = budgetRepository.getBudgets(selectedYearMonth)
                    showDialog = false
                }
            }
        )
    }
}

/**
 * فهرست دسته‌های قابل‌بودجه در ورق انتخاب: همه دسته‌های اصلی کاربر، به
 * ترتیب ذخیره‌شان — هزینه، درآمد و سرمایه‌گذاری با هم، مثل ورق پرگار.
 * زیردسته‌ها در این فهرست نیستند.
 */
internal fun allBudgetCategoryNames(categories: List<CategoryEntity>): List<String> =
    categories.filter { it.parentId == null }.map { it.name }

/** دکمه «ماه بعد» در نوار ماه فقط وقتی ماه جاری انتخاب شده غیرفعال است. */
internal fun isBudgetMonthNextDisabled(selectedYearMonth: String, currentYearMonth: String): Boolean =
    selectedYearMonth == currentYearMonth

/** نوار ماه تک‌قرصی پرگار: قرص تیره با دو پیکان و نشان «ماه جاری». */
@Composable
private fun BudgetMonthPillBar(
    monthDisplay: String,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
        border = BorderStroke(1.dp, Color(0xFF3D3368))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "ماه قبل",
                    tint = Color.White
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    monthDisplay,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (isCurrentMonth) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3D3368)
                    ) {
                        Text(
                            "ماه جاری",
                            color = Color(0xFFC084FC),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = onNext,
                enabled = !isCurrentMonth
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "ماه بعد",
                    tint = Color.White
                )
            }
        }
    }
}

/** آیکن دونات کارت خالی بودجه: حلقه بنفش روی زمینه تیره. */
@Composable
private fun BudgetDonutIcon() {
    Canvas(modifier = Modifier.size(72.dp)) {
        drawArc(
            color = Color(0xFF3D3368),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 18.dp.toPx())
        )
        drawArc(
            color = Color(0xFFA855F7),
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 18.dp.toPx())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartBudgetSheet(
    allCategories: List<String>,
    monthDisplay: String,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Long) -> Unit
) {
    var category by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1438)
    ) {
        if (showPicker) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "انتخاب دسته‌بندی",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        IconButton(onClick = { showPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }
                    }
                }
                if (allCategories.isEmpty()) {
                    item {
                        Text(
                            "دسته‌ای وجود ندارد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB3ACD6)
                        )
                    }
                }
                allCategories.forEach { name ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    category = name
                                    showPicker = false
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
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
                            "بودجه جدید ($monthDisplay)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "دسته‌بندی",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
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
                }
                item {
                    OutlinedButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            category ?: "انتخاب دسته‌بندی",
                            color = if (category == null) Color(0xFFB3ACD6) else Color.White
                        )
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "سقف بودجه ماهانه (تومان)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2A2150)
                        ) {
                            Text(
                                "کیپد فارسی",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB3ACD6),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newAmount ->
                            amount = newAmount.filter { char ->
                                char.isDigit() || char in "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩"
                            }
                        },
                        suffix = { Text("تومان", color = Color(0xFFA855F7)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (validation.isNotEmpty()) {
                    item {
                        Text(validation, color = MaterialTheme.colorScheme.error)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) { Text("انصراف") }
                        Button(
                            onClick = {
                                val value = normalizeDigits(amount).toLongOrNull()
                                when {
                                    category == null -> validation = "دسته را انتخاب کنید."
                                    value == null || value <= 0L -> validation = "مبلغ معتبر وارد کنید."
                                    else -> onSave(category!!, value)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA855F7),
                                contentColor = Color.White
                            )
                        ) { Text("ذخیره") }
                    }
                }
            }
        }
    }
}
