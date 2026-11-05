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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.polino.app.data.ChequeReminderEntity
import com.polino.app.data.GoalEntity
import com.polino.app.data.InstallmentEntity
import com.polino.app.data.NoteEntity
import com.polino.app.data.PlanEntity
import com.polino.app.data.TaskEntity
import com.polino.app.data.TransactionRepository
import com.polino.app.data.goalProgressFraction
import com.polino.app.data.goalsOverallFraction
import com.polino.app.data.isGoalCompleted
import java.util.UUID
import kotlinx.coroutines.launch

/** رنگ‌های ورق تیره هدف، به زبان ورق‌های تیره مرحله‌های ۶۹ تا ۷۱. */
private val GoalSheetDark = Color(0xFF1E1438)
private val GoalSheetCard = Color(0xFF2A2150)
private val GoalSheetBorder = Color(0xFF3D3368)
private val GoalSheetMuted = Color(0xFFB3ACD6)
private val GoalPurple = Color(0xFFA855F7)
private val GoalGreen = Color(0xFF34C89E)
private val GoalPriorityOrange = Color(0xFFF59E0B)

/** پیشنهادهای سریع فرم هدف، عین عکس‌های پرگار. */
private val goalSuggestions = listOf(
    "پس‌انداز اضطراری", "خرید لپ‌تاپ", "سفر تفریحی", "خرید خودرو",
    "مسکن", "خرید کالا", "سرمایه‌گذاری", "سفر", "خودرو", "آموزش", "سایر"
)

/** دسته‌بندی‌های هدف، عین فرم پرگار. */
private val goalCategories = listOf(
    "پس‌انداز", "سرمایه‌گذاری", "خرید کالا", "مسکن", "سفر", "خودرو", "آموزش", "سایر"
)

/** اولویت‌ها به ترتیبی که در فرم پرگار دیده می‌شوند: زیاد، متوسط، کم. */
private val goalPriorities = listOf("زیاد", "متوسط", "کم")

