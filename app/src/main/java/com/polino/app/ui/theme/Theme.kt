package com.polino.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.polino.app.R

// مرحله ۴۷: زبان طراحی پرگار با بنفش — انتخاب خود کاربر (۱۲ آبان ۱۴۰۵).
// تیره: بنفش-سرمه‌ای عمیق با کارت‌های بوردر‌دار؛ روشن: همان زبان با سطوح روشنِ بنفش‌مایه.
private val PurplePrimary = Color(0xFFA855F7)
private val PurpleLight = Color(0xFFC084FC)
private val PurpleDarkBg = Color(0xFF130F28)
private val PurpleDarkLowest = Color(0xFF0F0B22)
private val PurpleDarkLow = Color(0xFF171233)
private val PurpleDarkSurface = Color(0xFF1E1838)
private val PurpleDarkHigh = Color(0xFF2A2150)
private val PurpleDarkHighest = Color(0xFF372A63)
private val PurpleDarkContainer = Color(0xFF2E1A5E)
private val PurpleDarkOnContainer = Color(0xFFE9D5FF)
private val PurpleDarkMuted = Color(0xFFB3ACD6)
private val PurpleDarkOutline = Color(0xFF3D3368)
private val PurpleSoftWhite = Color(0xFFF4F2FF)
private val PurpleLightBg = Color(0xFFFAF8FF)
private val PurpleLightVariant = Color(0xFFEDE9FE)
private val PurpleLightLow = Color(0xFFF5F3FF)
private val PurpleLightHigh = Color(0xFFDDD6FE)
private val PurpleLightHighest = Color(0xFFC4B5FD)
private val PurpleDeep = Color(0xFF7C3AED)
private val PurpleMid = Color(0xFF8B5CF6)
private val PurpleInk = Color(0xFF1E1B36)
private val PurpleLightMuted = Color(0xFF6B6584)
private val PurpleLightOutline = Color(0xFFD8D0F5)
private val PurpleLightOnContainer = Color(0xFF4C1D95)
private val SoftRed = Color(0xFFFB7185)
private val StrongRed = Color(0xFFDC2626)

private val LightColors = lightColorScheme(
    primary = PurpleDeep,
    secondary = PurpleMid,
    background = PurpleLightBg,
    surface = Color.White,
    surfaceVariant = PurpleLightVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = PurpleLightLow,
    surfaceContainer = PurpleLightVariant,
    surfaceContainerHigh = PurpleLightHigh,
    surfaceContainerHighest = PurpleLightHighest,
    surfaceTint = PurpleLightVariant,
    primaryContainer = PurpleLightVariant,
    onPrimaryContainer = PurpleLightOnContainer,
    outline = PurpleLightOutline,
    outlineVariant = PurpleLightVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = PurpleInk,
    onSurface = PurpleInk,
    onSurfaceVariant = PurpleLightMuted,
    error = StrongRed,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = PurplePrimary,
    secondary = PurpleLight,
    background = PurpleDarkBg,
    surface = PurpleDarkSurface,
    surfaceVariant = PurpleDarkHigh,
    surfaceContainerLowest = PurpleDarkLowest,
    surfaceContainerLow = PurpleDarkLow,
    surfaceContainer = PurpleDarkSurface,
    surfaceContainerHigh = PurpleDarkHigh,
    surfaceContainerHighest = PurpleDarkHighest,
    primaryContainer = PurpleDarkContainer,
    onPrimaryContainer = PurpleDarkOnContainer,
    outline = PurpleDarkOutline,
    outlineVariant = PurpleDarkHigh,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = PurpleSoftWhite,
    onSurface = PurpleSoftWhite,
    onSurfaceVariant = PurpleDarkMuted,
    error = SoftRed,
    errorContainer = Color(0xFF3F1015),
    onErrorContainer = Color(0xFFFECDD3)
)

private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

