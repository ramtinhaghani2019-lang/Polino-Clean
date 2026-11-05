package com.polino.app.dashboard

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.polino.app.voice.parseSpokenAmount
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
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        val parsed = spoken?.let { parseSpokenAmount(it) }
        if (parsed != null && parsed > 0L) {
            amountText = parsed.toString()
            error = ""
        } else {
            error = "مبلغ از گفتار تشخیص داده نشد."
        }
    }
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

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Spacer(Modifier.width(8.dp))
                Text("تراکنش جدید", style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            // مرحله ۱۰۸: قرص‌های باریک‌تر مثل پرگار؛ متن ساده بدون پیکان.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isExpense) {
                    Button(
                        onClick = { isExpense = true },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EntryRed,
                            contentColor = Color.White
                        )
                    ) { Text("هزینه") }
                } else {
                    OutlinedButton(
                        onClick = { isExpense = true },
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) { Text("هزینه") }
                }
                if (!isExpense) {
                    Button(
                        onClick = { isExpense = false },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EntryGreen,
                            contentColor = Color.White
                        )
                    ) { Text("درآمد") }
                } else {
                    OutlinedButton(
                        onClick = { isExpense = false },
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) { Text("درآمد") }
                }
            }
        }
        item {
            val amountContainer = EntryDark
            val amountLabel = Color(0xFF9AA3B8)
            // مرحله ۱۰۸: کارت شناور بدون بوردر مثل پرگار؛ برچسب یک خط،
            // چیپ‌ها و ورودی صوتی در ردیف دوم، مبلغ بزرگ راست‌چین.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = amountContainer)
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EntryGold
                        ) {
                            Text(
                                "کیپد فعال",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            val intent =
                                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                                    putExtra(
                                        RecognizerIntent.EXTRA_PROMPT,
                                        "مبلغ را بگویید؛ مثلاً پانصد هزار تومان"
                                    )
                                }
                            runCatching { voiceLauncher.launch(intent) }
                                .onFailure {
                                    error = "تشخیص گفتار روی این گوشی در دسترس نیست."
                                }
                        }) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "ورودی صوتی",
                                tint = EntryGold
                            )
                        }
                        TextButton(onClick = { amountText = "" }) {
                            Text("پاک کردن", color = EntryGold)
                        }
                    }
                    Text(
                        money(amount),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isExpense) EntryRed else EntryGreen,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            // مرحله ۱۰۸: کیپد همیشه راست‌به‌چپ مثل پرگار — ۱ بالا-راست،
            // ⌫ پایین-چپ؛ ترتیب فهرست دست نخورده چون با چیدمان راست‌به‌چپ
            // سراسری برنامه همان‌طور رندر می‌شود (تأیید با ویدیوی پولینو).
            // کلیدها باریک‌تر، با فاصله بیشتر و رقم‌های کوچک‌تر و سبک‌تر.
            val keys = listOf(
                listOf("۱" to "1", "۲" to "2", "۳" to "3"),
                listOf("۴" to "4", "۵" to "5", "۶" to "6"),
                listOf("۷" to "7", "۸" to "8", "۹" to "9"),
                listOf("۰۰۰" to "000", "۰" to "0", null)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
        item {
            Text("انتخاب حساب بانکی", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { selectedAccountId = account.id },
                        label = { Text(account.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
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
                    style = MaterialTheme.typography.titleMedium
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
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        categoryIconForName(categories, suggestion.category),
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Column {
                                        Text(suggestion.category)
                                        Text(
                                            money(suggestion.amount),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
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
            Text("اطلاعات تکمیلی", style = MaterialTheme.typography.titleMedium)
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
                    Text("تاریخ تراکنش: ${JalaliDate.formatJalali(dateMillis)}")
                    TextButton(onClick = { showDatePicker = true }) { Text("تغییر تاریخ") }
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
