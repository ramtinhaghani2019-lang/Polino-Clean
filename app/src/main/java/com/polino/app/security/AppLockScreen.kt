package com.polino.app.security

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

const val LOCK_PREFS_NAME = "polino_prefs"
const val LOCK_ENABLED_KEY = "app_lock_enabled"

/**
 * صفحه قفل برنامه: تا وقتی کاربر با اثر انگشت یا رمز دستگاه احراز نشود،
 * محتوای برنامه نشان داده نمی‌شود. در صورت لغو یا خطا، کاربر قفل نمی‌شود؛
 * یک دکمه «تلاش دوباره» می‌بیند و می‌تواند دوباره تلاش کند.
 */
@Composable
fun AppLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var promptRequested by remember { mutableStateOf(false) }

    fun showPrompt() {
        val fragmentActivity = activity ?: return
        val executor = ContextCompat.getMainExecutor(fragmentActivity)
        val prompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    errorMessage = errString.toString()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("قفل پولینو")
            .setSubtitle("برای دیدن اطلاعات مالی، احراز هویت کنید")
            .setAllowedAuthenticators(
                BiometricPrompt.Authenticators.BIOMETRIC_STRONG or
                    BiometricPrompt.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(promptRequested) {
        if (!promptRequested) {
            promptRequested = true
            showPrompt()
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
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("برنامه قفل است", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            errorMessage ?: "برای ادامه، با اثر انگشت یا رمز دستگاه احراز هویت کنید.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { showPrompt() }) {
            Text("باز کردن قفل")
        }
    }
}
