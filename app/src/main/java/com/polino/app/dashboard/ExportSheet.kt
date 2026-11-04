package com.polino.app.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.polino.app.data.DebtEntity
import com.polino.app.data.MoneyTransaction
import java.util.Locale

private val ExportSheetDark = Color(0xFF1E1438)
private val ExportSheetSurface = Color(0xFF2A2150)
private val ExportSheetBorder = Color(0xFF3D3368)
private val ExportSheetMuted = Color(0xFFB3ACD6)
private val ExportSheetPurple = Color(0xFFA855F7)
private val ExportSheetGreen = Color(0xFF64C0AB)

/** بازه‌های ورق خروجی اکسل عین پرگار: ماه جاری، ماه قبل، ۳ و ۶ ماه اخیر. */
internal enum class ExportRange(val label: String) {
    THIS_MONTH("این ماه"),
    PREVIOUS_MONTH("ماه قبل"),
    LAST_3_MONTHS("۳ ماه اخیر"),
    LAST_6_MONTHS("۶ ماه اخیر")
}

/** محتوای ورق خروجی اکسل عین پرگار؛ پیش‌فرض همه تیک‌خورده‌اند. */
internal data class ExportContent(
    val summary: Boolean = true,
    val transactions: Boolean = true,
    val categories: Boolean = true,
    val receivables: Boolean = true,
    val payables: Boolean = true
) {
    internal val anySelected: Boolean
        get() = summary || transactions || categories || receivables || payables
}

internal data class ExportRangeBounds(
    val months: List<String>,
    val startMillis: Long,
    val endExclusiveMillis: Long,
    val fromLabel: String,
    val toLabel: String
)

internal fun persianDigits(input: String): String = input.map { ch ->
    if (ch in '0'..'9') ('۰'.code + ch.code - '0'.code).toChar() else ch
}.joinToString("")

private fun parseYearMonth(yearMonth: String): Pair<Int, Int>? {
    val parts = yearMonth.split("-")
    if (parts.size != 2) return null
    val jy = parts[0].toIntOrNull() ?: return null
    val jm = parts[1].toIntOrNull() ?: return null
    if (jm !in 1..12) return null
    return jy to jm
}

/**
 * مرزهای بازه خروجی: ماه‌های شمسی بازه (از قدیمی به جدید)، شروع روز اول
 * ماه اول، و پایان انحصاری (شروع ماه بعد از ماه آخر) برای فیلتر تراکنش‌ها،
 * به‌همراه برچسب‌های «از تاریخ» و «تا تاریخ» با ارقام فارسی.
 */
internal fun exportRangeBounds(range: ExportRange, currentYearMonth: String): ExportRangeBounds {
    val anchor = parseYearMonth(currentYearMonth)
        ?: parseYearMonth(JalaliDate.currentJalaliYearMonth())
        ?: (1405 to 1)
    val anchorText = "%04d-%02d".format(Locale.US, anchor.first, anchor.second)
    val months = when (range) {
        ExportRange.THIS_MONTH -> listOf(anchorText)
        ExportRange.PREVIOUS_MONTH -> listOf(JalaliDate.previousJalaliYearMonth(anchorText))
        ExportRange.LAST_3_MONTHS -> JalaliDate.lastJalaliYearMonths(3, anchorText)
        ExportRange.LAST_6_MONTHS -> JalaliDate.lastJalaliYearMonths(6, anchorText)
    }
    val (fromY, fromM) = parseYearMonth(months.first()) ?: anchor
    val (toY, toM) = parseYearMonth(months.last()) ?: anchor
    val start = JalaliDate.jalaliToMillis(fromY, fromM, 1)
    val nextY = if (toM == 12) toY + 1 else toY
    val nextM = if (toM == 12) 1 else toM + 1
    val endExclusive = JalaliDate.jalaliToMillis(nextY, nextM, 1)
    val fromLabel = persianDigits("%04d/%02d/01".format(Locale.US, fromY, fromM))
    val toLabel = persianDigits(
        "%04d/%02d/%02d".format(Locale.US, toY, toM, JalaliDate.daysInJalaliMonth(toY, toM))
    )
    return ExportRangeBounds(months, start, endExclusive, fromLabel, toLabel)
}

