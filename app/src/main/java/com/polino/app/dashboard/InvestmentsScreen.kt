package com.polino.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.polino.app.data.AccountEntity
import com.polino.app.data.AssetEntity
import com.polino.app.data.TransactionRepository
import com.polino.app.data.assetCurrentValue
import com.polino.app.data.portfolioTotals
import java.util.UUID
import kotlinx.coroutines.launch

/** رنگ‌های صفحه پرتفوی، به زبان ورق‌های تیره مرحله‌های ۶۹ تا ۸۸. */
private val InvestmentDark = Color(0xFF1E1438)
private val InvestmentCard = Color(0xFF2A2150)
private val InvestmentBorder = Color(0xFF3D3368)
private val InvestmentMuted = Color(0xFFB3ACD6)
private val InvestmentTeal = Color(0xFF2DD4BF)
private val InvestmentTealInk = Color(0xFF0B1F1A)
private val InvestmentGreen = Color(0xFF34C89E)
private val InvestmentRed = Color(0xFFF87171)

/**
 * زیرنوع نمایشی دارایی، از روی واحد آن: برای «سهام و سایر» واحد سهم یعنی
 * سهام، واحد صندوق یعنی صندوق سرمایه‌گذاری و واحد یعنی اوراق مشارکت
 * (همان سه پیشنهاد سریعی که فرم افزودن نشان می‌دهد)؛ برای «سایر» برچسب
 * «سایر دارایی‌ها»؛ برای بقیه نوع‌ها زیرنوعی نشان داده نمی‌شود.
 */
internal fun assetSubtypeLabel(asset: AssetEntity): String = when (asset.type) {
    "سهام و سایر" -> when (asset.unit) {
        "سهم" -> "سهام"
        "واحد صندوق" -> "صندوق سرمایه‌گذاری"
        else -> "اوراق مشارکت"
    }
    "سایر" -> "سایر دارایی‌ها"
    else -> ""
}

/** ترتیب نمایش گروه‌های دارایی در صفحه پرتفوی. */
internal fun investmentGroupOrder(type: String): Int = when (type) {
    "طلا" -> 0
    "ارز" -> 1
    "ارز دیجیتال" -> 2
    "سهام و سایر" -> 3
    else -> 4
}

/** واحدهای رمزارز موجود در پرتفوی، به ترتیب اولین دیده‌شدن، بدون تکرار. */
internal fun cryptoCoinUnits(assets: List<AssetEntity>): List<String> =
    assets.filter { it.type == "ارز دیجیتال" }.map { it.unit }.distinct()

private fun investmentTypeIcon(type: String): ImageVector = when (type) {
    "طلا" -> Icons.Default.Star
    "ارز" -> Icons.Default.AttachMoney
    "ارز دیجیتال" -> Icons.Default.TrendingUp
    "سهام و سایر" -> Icons.Default.ShowChart
    else -> Icons.Default.AccountBalanceWallet
}

private fun investmentTypeColor(type: String): Color = when (type) {
    "طلا" -> Color(0xFFE3B04B)
    "ارز" -> Color(0xFF64C0AB)
    "ارز دیجیتال" -> Color(0xFFF59E0B)
    "سهام و سایر" -> Color(0xFFA855F7)
    else -> InvestmentMuted
}

/**
 * صفحه اختصاصی سرمایه‌گذاری و دارایی‌ها: کارت سرخط پرتفوی (ارزش کل،
 * تعداد دارایی‌های فعال، ارزش روز، سود/زیان با درصد)، دکمه سبزآبی
 * «+ افزودن دارایی جدید»، گروه‌بندی دارایی‌ها بر حسب نوع با زیرنوع
 * سهام، نمایش دوردیفی ارزها و چیپ سکه برای رمزارزها.
 */
