package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * ماشین‌حساب با اجرای فوری (بدون اولویت عملگر)، مثل ماشین‌حساب گوشی:
 * هر عملگر، نتیجه تا آن لحظه را حساب می‌کند. تقسیم بر صفر نادیده گرفته می‌شود.
 * ورودی اعشاری است (BigDecimal) تا ٪ و ممیز دقیق بمانند؛ نمایش داخلی همیشه
 * با ارقام لاتین و ممیز نقطه است و فقط موقع نمایش به ارقام فارسی تبدیل می‌شود.
 */
class CalculatorState {
    var display: String = ""
        private set
    private var accumulator: BigDecimal? = null
    private var pendingOp: Char? = null
    private var fresh: Boolean = true

    private val value: BigDecimal
        get() = display.toBigDecimalOrNull() ?: BigDecimal.ZERO

    /** ارقام تکی یا «۰۰»/«۰۰۰»؛ حداکثر ۱۲ رقم. */
    fun inputDigit(digit: String) {
        val digits = digit.filter { it.isDigit() }
        if (digits.isEmpty()) return
        if (fresh) {
            display = digits.trimStart('0').ifEmpty { "0" }
            fresh = false
        } else {
            val currentDigits = display.count { it.isDigit() }
            if (currentDigits + digits.length <= 12) {
                display = if (display == "0" || display == "-0") {
                    (if (display.startsWith("-")) "-" else "") +
                        digits.trimStart('0').ifEmpty { "0" }
                } else {
                    display + digits
                }
            }
        }
    }

    fun inputDecimalPoint() {
        if (fresh) {
            display = "0."
            fresh = false
        } else if (!display.contains('.')) {
            display = if (display.isEmpty() || display == "-") "0." else "$display."
        }
    }

    fun inputBackspace() {
        if (display.isEmpty()) return
        display = display.dropLast(1)
        if (display.isEmpty() || display == "-") {
            display = ""
            fresh = true
        } else {
            fresh = false
        }
    }

    /** مقدار فعلی تقسیم بر ۱۰۰؛ روی همان ورودی اعمال می‌شود. */
    fun inputPercent() {
        if (display.isEmpty()) return
        display = formatCalcResult(value.divide(BigDecimal(100)))
        fresh = false
    }

    /** علامت ورودی فعلی را برعکس می‌کند؛ روی خالی و صفر بی‌اثر است. */
    fun inputToggleSign() {
        if (display.isEmpty() || display == "-") return
        if (value.signum() == 0) return
        display = if (display.startsWith("-")) display.removePrefix("-") else "-$display"
        fresh = false
    }

    fun inputOp(op: Char) {
        val current = value
        val acc = accumulator
        if (acc == null) {
            accumulator = current
        } else if (!fresh) {
            accumulator = applyOp(acc, current, pendingOp)
        }
        pendingOp = op
        fresh = true
        display = formatCalcResult(accumulator!!)
    }

    fun inputEquals() {
        val acc = accumulator
        val op = pendingOp
        if (acc != null && op != null) {
            display = formatCalcResult(applyOp(acc, value, op))
            accumulator = null
            pendingOp = null
            fresh = true
        }
    }

    fun clear() {
        display = ""
        accumulator = null
        pendingOp = null
        fresh = true
    }

    private fun applyOp(a: BigDecimal, b: BigDecimal, op: Char?): BigDecimal = when (op) {
        '+' -> a + b
        '−' -> a - b
        '×' -> a * b
        '÷' -> if (b.signum() == 0) a else a.divide(b, 10, RoundingMode.HALF_UP)
        else -> b
    }
}

/** نتیجه BigDecimal را بدون صفرهای اضافی و بدون نماد علمی به رشته تبدیل می‌کند. */
internal fun formatCalcResult(value: BigDecimal): String {
    if (value.signum() == 0) return "0"
    return value.stripTrailingZeros().toPlainString()
}

/**
 * نمایش ماشین‌حساب برای کاربر: ارقام فارسی، جداکننده هزارگان، ممیز فارسی ٫،
 * و پسوند تومان. ورودی خالی «۰ تومان» نشان می‌دهد.
 */
