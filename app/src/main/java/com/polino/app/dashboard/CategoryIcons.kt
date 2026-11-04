package com.polino.app.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** آیکون هر دسته؛ دسته ناشناخته آیکون عمومی می‌گیرد. */
internal fun categoryIcon(category: String): ImageVector = when (category) {
    "خوراک" -> Icons.Filled.Restaurant
    "مسکن" -> Icons.Filled.Home
    "حمل‌ونقل" -> Icons.Filled.DirectionsCar
    "سلامت" -> Icons.Filled.Favorite
    "آموزش" -> Icons.Filled.School
    "تفریح" -> Icons.Filled.Movie
    "پوشاک" -> Icons.Filled.Checkroom
    "حقوق" -> Icons.Filled.Work
    "فروش" -> Icons.Filled.Store
    "هدیه" -> Icons.Filled.CardGiftcard
    "درمان و سلامت" -> Icons.Filled.Favorite
    "تفریح و ورزش" -> Icons.Filled.Movie
    "خریدهای روزانه" -> Icons.Filled.Store
    "تعهدات و اقساط" -> Icons.Filled.AccountBalance
    "خوراک و پوشاک" -> Icons.Filled.Restaurant
    "حمل و نقل" -> Icons.Filled.DirectionsCar
    "قبوض و ارتباطات" -> Icons.Filled.Receipt
    "خیریه" -> Icons.Filled.VolunteerActivism
    "اقساط" -> Icons.Filled.Payments
    "حقوق و دستمزد" -> Icons.Filled.Work
    "پاداش و کارانه" -> Icons.Filled.CardGiftcard
    "فروش و درآمد جانبی" -> Icons.Filled.Store
    else -> Icons.Filled.Category
}