@Composable
internal fun InvestmentsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    var assets by remember { mutableStateOf<List<AssetEntity>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<AssetEntity?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        assets = repository.getAssets()
        accounts = repository.getAccounts()
    }

    val totals = portfolioTotals(assets)
    val profitLoss = totals.profitLoss
    val percent = Math.round(totals.profitLossPercent * 10) / 10.0
    val grouped = assets.groupBy { it.type }
        .toList()
        .sortedBy { investmentGroupOrder(it.first) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "سرمایه‌گذاری و دارایی‌ها",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${assets.size} دارایی فعال",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = InvestmentDark),
                border = BorderStroke(1.dp, InvestmentBorder)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "ارزش کل پرتفوی",
                        style = MaterialTheme.typography.labelLarge,
                        color = InvestmentMuted
                    )
                    Text(
                        money(totals.totalValue.toLong()),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        "دارایی‌های فعال: ${assets.size} · ارزش روز: ${money(totals.totalValue.toLong())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InvestmentMuted
                    )
                    Text(
                        "سود/زیان: ${money(profitLoss.toLong())} ($percent٪)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (profitLoss >= 0) InvestmentGreen else InvestmentRed
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    editingAsset = null
                    showDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InvestmentTeal,
                    contentColor = InvestmentTealInk
                )
            ) {
                Text(
                    "+ افزودن دارایی جدید",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        if (assets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = InvestmentCard),
                    border = BorderStroke(1.dp, InvestmentBorder)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = InvestmentTeal,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "هنوز دارایی ثبت نشده است.",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "طلا، ارز، سهام یا ملک؛ دارایی‌هایت را اینجا ثبت کن تا ارزش روز و سود و زیانشان را یک‌جا ببینی.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InvestmentMuted
                        )
                        TextButton(onClick = {
                            editingAsset = null
                            showDialog = true
                        }) {
                            Text("شروع کنید", color = InvestmentTeal)
                        }
                    }
                }
            }
        } else {
            grouped.forEach { (type, typeAssets) ->
                item {
                    Text(
                        "$type (${typeAssets.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                typeAssets.forEach { asset ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = InvestmentCard),
                            border = BorderStroke(1.dp, InvestmentBorder)
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = investmentTypeColor(type)
                                    ) {
                                        Icon(
                                            investmentTypeIcon(type),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            asset.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        if (type == "ارز") {
                                            Text(
                                                "واحد: ${asset.unit} · مقدار: ${formatAssetQuantity(asset.quantity)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = InvestmentMuted
                                            )
                                        } else {
                                            Text(
                                                "${formatAssetQuantity(asset.quantity)} ${asset.unit}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = InvestmentMuted
                                            )
                                        }
                                    }
                                    val subtype = assetSubtypeLabel(asset)
                                    if (subtype.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = InvestmentDark,
                                            border = BorderStroke(1.dp, InvestmentBorder)
                                        ) {
                                            Text(
                                                subtype,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = InvestmentMuted,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                    if (type == "ارز دیجیتال") {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = InvestmentTeal
                                        ) {
                                            Text(
                                                asset.unit,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = InvestmentTealInk,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }
                                Text("ارزش روز: ${money(assetCurrentValue(asset).toLong())}")
                                val assetProfitLoss =
                                    assetCurrentValue(asset) - asset.quantity * asset.buyPrice
                                Text(
                                    "سود/زیان: ${money(assetProfitLoss.toLong())}",
                                    color = if (assetProfitLoss >= 0) InvestmentGreen else InvestmentRed
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        editingAsset = asset
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "ویرایش")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            repository.deleteAsset(asset.id)
                                            assets = repository.getAssets()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AssetDialog(
            accounts = accounts,
            onDismiss = {
                showDialog = false
                editingAsset = null
            },
            onSave = { name, type, unit, quantity, buyPrice, currentPrice, accountId, note ->
                scope.launch {
                    repository.addAsset(
                        AssetEntity(
                            id = editingAsset?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            type = type,
                            unit = unit,
                            quantity = quantity,
                            buyPrice = buyPrice,
                            currentPrice = currentPrice,
                            accountId = accountId,
                            note = note
                        )
                    )
                    assets = repository.getAssets()
                    showDialog = false
                    editingAsset = null
                }
            },
            initial = editingAsset
        )
    }
}
