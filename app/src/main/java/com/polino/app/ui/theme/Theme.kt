package com.polino.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Emerald = Color(0xFF0B6B57)
private val DeepGreen = Color(0xFF083F35)
private val Gold = Color(0xFFE3B04B)
private val Cream = Color(0xFFF7F4EA)

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
    onPrimary = Color.White,
    onSecondary = DeepGreen,
    onBackground = DeepGreen,
    onSurface = DeepGreen
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

@Composable
fun PolinoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            DarkColors
        } else {
            LightColors
        },
        content = content
    )
}

