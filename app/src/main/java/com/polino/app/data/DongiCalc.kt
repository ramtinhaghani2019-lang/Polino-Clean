package com.polino.app.data

/**
 * تقسیم مساوی مبلغ کل بین نفرات؛ باقیمانده (کمتر از تعداد نفرات) یکی‌یکی
 * به نفرات اول می‌رسد تا جمع سهم‌ها دقیقاً برابر مبلغ کل بماند.
 * ورودی نامعتبر (مبلغ منفی یا کمتر از یک نفر) فهرست خالی می‌دهد.
 */
fun dongiShares(total: Long, people: Int): List<Long> {
    if (total < 0 || people < 1) return emptyList()
    val base = total / people
    val remainder = (total % people).toInt()
    return List(people) { index -> base + if (index < remainder) 1 else 0 }
}

/**
 * عضو گروه دنگی: نام، مبلغی که تاکنون پرداخته، و آیا تسویه‌اش ثبت شده است.
 * پرداختی منفی در محاسبه صفر فرض می‌شود.
 */
data class DongiMember(
    val name: String,
    val paid: Long,
    val settled: Boolean = false
)

/** یک پرداخت تسویه: from مبلغ amount را به to می‌پردازد. */
data class DongiSettlement(
    val from: String,
    val to: String,
    val amount: Long
)

/**
 * تسویه‌حساب دنگی با الگوریتم حریصانه: مانده هر عضو = پرداختی − سهم مساوی
 * (سهم‌ها همان dongiShares با باقیمانده یکی‌یکی به نفرات اول است).
 * بدهکاران به ترتیب اعضا به بستانکاران می‌پردازند تا یک طرف تمام شود.
 * اعضای «تسویه شده» از محاسبه کنار گذاشته می‌شوند؛ وقتی همه متوازن‌اند
 * (یا عضوی نیست) فهرست خالی برمی‌گردد.
 */
fun dongiSettlements(members: List<DongiMember>): List<DongiSettlement> {
    if (members.isEmpty()) return emptyList()
    val total = members.sumOf { it.paid.coerceAtLeast(0L) }
    val shares = dongiShares(total, members.size)
    if (shares.isEmpty()) return emptyList()
    val debtors = ArrayDeque<Pair<String, Long>>()
    val creditors = ArrayDeque<Pair<String, Long>>()
    members.forEachIndexed { index, member ->
        if (member.settled) return@forEachIndexed
        val balance = member.paid.coerceAtLeast(0L) - shares[index]
        when {
            balance < 0 -> debtors.add(member.name to -balance)
            balance > 0 -> creditors.add(member.name to balance)
        }
    }
    val result = mutableListOf<DongiSettlement>()
    while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
        val (debtor, debt) = debtors.removeFirst()
        val (creditor, credit) = creditors.removeFirst()
        val amount = minOf(debt, credit)
        result.add(DongiSettlement(debtor, creditor, amount))
        if (debt > amount) debtors.addFirst(debtor to debt - amount)
        if (credit > amount) creditors.addFirst(creditor to credit - amount)
    }
    return result
}