private fun debtExportSection(
    items: List<DebtEntity>,
    name: String,
    totalHeader: String,
    settledHeader: String
): XlsxSection {
    val grouped = items.groupBy { it.personName }.toSortedMap()
    val rows = grouped.map { (person, list) ->
        val settled = list.filter { it.settled }.sumOf { it.amount }
        val remaining = list.filterNot { it.settled }.sumOf { it.amount }
        listOf(
            XlsxCell.Text(person),
            XlsxCell.Number(settled + remaining),
            XlsxCell.Number(settled),
            XlsxCell.Number(remaining)
        )
    }
    return XlsxSection(
        name,
        listOf("شخص", totalHeader, settledHeader, "باقیمانده (تومان)"),
        rows
    )
}

/**
 * بخش‌های فایل اکسل از روی تراکنش‌های بازه و طلب/بدهی‌ها، به ترتیب ورق
 * پرگار: خلاصه مالی، تراکنش‌ها، دسته‌بندی‌ها، طلب‌ها، بدهی‌ها. فقط
 * بخش‌های تیک‌خورده ساخته می‌شوند؛ خلاصه از همان تراکنش‌های بازه حساب
 * می‌شود و دسته‌بندی‌ها فقط هزینه‌ها را تفکیک می‌کنند.
 */
internal fun buildExportSections(
    rangeTransactions: List<MoneyTransaction>,
    debts: List<DebtEntity>,
    content: ExportContent
): List<XlsxSection> = buildList {
    if (content.summary) {
        val totals = monthTotals(rangeTransactions)
        add(
            XlsxSection(
                "خلاصه مالی",
                listOf("شاخص", "مبلغ (تومان)"),
                listOf(
                    listOf(XlsxCell.Text("درآمد"), XlsxCell.Number(totals.income)),
                    listOf(XlsxCell.Text("هزینه"), XlsxCell.Number(totals.expense)),
                    listOf(
                        XlsxCell.Text("مانده"),
                        XlsxCell.Number(totals.income - totals.expense)
                    )
                )
            )
        )
    }
    if (content.transactions) {
        add(
            XlsxSection(
                "تراکنش‌ها",
                listOf("عنوان", "مبلغ", "نوع", "دسته", "تاریخ شمسی", "یادداشت"),
                rangeTransactions.map { t ->
                    listOf(
                        XlsxCell.Text(t.title),
                        XlsxCell.Number(t.amount),
                        XlsxCell.Text(if (t.income) "درآمد" else "هزینه"),
                        XlsxCell.Text(t.category),
                        XlsxCell.Text(JalaliDate.formatJalali(t.dateMillis)),
                        XlsxCell.Text(t.note)
                    )
                }
            )
        )
    }
    if (content.categories) {
        add(
            XlsxSection(
                "دسته‌بندی‌ها",
                listOf("دسته", "مبلغ هزینه (تومان)"),
                spentByCategory(rangeTransactions).map { (category, amount) ->
                    listOf(XlsxCell.Text(category), XlsxCell.Number(amount))
                }
            )
        )
    }
    if (content.receivables) {
        add(
            debtExportSection(
                debts.filter { it.isReceivable },
                "طلب‌ها",
                "مبلغ کل طلب (تومان)",
                "مبلغ وصول‌شده (تومان)"
            )
        )
    }
    if (content.payables) {
        add(
            debtExportSection(
                debts.filterNot { it.isReceivable },
                "بدهی‌ها",
                "مبلغ کل بدهی (تومان)",
                "مبلغ پرداخت‌شده (تومان)"
            )
        )
    }
}

