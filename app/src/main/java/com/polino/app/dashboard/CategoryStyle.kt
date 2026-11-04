package com.polino.app.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.polino.app.data.CategoryEntity

/**
 * گزینه‌های آیکون برگه ویرایش دسته، بر اساس شبکه آیکون پرگار
 * (فریم‌های g_26 و g_27 ویدیوی پرگار: شبکه ۵ ستونی آیکون‌های خطی
 * سفید روی زمینه تیره). کلیدها در پایگاه و پشتیبان ذخیره می‌شوند، پس
 * هرگز تغییرشان نده و حذفشان نکن.
 */
internal data class CategoryIconOption(val key: String, val icon: ImageVector)

internal val CATEGORY_ICON_OPTIONS = listOf(
    CategoryIconOption("bag", Icons.Filled.ShoppingBag),
    CategoryIconOption("car", Icons.Filled.DirectionsCar),
    CategoryIconOption("receipt", Icons.Filled.Receipt),
    CategoryIconOption("medical", Icons.Filled.LocalHospital),
    CategoryIconOption("money", Icons.Filled.AttachMoney),
    CategoryIconOption("game", Icons.Filled.SportsEsports),
    CategoryIconOption("card", Icons.Filled.CreditCard),
    CategoryIconOption("wallet", Icons.Filled.AccountBalanceWallet),
    CategoryIconOption("gift", Icons.Filled.CardGiftcard),
    CategoryIconOption("store", Icons.Filled.Store),
    CategoryIconOption("piggy", Icons.Filled.Savings),
    CategoryIconOption("coffee", Icons.Filled.LocalCafe),
    CategoryIconOption("flight", Icons.Filled.Flight),
    CategoryIconOption("home", Icons.Filled.Home),
    CategoryIconOption("school", Icons.Filled.School),
    CategoryIconOption("pets", Icons.Filled.Pets),
    CategoryIconOption("phone", Icons.Filled.PhoneAndroid),
    CategoryIconOption("fuel", Icons.Filled.LocalGasStation),
    CategoryIconOption("fitness", Icons.Filled.FitnessCenter),
    CategoryIconOption("chart", Icons.Filled.TrendingUp),
    CategoryIconOption("shapes", Icons.Filled.Category),
    CategoryIconOption("people", Icons.Filled.People),
    CategoryIconOption("party", Icons.Filled.Celebration),
    CategoryIconOption("wrench", Icons.Filled.Build),
    CategoryIconOption("leaf", Icons.Filled.Eco)
)

/** آیکون یک کلید، یا تهی اگر کلید در فهرست نباشد. */
internal fun categoryIconByKey(key: String): ImageVector? =
    CATEGORY_ICON_OPTIONS.firstOrNull { it.key == key }?.icon

/**
 * آیکون نمایشی یک دسته: اگر کاربر آیکون شخصی انتخاب کرده همان، وگرنه
 * نگاشت قدیمی از روی نام دسته.
 */
internal fun categoryIconFor(category: CategoryEntity): ImageVector =
    categoryIconByKey(category.iconKey) ?: categoryIcon(category.name)

/**
 * پالت رنگ شاخص برگه ویرایش دسته، بر اساس دو ردیف دایره‌های رنگی
 * پرگار (فریم g_27: ردیف اول ۸ رنگ، ردیف دوم ۵ رنگ). مقدارها ARGB
 * کامل با آلفای FF هستند.
 */
internal val CATEGORY_COLOR_PALETTE = listOf(
    0xFFE53935L,
    0xFFF4511EL,
    0xFFFB8C00L,
    0xFFFBC02DL,
    0xFF43A047L,
    0xFF00897BL,
    0xFF00ACC1L,
    0xFF1E88E5L,
    0xFF3949ABL,
    0xFF8E24AAL,
    0xFFD81B60L,
    0xFF6D4C41L,
    0xFF78909CL
)

/**
 * رنگ نمایشی یک دسته: اگر کاربر رنگ شخصی انتخاب کرده همان، وگرنه
 * رنگ پیش‌فرض داده‌شده (معمولاً رنگ نوع دسته).
 */
internal fun categoryColorFor(category: CategoryEntity, fallback: Color): Color =
    if (category.colorArgb != 0L) Color(category.colorArgb) else fallback
