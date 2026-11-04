package com.polino.app.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * ماشین‌حساب ساده با اجرای فوری (بدون اولویت عملگر)، مثل ماشین‌حساب گوشی:
 * هر عملگر، نتیجه تا آن لحظه را حساب می‌کند. تقسیم بر صفر نادیده گرفته می‌شود.
 */
class CalculatorState {
    var display: String = ""
        private set
    private var accumulator: Long? = null
    private var pendingOp: Char? = null
    private var fresh: Boolean = true

    private val value: Long
        get() = display.toLongOrNull() ?: 0L

    fun inputDigit(digit: String) {
        if (fresh) {
            display = digit
            fresh = false
        } else if (display.length + digit.length <= 12) {
            display += digit
        }
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
        display = accumulator.toString()
    }

    fun inputEquals() {
        val acc = accumulator
        val op = pendingOp
        if (acc != null && op != null) {
            display = applyOp(acc, value, op).toString()
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

    private fun applyOp(a: Long, b: Long, op: Char?): Long = when (op) {
        '+' -> a + b
        '−' -> a - b
        '×' -> a * b
        '÷' -> if (b == 0L) a else a / b
        else -> b
    }
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E1438) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (shown.isEmpty()) "۰ تومان" else money(shown.toLongOrNull() ?: 0L),
                style = MaterialTheme.typography.displaySmall
            )
            val rows = listOf(
                listOf("۱" to "1", "۲" to "2", "۳" to "3", "÷" to "÷"),
                listOf("۴" to "4", "۵" to "5", "۶" to "6", "×" to "×"),
                listOf("۷" to "7", "۸" to "8", "۹" to "9", "−" to "−"),
                listOf("C" to "C", "۰" to "0", "=" to "=", "+" to "+")
            )
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, value) ->
                        OutlinedButton(
                            onClick = {
                                when (value) {
                                    "C" -> press { calc.clear() }
                                    "=" -> press { calc.inputEquals() }
                                    "+", "−", "×", "÷" -> press {
                                        calc.inputOp(value.first())
                                    }
                                    else -> press { calc.inputDigit(value) }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val amount = shown.toLongOrNull()?.takeIf { it > 0L }?.toString()
                    if (amount != null) onUseAmount(amount)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA855F7),
                    contentColor = Color.White
                )
            ) {
                Text("استفاده در ثبت", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("بستن") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
