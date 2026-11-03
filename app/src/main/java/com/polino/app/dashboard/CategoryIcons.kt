package com.polino.app.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Store
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
    else -> Icons.Filled.Category
}