/**
 * مرحله ۷۰: مقیاس تایپوگرافی پرگار — همان اندازه‌های پیش‌فرض متریال ۳ (که با
 * مقادیر اندازه‌گیری‌شده از عکس‌های پرگار می‌خواند: تیتر بخش ۱۶sp، تیتر صفحه
 * ۲۴sp) ولی با وزن Bold برای تیترها، سرخط‌ها، اعداد بزرگ و متن دکمه‌ها.
 * پرگار تیترهایش را توپر و درشت نشان می‌دهد؛ متریال ۳ آن‌ها را Regular/Medium
 * می‌گذارد و همین اختلاف، محرک اصلی PARTIALها در ممیزی عکس‌به‌عکس بود.
 * اندازه‌ها عوض نشده‌اند تا چیدمان صفحه‌های تأییدشده (مثل مبلغ صفحه ثبت)
 * به‌هم نریزد؛ وزن‌ها در سطح تم عوض شده‌اند تا همه صفحه‌ها یکجا ارث ببرند.
 */
private fun withVazirmatn(typography: Typography): Typography = Typography(
    displayLarge = typography.displayLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    displayMedium = typography.displayMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    displaySmall = typography.displaySmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    headlineLarge = typography.headlineLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    headlineMedium = typography.headlineMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    headlineSmall = typography.headlineSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    titleLarge = typography.titleLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    titleMedium = typography.titleMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    titleSmall = typography.titleSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    bodyLarge = typography.bodyLarge.copy(fontFamily = Vazirmatn),
    bodyMedium = typography.bodyMedium.copy(fontFamily = Vazirmatn),
    bodySmall = typography.bodySmall.copy(fontFamily = Vazirmatn),
    labelLarge = typography.labelLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    labelMedium = typography.labelMedium.copy(fontFamily = Vazirmatn),
    labelSmall = typography.labelSmall.copy(fontFamily = Vazirmatn)
)

private val PolinoTypography: Typography
    get() = withVazirmatn(Typography())

/**
 * مرحله ۷۰: زبان کارت پرگار در سطح تم — کارت‌های بدون شکل صریح، به‌جای
 * گوشه‌های ۱۲/۱۶dp پیش‌فرض متریال ۳، گوشه‌های ۲۰dp (کارت محتوا) و ۲۸dp
 * (کارت قهرمان/برگه) می‌گیرند؛ همان مقادیری که مرحله ۶۹ برای خانه اندازه
 * گرفت و مرحله‌های ۶۱ تا ۶۶ برای بقیه کارت‌ها صریح گذاشته بودند.
 */
private val PolinoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * مرحله ۷۰: محو آبی→بنفش صفحه خانه (مرحله ۶۹)، تعمیم‌یافته به همه صفحه‌های
 * اصلی در تم تیره؛ تم روشن پس‌زمینه تم را نگه می‌دارد.
 */
private val PergarPageWash = Brush.linearGradient(
    colors = listOf(Color(0xFF12377E), Color(0xFF7C3AED), Color(0xFF150F2E)),
    start = Offset.Zero,
    end = Offset.Infinite
)

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
    val dark = isDarkThemeForMode(themeMode, isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (dark) {
            DarkColors
        } else {
            LightColors
        },
        typography = PolinoTypography,
        shapes = PolinoShapes
    ) {
        // پس‌زمینه یکپارچه همه صفحه‌ها: در تم تیره محو آبی→بنفش پرگار،
        // در تم روشن همان پس‌زمینه تم. اسکافولد باید شفاف باشد تا این
        // پس‌زمینه دیده شود (MainActivity). صفحه خانه گرادیان خودش را با
        // همین رنگ‌ها روی همین پس‌زمینه می‌کشد، پس دو لایه یکسان دیده
        // می‌شوند و تضادی ندارد.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (dark) PergarPageWash else SolidColor(MaterialTheme.colorScheme.background))
        ) {
            content()
        }
    }
}

