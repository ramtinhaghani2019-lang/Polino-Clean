package com.polino.app.security

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

private val SheetDark = Color(0xFF1E1438)
private val SheetBorder = Color(0xFF3D3368)
private val SheetMuted = Color(0xFFB3ACD6)
private val SheetPurple = Color(0xFFA855F7)
private val SheetRed = Color(0xFFFB7185)

private enum class SetupStep { VERIFY_OLD, ENTER_NEW, CONFIRM_NEW }

/**
 * ورق «تنظیمات امنیت و حریم خصوصی» عین ورق پرگار: دو کلید قفل با پین‌کد و
 * تشخیص چهره / اثر انگشت، اقدام قرمز حذف و بازنشانی کامل، دکمه بنفش بستن.
 * پین فقط به‌صورت هش نمکی در polino_prefs ذخیره می‌شود، هرگز به‌صورت متن ساده.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySheet(
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(LOCK_PREFS_NAME, Context.MODE_PRIVATE)
    }
    var pinSet by remember {
        mutableStateOf(
            prefs.contains(PIN_HASH_KEY) && prefs.contains(PIN_SALT_KEY)
        )
    }
    var setupStep by remember { mutableStateOf<SetupStep?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var setupError by remember { mutableStateOf<String?>(null) }
    var showWipeWarning by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    fun clearPin() {
        prefs.edit().remove(PIN_HASH_KEY).remove(PIN_SALT_KEY).apply()
        pinSet = false
    }

    fun savePin(pin: String) {
        val salt = generatePinSalt()
        prefs.edit()
            .putString(PIN_SALT_KEY, salt)
            .putString(PIN_HASH_KEY, hashPin(pin, salt))
            .apply()
        pinSet = true
    }

    fun submitSetupPin() {
        if (enteredPin.length < 4) return
        when (setupStep) {
            SetupStep.VERIFY_OLD -> {
                val hash = prefs.getString(PIN_HASH_KEY, null)
                val salt = prefs.getString(PIN_SALT_KEY, null)
                if (!hash.isNullOrBlank() && !salt.isNullOrBlank() &&
                    verifyPin(enteredPin, salt, hash)
                ) {
                    enteredPin = ""
                    setupError = null
                    setupStep = SetupStep.ENTER_NEW
                } else {
                    enteredPin = ""
                    setupError = "پین فعلی اشتباه است."
                }
            }
            SetupStep.ENTER_NEW -> {
                if (!isValidPin(enteredPin)) {
                    setupError = "پین باید ۴ تا ۶ رقم باشد."
                } else {
                    newPin = enteredPin
                    enteredPin = ""
                    setupError = null
                    setupStep = SetupStep.CONFIRM_NEW
                }
            }
            SetupStep.CONFIRM_NEW -> {
                if (enteredPin == newPin) {
                    savePin(newPin)
                    newPin = ""
                    enteredPin = ""
                    setupError = null
                    setupStep = null
                } else {
                    newPin = ""
                    enteredPin = ""
                    setupError = "پین‌ها مطابقت ندارند؛ دوباره وارد کنید."
                    setupStep = SetupStep.ENTER_NEW
                }
            }
            null -> Unit
        }
    }

    fun cancelSetup() {
        setupStep = null
        enteredPin = ""
        newPin = ""
        setupError = null
    }

    fun wipeAllData() {
        context.deleteDatabase("polino_db")
        context.deleteDatabase("polino_budgets_db")
        prefs.edit().clear().apply()
        context.getSharedPreferences("polino_finance_v1", Context.MODE_PRIVATE)
            .edit().clear().apply()
        (context as? Activity)?.recreate()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (setupStep == null) {
                Text(
                    "تنظیمات امنیت و حریم خصوصی",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "قفل برنامه با پین‌کد",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = pinSet,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!pinSet) setupStep = SetupStep.ENTER_NEW
                            } else {
                                clearPin()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = SheetPurple,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = SheetBorder,
                            uncheckedThumbColor = SheetMuted
                        )
                    )
                }
                if (pinSet) {
                    TextButton(
                        onClick = { setupStep = SetupStep.VERIFY_OLD },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("تغییر پین", color = SheetPurple)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "تشخیص چهره / اثر انگشت",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = onBiometricChange,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = SheetPurple,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = SheetBorder,
                            uncheckedThumbColor = SheetMuted
                        )
                    )
                }
                HorizontalDivider(color = SheetBorder)
                TextButton(
                    onClick = { showWipeWarning = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "حذف و بازنشانی کامل اطلاعات مالی",
                        style = MaterialTheme.typography.titleMedium,
                        color = SheetRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SheetPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("بستن")
                }
            } else {
                val setupTitle = when (setupStep) {
                    SetupStep.VERIFY_OLD -> "پین فعلی را وارد کنید"
                    SetupStep.ENTER_NEW -> "پین جدید را وارد کنید"
                    SetupStep.CONFIRM_NEW -> "پین جدید را دوباره وارد کنید"
                    null -> ""
                }
                Text(
                    setupTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PinDots(enteredPin.length)
                    if (setupError != null) {
                        Text(
                            setupError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SheetRed,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            "۴ تا ۶ رقم",
                            style = MaterialTheme.typography.bodySmall,
                            color = SheetMuted
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PinKeypad(
                        enabled = true,
                        onDigit = { digit ->
                            if (enteredPin.length < 6) enteredPin += digit
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        onConfirm = { submitSetupPin() },
                        confirmEnabled = enteredPin.length >= 4
                    )
                }
                TextButton(
                    onClick = { cancelSetup() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("انصراف", color = SheetMuted)
                }
            }
        }
    }

    if (showWipeWarning) {
        AlertDialog(
            onDismissRequest = { showWipeWarning = false },
            title = { Text("حذف و بازنشانی کامل اطلاعات مالی") },
            text = {
                Text(
                    "تمام اطلاعات مالی ذخیره‌شده در برنامه — تراکنش‌ها، حساب‌ها، " +
                        "دسته‌ها، بودجه‌ها، اهداف، اقساط، بدهی‌ها، کارها، رویدادها، " +
                        "دارایی‌ها، الگوهای پیامک و یادآورها — و همه تنظیمات برنامه " +
                        "پاک می‌شود. فایل‌های خروجی‌ای که قبلاً در حافظه دستگاه " +
                        "ذخیره کرده‌اید (مانند فایل‌های پشتیبان و اکسل) حذف نمی‌شوند."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeWarning = false
                        showWipeConfirm = true
                    }
                ) { Text("ادامه") }
            },
            dismissButton = {
                TextButton(onClick = { showWipeWarning = false }) { Text("انصراف") }
            }
        )
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("مطمئن هستید؟") },
            text = {
                Text(
                    "این عمل قابل بازگشت نیست و همه اطلاعات مالی شما " +
                        "برای همیشه پاک می‌شود."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeConfirm = false
                        wipeAllData()
                    }
                ) { Text("حذف کامل", color = SheetRed) }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text("انصراف") }
            }
        )
    }
}
