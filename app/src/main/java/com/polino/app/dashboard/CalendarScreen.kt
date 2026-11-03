package com.polino.app.dashboard

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.TransactionRepository
import com.polino.app.data.nextJalaliYearMonth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    var selectedYearMonth by remember { mutableStateOf(JalaliDate.currentJalaliYearMonth()) }
    var selectedDay by remember { mutableStateOf(todayJalaliDay()) }

    LaunchedEffect(Unit) {
        transactions = runCatching { repository.getAll() }.getOrDefault(emptyList())
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
                                                if (isSelected) Color(0xFFE3B04B)
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
                                            color = if (isSelected) Color.Black
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            JalaliDate.jalaliToLocalDate(jy, jm, day).dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) Color.Black
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1730))
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
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        categoryIcon(transaction.category),
                        contentDescription = transaction.category,
                        tint = if (transaction.income) MaterialTheme.colorScheme.primary
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
                        color = if (transaction.income) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
