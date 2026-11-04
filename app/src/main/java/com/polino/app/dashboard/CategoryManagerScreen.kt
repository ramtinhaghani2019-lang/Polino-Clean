package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.polino.app.data.CategoryEntity
import com.polino.app.data.CategoryType
import com.polino.app.data.FALLBACK_CATEGORY
import com.polino.app.data.TransactionRepository
import com.polino.app.data.categoryType
import com.polino.app.data.categoryTypeLabel
import com.polino.app.data.mainCategories
import com.polino.app.data.subcategoriesOf
import kotlinx.coroutines.launch

private val CategoryManagerPurple = Color(0xFFA855F7)
private val CategoryManagerMuted = Color(0xFFB3ACD6)
private val CategoryManagerBorder = Color(0xFF3D3368)
private val CategoryExpense = Color(0xFFF87171)
private val CategoryIncome = Color(0xFF34C89E)
private val CategoryInvestment = Color(0xFF2DD4BF)

private fun categoryTypeColor(type: CategoryType): Color = when (type) {
    CategoryType.EXPENSE -> CategoryExpense
    CategoryType.INCOME -> CategoryIncome
    CategoryType.INVESTMENT -> CategoryInvestment
}

/**
 * صفحه اختصاصی مدیریت دسته‌ها: سرخط با شمار کل، زبانه‌های نوع با شمار
 * دسته‌های اصلی، کارت‌های بزرگ با کاشی آیکن رنگی، زیردسته‌های درون‌کارتی،
 * دکمه «زیردسته جدید» به شکل قرص، و دکمه‌های نمایشی/ویرایش/حذف. «سایر»
 * محافظت‌شده است (نه تغییرنام، نه حذف) و دسته اصلی دارای زیردسته حذف
 * نمی‌شود. افزودن/ویرایش/حذف از همان متدهای موجود مخزن استفاده می‌کند.
 */