/**
 * صفحه «اهداف و برنامه‌ریزی»، عین هاب پرگار: کارت وضعیت کل اهداف،
 * کاشی‌های اهداف و کارها، کاشی‌های برنامه‌ها و یادداشت‌ها، ردیف
 * سراسری یادآورها، صافی‌های وضعیت و کارت‌های هدف با فرم ساخت و
 * ویرایش. کاشی‌های برنامه‌ها و یادداشت‌ها به صفحه‌های خودشان می‌روند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToPlans: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val scope = rememberCoroutineScope()
    var goals by remember { mutableStateOf<List<GoalEntity>>(emptyList()) }
    var tasks by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }
    var cheques by remember { mutableStateOf<List<ChequeReminderEntity>>(emptyList()) }
    var installments by remember { mutableStateOf<List<InstallmentEntity>>(emptyList()) }
    var plans by remember { mutableStateOf<List<PlanEntity>>(emptyList()) }
    var notes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
    var filter by remember { mutableStateOf("همه") }
    var showSheet by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }
    val listState = rememberLazyListState()

    suspend fun reloadData() {
        runCatching {
            goals = repository.getGoals()
            tasks = repository.getTasks()
            cheques = repository.getCheques()
            installments = repository.getInstallments()
            plans = repository.getPlans()
            notes = repository.getNotes()
        }
    }

    LaunchedEffect(Unit) { reloadData() }

    val filteredGoals = when (filter) {
        "در جریان" -> goals.filter { !isGoalCompleted(it) }
        "تکمیل شده" -> goals.filter { isGoalCompleted(it) }
        else -> goals
    }
    val inProgressCount = goals.count { !isGoalCompleted(it) }
    val completedCount = goals.size - inProgressCount
    val totalSaved = goals.sumOf { it.savedAmount.coerceAtLeast(0) }
    val totalTarget = goals.sumOf { it.target }
    val overallFraction = goalsOverallFraction(goals)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onBack) { Text("بازگشت") }
                Text("اهداف و برنامه‌ریزی", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                "وضعیت کل اهداف و برنامه‌های مالی",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "وضعیت کل اهداف",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    "$inProgressCount هدف در جریان",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text("پیشرفت کل: ${(overallFraction * 100).toInt()}٪")
                            LinearProgressIndicator(
                                progress = { overallFraction },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("کل پس‌انداز شده", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        money(totalSaved),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GoalGreen
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("مجموع هدف‌ها", style = MaterialTheme.typography.bodySmall)
                                    Text(money(totalTarget), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GoalHubTile(
                            modifier = Modifier.weight(1f),
                            label = "اهداف",
                            subtitle = goalsHubSubtitle(goals),
                            icon = Icons.Default.Flag,
                            active = true,
                            onClick = {
                                scope.launch { listState.animateScrollToItem(4) }
                            }
                        )
                        GoalHubTile(
                            modifier = Modifier.weight(1f),
                            label = "کارها",
                            subtitle = tasksHubSubtitle(tasks),
                            icon = Icons.Default.List,
                            active = false,
                            onClick = onNavigateToTasks
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GoalHubTile(
                            modifier = Modifier.weight(1f),
                            label = "برنامه‌ها",
                            subtitle = plansHubSubtitle(plans),
                            icon = Icons.Default.EventNote,
                            active = false,
                            onClick = onNavigateToPlans
                        )
                        GoalHubTile(
                            modifier = Modifier.weight(1f),
                            label = "یادداشت‌ها",
                            subtitle = notesHubSubtitle(notes),
                            icon = Icons.Default.Note,
                            active = false,
                            onClick = onNavigateToNotes
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
                item {
                    GoalRemindersRow(
                        countText = remindersHubSubtitle(
                            cheques,
                            installments,
                            System.currentTimeMillis()
                        ),
                        onClick = onNavigateToReminders
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalFilterChip(
                            label = "همه (${goals.size})",
                            selected = filter == "همه",
                            onClick = { filter = "همه" }
                        )
                        GoalFilterChip(
                            label = "در جریان ($inProgressCount)",
                            selected = filter == "در جریان",
                            onClick = { filter = "در جریان" }
                        )
                        GoalFilterChip(
                            label = "تکمیل شده ($completedCount)",
                            selected = filter == "تکمیل شده",
                            onClick = { filter = "تکمیل شده" }
                        )
                    }
                }
                item {
                    Text(
                        "هدف‌ها",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (goals.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = GoalPurple,
                                    modifier = Modifier.padding(8.dp)
                                )
                                Text(
                                    "هنوز هدف مالی ثبت نکرده‌اید",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    "برای پس‌انداز، خرید کالا، سفر یا سرمایه‌گذاری یک هدف تعریف کنید تا مسیر دستیابی آن برنامه‌ریزی شود.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(onClick = { showSheet = true }) {
                                    Text("ساخت اولین هدف مالی")
                                }
                            }
                        }
                    }
                } else if (filteredGoals.isEmpty()) {
                    item { Text("در این وضعیت هدفی نیست.") }
                } else {
                    filteredGoals.forEach { goal ->
                        item {
                            GoalCard(
                                goal = goal,
                                onEdit = { goalToEdit = goal },
                                onDelete = { goalToDelete = goal }
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "هدف مالی جدید")
        }
    }

    if (showSheet || goalToEdit != null) {
        GoalSheet(
            initial = goalToEdit,
            onDismiss = { showSheet = false; goalToEdit = null },
            onSave = { goal ->
                scope.launch {
                    runCatching { repository.addGoal(goal) }
                    reloadData()
                }
                showSheet = false
                goalToEdit = null
            }
        )
    }
    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("حذف هدف", fontWeight = FontWeight.Bold) },
            text = { Text("هدف «${goalToDelete!!.name}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    val target = goalToDelete
                    goalToDelete = null
                    if (target != null) {
                        scope.launch {
                            repository.deleteGoal(target.id)
                            reloadData()
                        }
                    }
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun GoalHubTile(
    modifier: Modifier = Modifier,
    label: String,
    subtitle: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            if (active) 2.dp else 1.dp,
            if (active) GoalGreen else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GoalRemindersRow(
    countText: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "یادآورها",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.weight(1f))
            Text(countText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GoalFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GoalPurple,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
private fun GoalMiniChip(text: String, borderColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val fraction = goalProgressFraction(goal.savedAmount, goal.target)
    val completed = fraction >= 1f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش هدف")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف هدف")
                    }
                }
            }
            Text("${money(goal.savedAmount)} از ${money(goal.target)}")
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goal.category.isNotBlank()) {
                    GoalMiniChip(
                        goal.category,
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GoalMiniChip(
                    "اولویت: ${goal.priority}",
                    GoalPriorityOrange,
                    GoalPriorityOrange
                )
                if (goal.dueMillis != null) {
                    Text(
                        "موعد: ${JalaliDate.formatJalali(goal.dueMillis)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (completed) {
                    Text("تکمیل شد", color = MaterialTheme.colorScheme.primary)
                }
            }
            if (goal.note.isNotBlank()) {
                Text(goal.note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * ورق ساخت و ویرایش هدف، عین فرم پرگار: پیشنهادهای سریع، عنوان و مبلغ
 * کل اجباری، مبلغ پس‌اندازشده، موعد اختیاری، دسته‌بندی، اولویت و یادداشت.
 * نشان «کیپد فارسی» فقط برچسب است؛ فیلد مبلغ از پیش ارقام فارسی و عربی
 * را هم می‌پذیرد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalSheet(
    initial: GoalEntity?,
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.name ?: "") }
    var targetText by remember { mutableStateOf(initial?.target?.toString() ?: "") }
    var savedText by remember {
        mutableStateOf(if (initial == null) "" else initial.savedAmount.toString())
    }
    var dueMillis by remember { mutableStateOf(initial?.dueMillis) }
    var category by remember {
        mutableStateOf(initial?.category?.ifBlank { "پس‌انداز" } ?: "پس‌انداز")
    }
    var priority by remember {
        mutableStateOf(initial?.priority?.ifBlank { "متوسط" } ?: "متوسط")
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var validation by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueMillis ?: System.currentTimeMillis()
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GoalSheetDark
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
                        if (initial == null) "تعریف هدف مالی جدید" else "ویرایش هدف مالی",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                }
            }
            item {
                Text(
                    "پیشنهادهای سریع:",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goalSuggestions.forEach { suggestion ->
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { title = suggestion },
                                label = { Text(suggestion) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = GoalSheetCard,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            item {
                GoalSheetAmountField(
                    label = "عنوان هدف مالی",
                    required = true,
                    value = title,
                    onValueChange = { title = it },
                    numeric = false
                )
            }
            item {
                GoalSheetAmountField(
                    label = "مبلغ کل هدف (تومان)",
                    required = true,
                    value = targetText,
                    onValueChange = { targetText = it.filterDigits() },
                    numeric = true
                )
            }
            item {
                GoalSheetAmountField(
                    label = "مبلغ پس‌انداز شده تا کنون (تومان)",
                    required = false,
                    value = savedText,
                    onValueChange = { savedText = it.filterDigits() },
                    numeric = true
                )
            }
            item {
                Text(
                    "تاریخ موعد دستیابی به هدف (اختیاری)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showDatePicker = true }) {
                        val selectedDue = dueMillis
                        Text(
                            if (selectedDue == null) "انتخاب تاریخ موعد"
                            else JalaliDate.formatJalali(selectedDue)
                        )
                    }
                    if (dueMillis != null) {
                        TextButton(onClick = { dueMillis = null }) {
                            Text("حذف موعد", color = Color.White)
                        }
                    }
                }
            }
            item {
                Text("دسته‌بندی هدف:", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goalCategories.forEach { item ->
                        item {
                            FilterChip(
                                selected = category == item,
                                onClick = { category = item },
                                label = { Text(item) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = GoalSheetCard,
                                    labelColor = Color.White,
                                    selectedContainerColor = GoalPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            item {
                Text("اولویت:", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goalPriorities.forEach { item ->
                        val selected = priority == item
                        OutlinedButton(
                            onClick = { priority = item },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) GoalPriorityOrange else GoalSheetBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) GoalPriorityOrange else GoalSheetMuted
                            )
                        ) { Text(item) }
                    }
                }
            }
            item {
                Text(
                    "توضیحات و یادداشت تکمیلی",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = goalSheetFieldColors()
                )
            }
            if (validation.isNotEmpty()) {
                item {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Button(
                    onClick = {
                        val parsedTarget = normalizeDigits(targetText).toLongOrNull()
                        val parsedSaved = if (savedText.isBlank()) {
                            0L
                        } else {
                            normalizeDigits(savedText).toLongOrNull()
                        }
                        val error = when {
                            title.isBlank() -> "عنوان هدف را وارد کنید."
                            parsedTarget == null || parsedTarget <= 0 -> "مبلغ معتبر وارد کنید."
                            parsedSaved == null || parsedSaved < 0 -> "مبلغ پس‌اندازشده معتبر نیست."
                            else -> ""
                        }
                        validation = error
                        if (error.isEmpty() && parsedTarget != null && parsedSaved != null) {
                            onSave(
                                initial?.copy(
                                    name = title.trim(),
                                    target = parsedTarget,
                                    savedAmount = parsedSaved,
                                    dueMillis = dueMillis,
                                    priority = priority,
                                    category = category,
                                    note = note.trim()
                                ) ?: GoalEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = title.trim(),
                                    target = parsedTarget,
                                    accountId = "main",
                                    savedAmount = parsedSaved,
                                    dueMillis = dueMillis,
                                    priority = priority,
                                    category = category,
                                    note = note.trim()
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (initial == null) "ساخت هدف مالی" else "ذخیره تغییرات")
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
                ) {
                    Text("تأیید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("لغو")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun GoalSheetAmountField(
    label: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    numeric: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
            if (required) {
                GoalMiniChip("اجباری", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
            }
            if (numeric) {
                GoalMiniChip("کیپد فارسی", GoalSheetBorder, GoalSheetMuted)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = if (numeric) {
                { Text("تومان", color = GoalPurple) }
            } else {
                null
            },
            colors = goalSheetFieldColors()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun goalSheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = GoalSheetCard,
    unfocusedContainerColor = GoalSheetCard,
    focusedBorderColor = GoalSheetBorder,
    unfocusedBorderColor = GoalSheetBorder,
    cursorColor = Color.White
)

/** فقط ارقام لاتین، فارسی و عربی می‌مانند؛ مثل فیلد مبلغ دیالوگ قدیمی هدف. */
private fun String.filterDigits(): String = filter { char ->
    char.isDigit() || char in "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩"
}
