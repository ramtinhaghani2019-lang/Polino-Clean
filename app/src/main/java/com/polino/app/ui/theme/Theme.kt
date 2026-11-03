package com.polino.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.polino.app.R

private val Emerald = Color(0xFF0B6B57)
private val DeepGreen = Color(0xFF083F35)
private val Gold = Color(0xFFE3B04B)
private val Cream = Color(0xFFF7F4EA)

// سطوح گرمِ هم‌خانواده کرم؛ جایگزین تناژهای پیش‌فرض بنفش‌مایه متریال که کارت‌ها را خاکستری‌-بنفش نشان می‌داد.
private val WarmSand = Color(0xFFEDE7DA)
private val WarmCard = Color(0xFFF5F0E6)
private val WarmHigh = Color(0xFFE3DCCB)
private val WarmHighest = Color(0xFFD8D0BC)
private val WarmMuted = Color(0xFF6F6A5E)

// پالت لوکس مرحله ۳۳: نیمه‌شب + طلایی + زمرد روشن؛ متفاوت از بنفش پرگار و در عین حال هم‌خانواده هویت پولینو.
private val Midnight = Color(0xFF0A0E1D)
private val MidnightSurface = Color(0xFF141B31)
private val MidnightVariant = Color(0xFF1E2747)
private val MintEmerald = Color(0xFF34C89E)
private val PremiumGold = Color(0xFFE3B04B)
private val SoftWhite = Color(0xFFF2F4F8)
private val MutedGray = Color(0xFF9AA3B8)
private val SoftRed = Color(0xFFFF6B6B)

private val LightColors = lightColorScheme(
    primary = Emerald,
    secondary = Gold,
    background = Cream,
    surface = Color.White,
    surfaceVariant = WarmSand,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WarmCard,
    surfaceContainer = WarmSand,
    surfaceContainerHigh = WarmHigh,
    surfaceContainerHighest = WarmHighest,
    surfaceTint = WarmSand,
    outline = WarmHighest,
    outlineVariant = WarmSand,
    onPrimary = Color.White,
    onSecondary = DeepGreen,
    onBackground = DeepGreen,
    onSurface = DeepGreen,
    onSurfaceVariant = WarmMuted
)

private val DarkColors = darkColorScheme(
    primary = MintEmerald,
    secondary = PremiumGold,
    background = Midnight,
    surface = MidnightSurface,
    surfaceVariant = MidnightVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = SoftWhite,
    onSurface = SoftWhite,
    onSurfaceVariant = MutedGray,
    error = SoftRed
)

private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

/**
 * فونت وزیرمتن را روی همه سبک‌های متنی می‌نشاند. (پارامتر defaultFontFamily
 * مال متریال ۲ است و در متریال ۳ وجود ندارد؛ به همین دلیل بیلد ۳۷ قرمز شد.)
 */
private fun withVazirmatn(typography: Typography): Typography = Typography(
    displayLarge = typography.displayLarge.copy(fontFamily = Vazirmatn),
    displayMedium = typography.displayMedium.copy(fontFamily = Vazirmatn),
    displaySmall = typography.displaySmall.copy(fontFamily = Vazirmatn),
    headlineLarge = typography.headlineLarge.copy(fontFamily = Vazirmatn),
    headlineMedium = typography.headlineMedium.copy(fontFamily = Vazirmatn),
    headlineSmall = typography.headlineSmall.copy(fontFamily = Vazirmatn),
    titleLarge = typography.titleLarge.copy(fontFamily = Vazirmatn),
    titleMedium = typography.titleMedium.copy(fontFamily = Vazirmatn),
    titleSmall = typography.titleSmall.copy(fontFamily = Vazirmatn),
    bodyLarge = typography.bodyLarge.copy(fontFamily = Vazirmatn),
    bodyMedium = typography.bodyMedium.copy(fontFamily = Vazirmatn),
    bodySmall = typography.bodySmall.copy(fontFamily = Vazirmatn),
    labelLarge = typography.labelLarge.copy(fontFamily = Vazirmatn),
    labelMedium = typography.labelMedium.copy(fontFamily = Vazirmatn),
    labelSmall = typography.labelSmall.copy(fontFamily = Vazirmatn)
)

private val PolinoTypography: Typography
    get() = withVazirmatn(Typography())

/** حالت تم ذخیره‌شده: system / light / dark. */
const val THEME_PREFS_NAME = "polino_prefs"
const val THEME_MODE_KEY = "theme_mode"

/** نگاشت حالت تم به تیره/روشن؛ system تابع تم دستگاه است. */
internal fun isDarkThemeForMode(mode: String, systemDark: Boolean): Boolean = when (mode) {
    "dark" -> true
    "light" -> false
    else -> systemDark
}

@Composable
fun PolinoTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isDarkThemeForMode(themeMode, isSystemInDarkTheme())) {
            DarkColors
        } else {
            LightColors
        },
        typography = PolinoTypography,
        content = content
    )
}