/**
 * ورق «خروجی اطلاعات مالی (Excel)» عین ورق پرگار: قرص‌های بازه (این ماه،
 * ماه قبل، ۳ ماه اخیر، ۶ ماه اخیر) با نمایش از/تا تاریخ، و پنج ردیف
 * محتوای تیک‌خورده (خلاصه مالی، تراکنش‌ها، دسته‌بندی‌ها، طلب‌ها، بدهی‌ها)
 * با دکمه سبز «ایجاد فایل Excel». تأیید فقط بازه و محتوا را برمی‌گرداند؛
 * ساخت فایل با موتور اکسل موجود انجام می‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportSheet(
    onDismiss: () -> Unit,
    onConfirm: (ExportRange, ExportContent) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRange by remember { mutableStateOf(ExportRange.THIS_MONTH) }
    var content by remember { mutableStateOf(ExportContent()) }
    val bounds = exportRangeBounds(selectedRange, JalaliDate.currentJalaliYearMonth())
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ExportSheetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "خروجی اطلاعات مالی (Excel)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "تولید فایل رسمی با فرمت .xlsx",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExportSheetMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ExportSheetGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.GridOn,
                        contentDescription = null,
                        tint = ExportSheetGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "بستن")
                }
            }
            Text(
                "۱. بازه زمانی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportRange.entries.forEach { range ->
                    val selected = range == selectedRange
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) ExportSheetPurple else ExportSheetSurface
                            )
                            .border(
                                1.dp,
                                if (selected) ExportSheetPurple else ExportSheetBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedRange = range }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            range.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else ExportSheetMuted
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExportDateBox(
                    label = "از تاریخ",
                    value = bounds.fromLabel,
                    modifier = Modifier.weight(1f)
                )
                ExportDateBox(
                    label = "تا تاریخ",
                    value = bounds.toLabel,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "۲. اطلاعات مورد نظر برای خروجی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            ExportContentRow(
                title = "خلاصه مالی",
                subtitle = "شاخص‌های کلیدی، مانده و تراز بازه",
                checked = content.summary,
                onToggle = { content = content.copy(summary = !content.summary) }
            )
            ExportContentRow(
                title = "تراکنش‌ها",
                subtitle = "فهرست کامل درآمدها و هزینه‌ها",
                checked = content.transactions,
                onToggle = { content = content.copy(transactions = !content.transactions) }
            )
            ExportContentRow(
                title = "دسته‌بندی‌ها",
                subtitle = "تحلیل و تفکیک مخارج به دسته‌ها",
                checked = content.categories,
                onToggle = { content = content.copy(categories = !content.categories) }
            )
            ExportContentRow(
                title = "طلب‌ها (مطالبات)",
                subtitle = "لیست طلب‌ها، مبالغ وصول‌شده و باقیمانده",
                checked = content.receivables,
                onToggle = { content = content.copy(receivables = !content.receivables) }
            )
            ExportContentRow(
                title = "بدهی‌ها",
                subtitle = "لیست بدهی‌ها، مبالغ پرداخت‌شده و باقیمانده",
                checked = content.payables,
                onToggle = { content = content.copy(payables = !content.payables) }
            )
            Button(
                onClick = { onConfirm(selectedRange, content) },
                enabled = content.anySelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExportSheetGreen,
                    contentColor = Color(0xFF130F28),
                    disabledContainerColor = ExportSheetSurface,
                    disabledContentColor = ExportSheetMuted
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    "ایجاد فایل Excel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ExportDateBox(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ExportSheetSurface)
            .border(1.dp, ExportSheetBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = ExportSheetMuted
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.DateRange,
            contentDescription = null,
            tint = ExportSheetPurple,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ExportContentRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) ExportSheetGreen else Color.Transparent)
                .border(
                    2.dp,
                    if (checked) ExportSheetGreen else ExportSheetMuted,
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ExportSheetMuted
            )
        }
    }
}
