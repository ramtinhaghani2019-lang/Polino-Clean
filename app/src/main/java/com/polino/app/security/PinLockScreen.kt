package com.polino.app.security

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.delay

private val PinKeyContainer = Color(0xFF2A2150)
private val PinKeyBorder = Color(0xFF3D3368)
private val PinKeyLabel = Color(0xFFD8D0F5)
private val PinPurple = Color(0xFFA855F7)

/** صفحه‌کلید عددی مشترک قفل پین و تنظیم پین: کلیدهای تیره مستطیلی عین صفحه ثبت. */
@Composable
fun PinKeypad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean
) {
    val rows = listOf(
        listOf("۱" to "1", "۲" to "2", "۳" to "3"),
        listOf("۴" to "4", "۵" to "5", "۶" to "6"),
        listOf("۷" to "7", "۸" to "8", "۹" to "9")
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    PinKey(label = label, enabled = enabled, onClick = { onDigit(value) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onConfirm,
                enabled = enabled && confirmEnabled,
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinPurple,
                    contentColor = Color.White,
                    disabledContainerColor = PinKeyContainer,
                    disabledContentColor = PinKeyLabel.copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = "تأیید")
            }
            PinKey(label = "۰", enabled = enabled, onClick = { onDigit("0") })
            Button(
                onClick = onBackspace,
                enabled = enabled,
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A2020),
                    contentColor = Color(0xFFFF6B6B),
                    disabledContainerColor = PinKeyContainer,
                    disabledContentColor = PinKeyLabel.copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "پاک کردن")
            }
        }
    }
}

@Composable
private fun PinKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(72.dp)
            .border(1.dp, PinKeyBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PinKeyContainer,
            contentColor = PinKeyLabel,
            disabledContainerColor = PinKeyContainer,
            disabledContentColor = PinKeyLabel.copy(alpha = 0.4f)
        )
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

/** نقطه‌های پین واردشده؛ وقتی چیزی وارد نشده، فقط راهنما نشان داده می‌شود. */
@Composable
fun PinDots(entered: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp)
    ) {
        repeat(entered) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(PinPurple)
            )
        }
    }
}

private const val MAX_FAILED_ATTEMPTS = 5
private const val COOLDOWN_SECONDS = 30

/**
 * قفل پین در شروع برنامه: وقتی پین تنظیم شده باشد، به‌جای احراز بیومتریک
 * همین صفحه نشان داده می‌شود. پس از ۵ تلاش ناموفق، ۳۰ ثانیه قفل می‌شود.
 * اگر هش یا نمک ذخیره‌شده پیدا نشود (مثلاً پس از پاک‌سازی)، کاربر گیر
 * نمی‌کند و قفل باز می‌شود.
 */
@Composable
fun PinLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(LOCK_PREFS_NAME, Context.MODE_PRIVATE)
    }
    val storedHash = remember { prefs.getString(PIN_HASH_KEY, null) }
    val storedSalt = remember { prefs.getString(PIN_SALT_KEY, null) }

    if (storedHash.isNullOrBlank() || storedSalt.isNullOrBlank()) {
        LaunchedEffect(Unit) { onUnlocked() }
        return
    }

    var pin by remember { mutableStateOf("") }
    var failures by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var cooldownLeft by remember { mutableStateOf(0) }

    LaunchedEffect(cooldownLeft) {
        if (cooldownLeft > 0) {
            delay(1000)
            cooldownLeft -= 1
        }
    }

    fun submit() {
        if (pin.length < 4 || cooldownLeft > 0) return
        if (verifyPin(pin, storedSalt, storedHash)) {
            onUnlocked()
        } else {
            pin = ""
            val nextFailures = failures + 1
            if (nextFailures >= MAX_FAILED_ATTEMPTS) {
                failures = 0
                cooldownLeft = COOLDOWN_SECONDS
                error = "پس از ۵ تلاش ناموفق، ۳۰ ثانیه صبر کنید."
            } else {
                failures = nextFailures
                error = "پین اشتباه است."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("برنامه قفل است", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "پین برنامه را وارد کنید.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        PinDots(pin.length)
        Spacer(Modifier.height(8.dp))
        if (cooldownLeft > 0) {
            Text(
                "تلاش دوباره تا $cooldownLeft ثانیه دیگر",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else if (error != null) {
            Text(
                error!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                "۴ تا ۶ رقم",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        PinKeypad(
            enabled = cooldownLeft == 0,
            onDigit = { digit -> if (pin.length < 6) pin += digit },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            onConfirm = { submit() },
            confirmEnabled = pin.length >= 4
        )
    }
}
