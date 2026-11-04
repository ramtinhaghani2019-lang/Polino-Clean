package com.polino.app.dashboard

import java.text.NumberFormat
import java.util.Locale

/**
 * Pure display helpers for the dongi summary card and person rows.
 * No Android types, no repository access — safe for JVM unit tests.
 */

private fun persianNumber(value: Int): String =
    NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(value)

/** Person-count chip text, e.g. «۲ نفر». */
internal fun dongiPersonCountText(count: Int): String = "${persianNumber(count)} نفر"

/** Settlement-count line, e.g. «۰ پرداخت» / «۲ پرداخت». */
internal fun dongiSettlementCountText(count: Int): String = "${persianNumber(count)} پرداخت"

/**
 * Share-range line for the summary card: a single value when every share is
 * equal, «min تا max» when the remainder splits the shares, empty when there
 * are no shares (no members).
 */
internal fun dongiShareSummary(shares: List<Long>): String {
    if (shares.isEmpty()) return ""
    val minShare = shares.min()
    val maxShare = shares.max()
    return if (minShare == maxShare) money(minShare) else "${money(minShare)} تا ${money(maxShare)}"
}

/**
 * Directional balance line for a person row. Settled members show no
 * directional text — their «تسویه شده» chip carries the state.
 */
internal fun dongiBalanceLabel(settled: Boolean, balance: Long): String = when {
    settled -> ""
    balance > 0 -> "باید بگیرد: ${money(balance)}"
    balance < 0 -> "باید بدهد: ${money(-balance)}"
    else -> "متوازن"
}
