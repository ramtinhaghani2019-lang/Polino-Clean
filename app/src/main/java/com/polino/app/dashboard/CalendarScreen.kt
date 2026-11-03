package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.polino.app.data.EventEntity
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import com.polino.app.data.eventsOnDay
import com.polino.app.data.nextJalaliYearMonth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.launch

private val Weekdays = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

private fun jalaliDayOf(millis: Long): Triple<Int, Int, Int> {
    val local = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return JalaliDate.gregorianToJalali(local.year, local.monthValue, local.dayOfMonth)
}

private fun todayJalaliDay(): Int {
    val today = LocalDate.now()
    return JalaliDate.gregorianToJalali(today.year, today.monthValue, today.dayOfMonth).third
}

private fun weekdayName(jy: Int, jm: Int, jd: Int): String {
    return when (JalaliDate.jalaliToLocalDate(jy, jm, jd).dayOfWeek) {
        java.time.DayOfWeek.SATURDAY -> "شنبه"
        java.time.DayOfWeek.SUNDAY -> "یکشنبه"
        java.time.DayOfWeek.MONDAY -> "دوشنبه"
        java.time.DayOfWeek.TUESDAY -> "سه‌شنبه"
        java.time.DayOfWeek.WEDNESDAY -> "چهارشنبه"
        java.time.DayOfWeek.THURSDAY -> "پنجشنبه"
        java.time.DayOfWeek.FRIDAY -> "جمعه"
        else -> ""
    }
}

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    var transactions by remember { mutableStateOf<List<MoneyTransaction>>(emptyList()) }
    var events by remember { mutableStateOf<List<EventEntity>>(emptyList()) }
    var selectedYearMonth by remember { mutableStateOf(JalaliDate.currentJalaliYearMonth()) }
    var selectedDay by remember { mutableStateOf(todayJalaliDay()) }
    var showEventDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        transactions = runCatching { repository.getAll() }.getOrDefault(emptyList())
        events = runCatching { repository.getEvents() }.getOrDefault(emptyList())
    }

    val currentMonth = JalaliDate.currentJalaliYearMonth()
    val monthParts = selectedYearMonth.split("-")
    val jy = monthParts.getOrNull(0)?.toIntOrNull() ?: 1405
    val jm = monthParts.getOrNull(1)?.toIntOrNull() ?: 1
    val daysInMonth = JalaliDate.daysInJalaliMonth(jy, jm)
    // شنبه آغاز هفته است؛ فاصله روز اول ماه تا شنبه همان هفته.
    val firstOffset = (JalaliDate.jalaliToLocalDate(jy, jm, 1).dayOfWeek.value + 1) % 7

    val monthTransactions = remember(transactions, selectedYearMonth) {
        transactionsInMonth(transactions, selectedYearMonth)
    }
    // برای هر روز ماه: آیا درآمد و/یا هزینه دارد (برای نقطه‌های زیر روز).
    val dayMarks = remember(monthTransactions) {
        val marks = mutableMapOf<Int, Pair<Boolean, Boolean>>()
        monthTransactions.forEach { tx ->
            val day = jalaliDayOf(tx.dateMillis).third
            val current = marks[day] ?: (false to false)
            marks[day] = (current.first || tx.income) to (current.second || !tx.income)
        }
        marks
    }
    val dayTransactions = remember(monthTransactions, selectedDay) {
        monthTransactions.filter { jalaliDayOf(it.dateMillis).third == selectedDay }
    }
    // روزهای دارای رویداد در ماه نمایش‌داده‌شده (برای نقطه بنفش زیر روز).
    val eventDays = remember(events, selectedYearMonth) {
        events.mapNotNull { event ->
            val (eventYear, eventMonth, eventDay) = jalaliDayOf(event.dateMillis)
            if (eventYear == jy && eventMonth == jm) eventDay else null
        }.toSet()
    }
    val dayEvents = remember(events, jy, jm, selectedDay) {
        eventsOnDay(events, JalaliDate.jalaliToMillis(jy, jm, selectedDay))
    }
    val dayIncome = dayTransactions.filter { it.income }.sumOf { it.amount }
    val dayExpense = dayTransactions.filterNot { it.income }.sumOf { it.amount }

    fun goToMonth(month: String) {
        selectedYearMonth = month
        val parts = month.split("-")
        val dim = JalaliDate.daysInJalaliMonth(
            parts.getOrNull(0)?.toIntOrNull() ?: jy,
            parts.getOrNull(1)?.toIntOrNull() ?: jm
        )
        if (selectedDay > dim) selectedDay = dim
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("تقویم مالی", style = MaterialTheme.typography.headlineSmall)
            Text("جریان روزانه و روزهای دارای تراکنش", style = MaterialTheme.typography.labelMedium)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { goToMonth(JalaliDate.previousJalaliYearMonth(selectedYearMonth)) }) {
                    Text("ماه قبل")
                }
                Text(
                    JalaliDate.jalaliMonthDisplay(selectedYearMonth),
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = { goToMonth(nextJalaliYearMonth(selectedYearMonth)) },
                    enabled = selectedYearMonth != currentMonth
                ) {
                    Text("ماه بعد")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Weekdays.forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        item {
            val cells: List<Int?> = List(firstOffset) { null } + (1..daysInMonth).toList()
            val weeks = cells.chunked(7).map { week ->
                if (week.size < 7) week + List(7 - week.size) { null } else week
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day == null) {
                                    Box(Modifier.size(52.dp))
                                } else {
                                    val isSelected = day == selectedDay
                                    val marks = dayMarks[day]
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) Color(0xFFA855F7)
                                                else Color.Transparent
                                            )
                                            .clickable { selectedDay = day }
                                            .padding(vertical = 6.dp)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            day.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) Color.White
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            JalaliDate.jalaliToLocalDate(jy, jm, day).dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) Color.White
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (marks?.first == true) {
                                                Box(
                                                    Modifier
                                                        .size(6.dp)
                                                        .background(Color(0xFF34C89E), CircleShape)
                                                )
                                            }
                                            if (marks?.second == true) {
                                                Box(
                                                    Modifier
                                                        .size(6.dp)
                                                        .background(Color(0xFFFF6B6B), CircleShape)
                                                )
                                            }
                                            if (eventDays.contains(day)) {
                                                Box(
                                                    Modifier
                                                        .size(6.dp)
                                                        .background(Color(0xFFA855F7), CircleShape)
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
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1438)),
                border = BorderStroke(1.dp, Color(0xFF3D3368))
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${weekdayName(jy, jm, selectedDay)} $selectedDay ${JalaliDate.jalaliMonthDisplay(selectedYearMonth)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        "مجموع درآمد: ${money(dayIncome)}",
                        color = Color(0xFF34C89E)
                    )
                    Text(
                        "مجموع هزینه: ${money(dayExpense)}",
                        color = Color(0xFFFF6B6B)
                    )
                    Text(
                        "خالص: ${money(dayIncome - dayExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (dayIncome - dayExpense >= 0) Color(0xFF34C89E)
                        else Color(0xFFFF6B6B)
                    )
                }
            }
        }
        item { Text("تراکنش‌های این روز", style = MaterialTheme.typography.titleMedium) }
        if (dayTransactions.isEmpty()) {
            item { Text("در این روز تراکنشی ثبت نشده است.") }
        }
        items(dayTransactions.sortedByDescending { it.dateMillis }) { transaction ->
            Card(
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
                        categoryIcon(transaction.category),
                        contentDescription = transaction.category,
                        tint = if (transaction.income) Color(0xFF16A34A)
                        else MaterialTheme.colorScheme.error
                    )
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(transaction.title)
                        Text(transaction.category, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        (if (transaction.income) "+" else "−") + money(transaction.amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (transaction.income) Color(0xFF16A34A)
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        item { Text("رویدادهای این روز", style = MaterialTheme.typography.titleMedium) }
        item {
            Button(onClick = { showEventDialog = true }) {
                Text("رویداد جدید")
            }
        }
        if (dayEvents.isEmpty()) {
            item { Text("در این روز رویدادی ثبت نشده است.") }
        } else {
            dayEvents.forEach { event ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(event.title, style = MaterialTheme.typography.titleMedium)
                                if (event.note.isNotBlank()) {
                                    Text(event.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        repository.deleteEvent(event.id)
                                        events = repository.getEvents()
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

    if (showEventDialog) {
        EventDialog(
            onDismiss = { showEventDialog = false },
            onSave = { title, note ->
                scope.launch {
                    repository.addEvent(
                        EventEntity(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            dateMillis = JalaliDate.jalaliToMillis(jy, jm, selectedDay),
                            note = note
                        )
                    )
                    events = repository.getEvents()
                    showEventDialog = false
                }
            }
        )
    }
}

@Composable
private fun EventDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("رویداد جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان رویداد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, note) },
                enabled = title.isNotBlank()
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        }
    )
}