@Composable
internal fun CategoryManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(CategoryType.EXPENSE) }
    var collapsed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var message by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var subcategoryParent by remember { mutableStateOf<CategoryEntity?>(null) }
    var renameTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { categories = repository.getCategories() }

    val mains = mainCategories(categories, selectedTab)
    val tabColor = categoryTypeColor(selectedTab)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("مدیریت دسته‌ها", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("${categories.size} دسته", style = MaterialTheme.typography.bodySmall, color = CategoryManagerMuted)
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1438), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CategoryType.entries.forEach { type ->
                    val selected = selectedTab == type
                    val color = categoryTypeColor(type)
                    Button(
                        onClick = { selectedTab = type },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) color else Color.Transparent,
                            contentColor = if (selected) Color.White else CategoryManagerMuted
                        )
                    ) {
                        Text("${categoryTypeLabel(type)} (${mainCategories(categories, type).size})")
                    }
                }
            }
        }
        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CategoryManagerPurple,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("دسته جدید", style = MaterialTheme.typography.titleMedium)
            }
        }
        if (message.isNotEmpty()) {
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        if (mains.isEmpty()) {
            item {
                Text(
                    "در این بخش دسته‌ای ثبت نشده است. با «دسته جدید» یکی بسازید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CategoryManagerMuted
                )
            }
        }
        mains.forEach { category ->
            item {
                val subs = subcategoriesOf(categories, category.name)
                val isCollapsed = category.name in collapsed
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
                    border = BorderStroke(1.dp, CategoryManagerBorder)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val tileColor = categoryColorFor(category, tabColor)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(tileColor.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    categoryIconFor(category),
                                    contentDescription = category.name,
                                    tint = tileColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(category.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    "دسته اصلی · ${categoryTypeLabel(categoryType(category))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CategoryManagerMuted
                                )
                            }
                            IconButton(onClick = {
                                collapsed = if (isCollapsed) collapsed - category.name else collapsed + category.name
                            }) {
                                Icon(
                                    if (isCollapsed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isCollapsed) "نمایش زیردسته‌ها" else "پنهان کردن زیردسته‌ها"
                                )
                            }
                            if (category.name != FALLBACK_CATEGORY) {
                                IconButton(onClick = { renameTarget = category }) {
                                    Icon(Icons.Default.Edit, contentDescription = "ویرایش")
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        if (subs.isNotEmpty()) {
                                            message = "اول زیردسته‌های این دسته را حذف کنید."
                                        } else if (repository.deleteCategory(category.name)) {
                                            message = ""
                                            categories = repository.getCategories()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                                }
                            }
                        }
                        if (!isCollapsed) {
                            subs.forEach { sub ->
                                Row(
                                    modifier = Modifier.padding(start = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(sub.name, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { renameTarget = sub }) {
                                        Icon(Icons.Default.Edit, contentDescription = "ویرایش")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            repository.deleteCategory(sub.name)
                                            categories = repository.getCategories()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف")
                                    }
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { subcategoryParent = category },
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("زیردسته جدید")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showAddDialog) {
        CategoryNameDialog(
            title = "دسته جدید",
            initialName = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                scope.launch {
                    val ok = repository.addCategory(
                        name,
                        isIncome = selectedTab == CategoryType.INCOME,
                        isInvestment = selectedTab == CategoryType.INVESTMENT
                    )
                    if (ok) {
                        message = ""
                        showAddDialog = false
                        categories = repository.getCategories()
                    } else {
                        message = "نام تکراری است."
                    }
                }
            }
        )
    }
    val parent = subcategoryParent
    if (parent != null) {
        CategoryNameDialog(
            title = "زیردسته جدید",
            initialName = "",
            onDismiss = { subcategoryParent = null },
            onConfirm = { name ->
                scope.launch {
                    val ok = repository.addCategory(
                        name,
                        parent.isIncome,
                        parent.isInvestment,
                        parent.name
                    )
                    if (ok) {
                        message = ""
                        subcategoryParent = null
                        categories = repository.getCategories()
                    } else {
                        message = "نام تکراری است."
                    }
                }
            }
        )
    }
    val target = renameTarget
    if (target != null) {
        CategoryEditSheet(
            target = target,
            onDismiss = { renameTarget = null },
            onConfirm = { name, iconKey, colorArgb ->
                scope.launch {
                    var ok = true
                    if (name != target.name) ok = repository.renameCategory(target.name, name)
                    if (ok) ok = repository.updateCategoryStyle(name, iconKey, colorArgb)
                    if (ok) {
                        message = ""
                        renameTarget = null
                        categories = repository.getCategories()
                    } else {
                        message = "نام تکراری یا نامعتبر است."
                    }
                }
            }
        )
    }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام دسته") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

/**
 * برگه ویرایش دسته به سبک پرگار (فریم‌های g_26 و g_27): پیش‌نمایش
 * آیکون و نام، فیلد نام با نشان «اجباری»، شبکه ۵ ستونی آیکون‌ها،
 * پالت رنگ شاخص در دو ردیف، و دکمه‌های انصراف و ذخیره. نوع و والد
 * دسته در این برگه تغییر نمی‌کنند؛ فقط نام و سبک (آیکون و رنگ).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditSheet(
    target: CategoryEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(target.name) }
    var iconKey by remember { mutableStateOf(target.iconKey) }
    var colorArgb by remember { mutableStateOf(target.colorArgb) }
    val typeColor = categoryTypeColor(categoryType(target))
    val selectedColor = if (colorArgb != 0L) Color(colorArgb) else typeColor
    val previewIcon = categoryIconByKey(iconKey) ?: categoryIcon(target.name)
    val scopeText = if (target.parentId == null) "دسته اصلی" else "زیردسته"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFF1E1438)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "ویرایش دسته‌بندی",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(selectedColor.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        previewIcon,
                        contentDescription = null,
                        tint = selectedColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        name.ifBlank { target.name },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "$scopeText · ${categoryTypeLabel(categoryType(target))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CategoryManagerMuted
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "نام دسته‌بندی",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Box(
                        modifier = Modifier
                            .background(CategoryExpense.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("اجباری", style = MaterialTheme.typography.labelSmall, color = CategoryExpense)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دسته") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "انتخاب آیکون دسته‌بندی:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CATEGORY_ICON_OPTIONS) { option ->
                        val selected = option.key == iconKey
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (selected) CategoryManagerPurple else Color(0xFF2A2150),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { iconKey = option.key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                option.icon,
                                contentDescription = option.key,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                TextButton(onClick = { iconKey = "" }) { Text("آیکون پیش‌فرض") }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "رنگ شاخص:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CATEGORY_COLOR_PALETTE) { argb ->
                        val selected = argb == colorArgb
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(argb), CircleShape)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color.White else CategoryManagerBorder,
                                    shape = CircleShape
                                )
                                .clickable { colorArgb = argb }
                        )
                    }
                }
                TextButton(onClick = { colorArgb = 0L }) { Text("بدون رنگ شخصی") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("انصراف") }
                Button(
                    onClick = { onConfirm(name.trim(), iconKey, colorArgb) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CategoryManagerPurple,
                        contentColor = Color.White
                    )
                ) { Text("ذخیره") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