internal fun formatCalcDisplay(display: String): String {
    if (display.isEmpty() || display == "-") return "۰ تومان"
    val bd = display.toBigDecimalOrNull() ?: return "۰ تومان"
    if (bd.signum() == 0) return "۰ تومان"
    val plain = formatCalcResult(bd)
    val parts = plain.split(".")
    val intPart = NumberFormat.getIntegerInstance(Locale("fa", "IR"))
        .format(parts[0].toLongOrNull() ?: 0L)
    val fraction = if (parts.size > 1) {
        "٫" + parts[1].map { c ->
            if (c in '0'..'9') ('۰'.code + c.code - '0'.code).toChar() else c
        }.joinToString("")
    } else {
        ""
    }
    return intPart + fraction + " تومان"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    onDismiss: () -> Unit,
    onUseAmount: (String) -> Unit
) {
    val calc = remember { CalculatorState() }
    var shown by remember { mutableStateOf(calc.display) }
    fun press(action: () -> Unit) {
        action()
        shown = calc.display
    }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    val isDarkCalc = MaterialTheme.colorScheme.background.luminance() < 0.5f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkCalc) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // مرحله ۱۱۵: سربرگ عین پرگار — کاشی آیکون بنفش + تیتر + زیرعنوان + ✕.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFA855F7), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Calculate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "ماشین حساب مالی",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkCalc) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "محاسبات سریع و هوشمند",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkCalc) Color(0xFFB3ACD6) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "بستن",
                        tint = if (isDarkCalc) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                formatCalcDisplay(shown),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkCalc) Color.White else MaterialTheme.colorScheme.onSurface
            )
            val rows = listOf(
                listOf("AC" to "AC", "⌫" to "BACK", "%" to "%", "÷" to "÷"),
                listOf("۷" to "7", "۸" to "8", "۹" to "9", "×" to "×"),
                listOf("۴" to "4", "۵" to "5", "۶" to "6", "−" to "−"),
                listOf("۱" to "1", "۲" to "2", "۳" to "3", "+" to "+"),
                listOf("+/-" to "+/-", "۰" to "0", "." to ".", "=" to "="),
                listOf("۰۰" to "00", "۰۰۰" to "000")
            )
            // مرحله ۱۱۵: کیپد فقط همین‌جا چپ‌به‌راست (عین پرگار)؛ بقیه اپ راست‌به‌چپ می‌ماند.
            // رنگ‌ها نمونه‌برداری‌شده از فریم g_51 پرگار: رقم #14142E، قرمز #3C2339،
            // عملگر بنفش #2B1752، مساوی #9133E9.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, value) ->
                                val containerColor = when (value) {
                                    "=" -> Color(0xFF9133E9)
                                    "AC", "BACK", "%", "+/-" -> Color(0xFF3C2339)
                                    "÷", "×", "−", "+" -> Color(0xFF2B1752)
                                    else -> Color(0xFF14142E)
                                }
                                val contentColor = when (value) {
                                    "=" -> Color.White
                                    "AC", "BACK", "%", "+/-" -> Color(0xFFF87171)
                                    "÷", "×", "−", "+" -> Color(0xFFA855F7)
                                    else -> Color.White
                                }
                                val keyBorder = when (value) {
                                    "=" -> null
                                    "AC", "BACK", "%", "+/-" -> BorderStroke(1.dp, Color(0xFF7F2D2D))
                                    else -> BorderStroke(1.dp, Color(0xFF3D3368))
                                }
                                Button(
                                    onClick = {
                                        when (value) {
                                            "AC" -> press { calc.clear() }
                                            "BACK" -> press { calc.inputBackspace() }
                                            "%" -> press { calc.inputPercent() }
                                            "+/-" -> press { calc.inputToggleSign() }
                                            "." -> press { calc.inputDecimalPoint() }
                                            "=" -> press { calc.inputEquals() }
                                            "+", "−", "×", "÷" -> press {
                                                calc.inputOp(value.first())
                                            }
                                            else -> press { calc.inputDigit(value) }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = containerColor,
                                        contentColor = contentColor
                                    ),
                                    border = keyBorder
                                ) {
                                    Text(label, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
            // مرحله ۱۱۵: سه دکمه می‌مانند؛ ارتفاع ۵۶dp استاندارد دکمه‌های اپ.
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(shown.ifEmpty { "0" }))
                    copied = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (copied) "کپی شد ✓" else "کپی نتیجه",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Button(
                onClick = {
                    val amount = shown.toBigDecimalOrNull()
                        ?.setScale(0, RoundingMode.HALF_UP)
                        ?.toLong()
                        ?.takeIf { it > 0L }
                        ?.toString()
                    if (amount != null) onUseAmount(amount)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA855F7),
                    contentColor = Color.White
                )
            ) {
                Text("استفاده در ثبت", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("بستن") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
