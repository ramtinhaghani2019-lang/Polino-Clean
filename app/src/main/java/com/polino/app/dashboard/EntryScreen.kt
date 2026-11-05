package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polino.app.data.AccountEntity
import com.polino.app.data.CategoryEntity
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import com.polino.app.data.CategoryType
import com.polino.app.data.mainCategories
import com.polino.app.data.subcategoriesOf
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

private val EntryGold = Color(0xFFA855F7)
private val EntryGreen = Color(0xFF34C89E)
private val EntryRed = Color(0xFFFF6B6B)
private val EntryDark = Color(0xFF1E1438)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    initialAmount: String = "",
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var isExpense by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var selectedAccountId by remember { mutableStateOf("main") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var recentTransactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var showAllCategories by remember { mutableStateOf(false) }
    var extraInfoExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        accounts = runCatching { repository.getAccounts() }.getOrDefault(emptyList())
        categories = runCatching { repository.getCategories() }.getOrDefault(emptyList())
        recentTransactions = runCatching { repository.getAll() }.getOrDefault(emptyList())
    }
    val pickedType = if (isExpense) CategoryType.EXPENSE else CategoryType.INCOME
    val groupedCategories: List<Pair<CategoryEntity, List<CategoryEntity>>> =
        mainCategories(categories, pickedType).map { main ->
            main to subcategoriesOf(categories, main.name)
        }
    val categoryNames = groupedCategories.flatMap { (main, subs) ->
        listOf(main.name) + subs.map { it.name }
    }
    LaunchedEffect(isExpense, categories) {
        if (selectedCategory == null || selectedCategory !in categoryNames) {
            selectedCategory = categoryNames.firstOrNull()
        }
    }
    val suggestions = remember(recentTransactions, isExpense) {
        deriveEntrySuggestions(recentTransactions, income = !isExpense)
    }

    val amount = amountText.toLongOrNull() ?: 0L
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
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

    fun appendDigits(digits: String) {
        if (amountText.length + digits.length <= 12) amountText += digits
    }

    // مرحله ۱۱۲: ثبت تراکنش شیت پایینی است مثل پرگار («ثبت سریع تراکنش»)؛
    // شیت روی صفحه خانه باز می‌شود و محتوای کامل ورود را در خود دارد.
    // دسته کشویی را خود شیت می‌کشد؛ رنگ شیت همان قرارداد CalculatorSheet است.
    val isDarkSheet = MaterialTheme.colorScheme.background.luminance() < 0.5f
    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDarkSheet) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface
    ) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // مرحله ۱۱۲: سربرگ شیت مثل پرگار — عنوان «ثبت سریع تراکنش» و ✕؛
            // دسته کشویی را خود شیت می‌کشد؛ دکمه بازگشت حذف شد.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ثبت سریع تراکنش",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item {
            // مرحله ۱۱۲: دکمه‌ها با فلش مثل پرگار — «درآمد ↑» / «هزینه ↓».
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isExpense) {
                    Button(
                        onClick = { isExpense = true },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EntryRed,
                            contentColor = Color.White
                        )
                    ) { Text("هزینه ↓") }
                } else {
                    OutlinedButton(
                        onClick = { isExpense = true },
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) { Text("هزینه ↓") }
                }
                if (!isExpense) {
                    Button(
                        onClick = { isExpense = false },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EntryGreen,
                            contentColor = Color.White
                        )
                    ) { Text("درآمد ↑") }
                } else {
                    OutlinedButton(
                        onClick = { isExpense = false },
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) { Text("درآمد ↑") }
                }
            }
        }
        item {
            val amountContainer = EntryDark
            val amountLabel = Color(0xFF9AA3B8)
            // مرحله ۱۱۲: کارت مبلغ با حاشیه بنفش مثل پرگار؛ مبلغ کوچک
            // (رقم سفید + واحد بنفش)؛ بدون میکروفون و «پاک کردن» در کارت.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = amountContainer),
                border = BorderStroke(1.dp, Color(0xFF3D3368))
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "مبلغ تراکنش (تومان)",
                        style = MaterialTheme.typography.labelLarge,
                        color = amountLabel,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            color = EntryGold
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "کیپد فعال",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(amount),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "تومان",
                            style = MaterialTheme.typography.titleLarge,
                            color = EntryGold
                        )
                    }
                }
            }
        }
        item {
            // مرحله ۱۱۲: کیپد چپ‌به‌راست مثل پرگار — ۱ بالا-چپ، ⌫ پایین-راست؛
            // فقط کیپد به چپ‌به‌راست محدود شده، بقیه برنامه راست‌به‌چپ می‌ماند.
            // کلیدها داخل یک قاب تیره با حاشیه‌اند؛ باریکی و فاصله از ۱۰۸ می‌ماند.
            val keys = listOf(
                listOf("۱" to "1", "۲" to "2", "۳" to "3"),
                listOf("۴" to "4", "۵" to "5", "۶" to "6"),
                listOf("۷" to "7", "۸" to "8", "۹" to "9"),
                listOf("۰۰۰" to "000", "۰" to "0", null)
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF3D3368)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171233))
            ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    keys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { key ->
                                if (key == null) {
                                    Button(
                                        onClick = { amountText = amountText.dropLast(1) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(64.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4A2020),
                                            contentColor = EntryRed
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Backspace,
                                            contentDescription = "حذف رقم"
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { appendDigits(key.second) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(64.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2A2150),
                                            contentColor = Color(0xFFD8D0F5)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF3D3368))
                                    ) {
                                        Text(
                                            key.first,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Normal
                                            )
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
        item {
            Text("انتخاب حساب بانکی", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    // مرحله ۱۱۲: قرص پرشده با آیکون بانک و برچسب دوخطی مثل پرگار؛
                    // انتخاب‌شده بنفش پررنگ، بقیه تیره با حاشیه.
                    val selectedAccount = selectedAccountId == account.id
                    Surface(
                        onClick = { selectedAccountId = account.id },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedAccount) EntryGold else Color(0xFF2A2150),
                        border = if (selectedAccount) null else BorderStroke(1.dp, Color(0xFF3D3368))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (account.bankName != null) Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    account.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    if (account.bankName != null) "بانک" else "نقدی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD8D0F5)
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (showAllCategories) "همه دسته‌ها" else "پیشنهادها",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { showAllCategories = !showAllCategories }) {
                    Text(if (showAllCategories) "پیشنهادها" else "همه دسته‌ها")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!showAllCategories && suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        FilterChip(
                            selected = selectedCategory == suggestion.category,
                            onClick = {
                                selectedCategory = suggestion.category
                                amountText = suggestion.amount.toString()
                                error = ""
                            },
                            // مرحله ۱۱۲: چیپ پیشنهاد فقط آیکون + برچسب است مثل پرگار؛
                            // مبلغ نمایش داده نمی‌شود، ولی با زدن چیپ همان‌طور پر می‌شود.
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        categoryIconForName(categories, suggestion.category),
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(suggestion.category)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupedCategories.forEach { (main, subs) ->
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategory == main.name,
                                onClick = { selectedCategory = main.name },
                                label = { Text(main.name) },
                                leadingIcon = {
                                    Icon(categoryIconFor(main), contentDescription = null)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            subs.forEach { sub ->
                                FilterChip(
                                    selected = selectedCategory == sub.name,
                                    onClick = { selectedCategory = sub.name },
                                    label = { Text(sub.name) },
                                    leadingIcon = {
                                        Icon(categoryIconFor(sub), contentDescription = null)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            // مرحله ۱۱۲: اطلاعات تکمیلی یک ردیف جمع‌شده با فلش بازشونده است مثل پرگار؛
            // فیلدها فقط در حالت باز دیده می‌شوند.
            Surface(
                onClick = { extraInfoExpanded = !extraInfoExpanded },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF3D3368)),
                color = Color(0xFF2A2150)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "اطلاعات تکمیلی (تاریخ، بابت، فروشگاه)",
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (extraInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFFD8D0F5)
                    )
                }
            }
            if (extraInfoExpanded) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان (اختیاری؛ خالی = نام دسته)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات یا بابت (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it },
                    label = { Text("فروشگاه/مبدأ (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تاریخ تراکنش: ${JalaliDate.formatJalali(dateMillis)}", color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { showDatePicker = true }) { Text("تغییر تاریخ") }
                }
            }
            }
        }
        if (error.isNotEmpty()) {
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(
                onClick = {
                    val category = selectedCategory
                    when {
                        amount <= 0L -> error = "مبلغ را وارد کنید."
                        category == null -> error = "دسته را انتخاب کنید."
                        else -> {
                            error = ""
                            scope.launch {
                                val ok = runCatching {
                                    repository.upsert(
                                        MoneyTransaction(
                                            id = UUID.randomUUID().toString(),
                                            title = title.ifBlank { category },
                                            amount = amount,
                                            income = !isExpense,
                                            category = category,
                                            dateMillis = dateMillis,
                                            accountId = selectedAccountId,
                                            note = note,
                                            store = store.trim()
                                        )
                                    )
                                }.isSuccess
                                if (ok) onSaved()
                                else error = "ذخیره اطلاعات ناموفق بود. دوباره تلاش کنید."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EntryGold,
                    contentColor = Color.White
                )
            ) {
                Text("ثبت و ذخیره تراکنش ✓", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
    }
}
