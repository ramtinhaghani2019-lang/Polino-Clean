package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.polino.app.data.PlanEntity
import com.polino.app.data.TransactionRepository
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * صفحه «برنامه‌ها»: فهرست برنامه‌های مالی با موعد اختیاری و نشان
 * انجام‌شدن، به‌همراه ورق تیره ساخت و ویرایش. از همان زبان ورق‌های
 * تیره مرحله‌های ۸۶ و ۸۷ استفاده می‌کند: عنوان + نشان اجباری، دکمه
 * سراسری ذخیره، بستن با X.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlansScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val scope = rememberCoroutineScope()
    var plans by remember { mutableStateOf<List<PlanEntity>>(emptyList()) }
    var showSheet by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<PlanEntity?>(null) }
    var planToDelete by remember { mutableStateOf<PlanEntity?>(null) }

    suspend fun reload() {
        runCatching { plans = repository.getPlans() }
    }

    LaunchedEffect(Unit) { reload() }

    val openCount = plans.count { !it.done }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) { Text("بازگشت") }
            Text("برنامه‌ها", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "${plans.size} برنامه · $openCount باز",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (plans.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
                        border = BorderStroke(1.dp, Color(0xFF3D3368))
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EventNote,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                "هنوز برنامه‌ای ثبت نکرده‌اید",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                "برنامه‌های مالی‌تان را با موعد مشخص بنویسید تا قدم‌به‌قدم دنبالشان کنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB3ACD6)
                            )
                            Button(onClick = { showSheet = true }) {
                                Text("ساخت اولین برنامه")
                            }
                        }
                    }
                }
            }
            plans.forEach { plan ->
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
                                    plan.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { planToEdit = plan }) {
                                    Icon(Icons.Default.Edit, contentDescription = "ویرایش برنامه")
                                }
                                IconButton(onClick = { planToDelete = plan }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف برنامه")
                                }
                            }
                            if (plan.note.isNotBlank()) {
                                Text(plan.note, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    when {
                                        plan.dueMillis == null -> "بدون موعد"
                                        isPlanOverdue(
                                            plan.dueMillis,
                                            System.currentTimeMillis(),
                                            plan.done
                                        ) -> "سررسید گذشته"
                                        else -> "موعد: ${JalaliDate.formatJalali(plan.dueMillis)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (
                                        isPlanOverdue(
                                            plan.dueMillis,
                                            System.currentTimeMillis(),
                                            plan.done
                                        )
                                    ) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(Modifier.weight(1f))
                                FilterChip(
                                    selected = plan.done,
                                    onClick = {
                                        scope.launch {
                                            repository.togglePlanDone(plan)
                                            reload()
                                        }
                                    },
                                    label = { Text(if (plan.done) "انجام شد" else "انجام نشده") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF16A34A),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "برنامه جدید")
        }
    }

    if (showSheet || planToEdit != null) {
        PlanSheet(
            initial = planToEdit,
            onDismiss = { showSheet = false; planToEdit = null },
            onSave = { title, note, dueMillis ->
                scope.launch {
                    val item = planToEdit?.copy(
                        title = title,
                        note = note,
                        dueMillis = dueMillis
                    ) ?: PlanEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        note = note,
                        dueMillis = dueMillis
                    )
                    repository.addPlan(item)
                    reload()
                    showSheet = false
                    planToEdit = null
                }
            }
        )
    }

    if (planToDelete != null) {
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("حذف برنامه") },
            text = { Text("برنامه «${planToDelete?.title}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    val id = planToDelete?.id
                    planToDelete = null
                    if (id != null) {
                        scope.launch {
                            repository.deletePlan(id)
                            reload()
                        }
                    }
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanSheet(
    initial: PlanEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, dueMillis: Long?) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var dueMillis by remember { mutableStateOf(initial?.dueMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueMillis ?: System.currentTimeMillis()
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1438)
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
                        if (initial == null) "برنامه جدید" else "ویرایش برنامه",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
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
                        "عنوان",
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
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان برنامه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    "یادداشت (اختیاری)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "موعد (اختیاری)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (dueMillis != null) {
                            TextButton(onClick = { dueMillis = null }) {
                                Text("بدون موعد")
                            }
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "انتخاب موعد",
                                tint = Color.White
                            )
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(
                                if (dueMillis == null) {
                                    "انتخاب موعد"
                                } else {
                                    JalaliDate.formatJalali(dueMillis!!)
                                },
                                color = Color.White
                            )
                        }
                    }
                }
            }
            if (validation.isNotEmpty()) {
                item {
                    Text(validation, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Button(
                    onClick = {
                        when {
                            title.isBlank() -> validation = "عنوان را وارد کنید."
                            else -> onSave(title.trim(), note.trim(), dueMillis)
                        }
                    },
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
                        "ذخیره برنامه",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueMillis = it }
                    showDatePicker = false
                }) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("انصراف") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
