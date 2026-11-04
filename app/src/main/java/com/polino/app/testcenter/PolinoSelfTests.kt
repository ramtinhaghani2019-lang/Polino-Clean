package com.polino.app.testcenter

import com.polino.app.BuildConfig
import com.polino.app.dashboard.JalaliDate
import com.polino.app.dashboard.RadarKind
import com.polino.app.dashboard.RadarInsight
import com.polino.app.dashboard.money
import com.polino.app.dashboard.monthTotals
import com.polino.app.dashboard.normalizeDigits
import com.polino.app.dashboard.spentByCategory
import com.polino.app.dashboard.spendingInsights
import com.polino.app.dashboard.transactionsInMonth
import com.polino.app.dashboard.transactionsToXlsx
import com.polino.app.data.AssetEntity
import com.polino.app.data.DebtEntity
import com.polino.app.data.MoneyTransaction
import com.polino.app.data.SmsPatternEntity
import com.polino.app.data.assetCurrentValue
import com.polino.app.data.dongiShares
import com.polino.app.data.installmentMonthlyAmount
import com.polino.app.data.installmentProgressFraction
import com.polino.app.data.isValidAssetInput
import com.polino.app.data.portfolioTotals
import com.polino.app.data.unsettledTotals
import com.polino.app.sms.findMatchingPattern
import com.polino.app.sms.parseBankSms
import com.polino.app.sms.resolveSmsIncome
import com.polino.app.sms.shouldRecordSms
import com.polino.app.voice.parseSpokenAmount
import kotlin.math.abs

internal data class SelfTestResult(
    val title: String,
    val description: String,
    val passed: Boolean,
    val details: String
)

/**
 * مرکز تست محاسبات پولینو: هر مورد موتور واقعی برنامه را با ورودی ثابت و
 * از پیش حساب‌شده صدا می‌زند؛ داده واقعی کاربر هرگز لمس نمی‌شود.
 * پیام خطا همیشه «مورد انتظار» در برابر «واقعی» را نشان می‌دهد.
 */
internal object PolinoSelfTests {

    private data class TestCase(
        val title: String,
        val description: String,
        val block: () -> String
    )

    private fun eq(expected: Any?, actual: Any?, label: String) {
        if (expected != actual) {
            throw IllegalStateException("«$label» مورد انتظار: $expected، واقعی: $actual")
        }
    }

    private fun eqApprox(expected: Double, actual: Double, label: String) {
        if (abs(expected - actual) > 1e-9) {
            throw IllegalStateException("«$label» مورد انتظار: $expected، واقعی: $actual")
        }
    }

    private fun eqFloatApprox(expected: Float, actual: Float, label: String) {
        if (abs(expected - actual) > 1e-6f) {
            throw IllegalStateException("«$label» مورد انتظار: $expected، واقعی: $actual")
        }
    }

    private fun tx(
        id: String,
        title: String,
        amount: Long,
        income: Boolean,
        category: String,
        millis: Long
    ): MoneyTransaction = MoneyTransaction(
        id = id,
        title = title,
        amount = amount,
        income = income,
        category = category,
        dateMillis = millis
    )

    private val cases: List<TestCase> = listOf(
        TestCase("شناسه برنامه", "بررسی package اصلی پولینو") {
            check(BuildConfig.APPLICATION_ID == "com.polino.app")
            BuildConfig.APPLICATION_ID
        },
        TestCase("نوع بیلد", "مرکز تست باید فقط در نسخه Debug دیده شود") {
            check(BuildConfig.DEBUG)
            "DEBUG = ${BuildConfig.DEBUG}"
        },
        TestCase("دونگی: تقسیم مساوی", "تقسیم مساوی و باقی‌مانده یکی‌یکی به نفرات اول") {
            eq(listOf(30000L, 30000L, 30000L), dongiShares(90000, 3), "تقسیم ۹۰۰۰۰ بین ۳ نفر")
            val shares = dongiShares(100000, 3)
            eq(listOf(33334L, 33333L, 33333L), shares, "تقسیم ۱۰۰۰۰۰ بین ۳ نفر")
            eq(100000L, shares.sum(), "جمع سهم‌های دونگی")
            eq(listOf(1L, 0L), dongiShares(1, 2), "تقسیم ۱ تومان بین ۲ نفر")
            "33334 + 33333 + 33333 = 100000"
        },
        TestCase("دونگی: ورودی نامعتبر", "مبلغ منفی یا صفر نفر باید فهرست خالی بدهد") {
            eq(emptyList<Long>(), dongiShares(-1000, 3), "دونگی مبلغ منفی")
            eq(emptyList<Long>(), dongiShares(1000, 0), "دونگی صفر نفر")
            "هر دو ورودی نامعتبر فهرست خالی دادند"
        },
        TestCase("اقساط: مبلغ هر قسط", "تقسیم صحیح؛ باقی‌مانده در قسط آخر می‌نشیند") {
            eq(100000L, installmentMonthlyAmount(1_000_000, 10), "قسط ۱٬۰۰۰٬۰۰۰ در ۱۰ ماه")
            eq(100000L, installmentMonthlyAmount(1_000_001, 10), "قسط ۱٬۰۰۰٬۰۰۱ در ۱۰ ماه")
            eq(0L, installmentMonthlyAmount(500_000, 0), "قسط با صفر ماه")
            "100000 تومان در هر ماه"
        },
        TestCase("اقساط: پیشرفت پرداخت", "پیشرفت بین ۰ و ۱ و سقف‌دار در تعداد پرداخت‌شده") {
            eqFloatApprox(0.3f, installmentProgressFraction(3, 10), "پیشرفت ۳ از ۱۰ قسط")
            eq(1f, installmentProgressFraction(12, 10), "پیشرفت بیش از کل اقساط")
            eq(0f, installmentProgressFraction(0, 10), "پیشرفت صفر قسط")
            eq(0f, installmentProgressFraction(5, 0), "پیشرفت با صفر ماه")
            "3 از 10 قسط = 0.3"
        },
        TestCase("پرتفوی: ارزش و سود", "ارزش روز با قیمت روز، وگرنه قیمت خرید") {
            val gold = AssetEntity("a1", "طلای ۱۸ عیار", "طلا", "گرم", 2.0, 100.0, 150.0, "main")
            val cash = AssetEntity("a2", "دلار", "ارز", "دلار", 5.0, 50.0, null, "main")
            eqApprox(300.0, assetCurrentValue(gold), "ارزش روز طلا")
            eqApprox(250.0, assetCurrentValue(cash), "ارزش روز ارز بدون قیمت روز")
            val totals = portfolioTotals(listOf(gold, cash))
            eqApprox(550.0, totals.totalValue, "ارزش کل پرتفوی")
            eqApprox(450.0, totals.totalCost, "بهای تمام‌شده پرتفوی")
            eqApprox(100.0, totals.profitLoss, "سود پرتفوی")
            eqApprox(100.0 / 450.0 * 100, totals.profitLossPercent, "درصد سود پرتفوی")
            "ارزش 550، بهای تمام‌شده 450، سود 100"
        },
        TestCase("پرتفوی: زیان و هزینه صفر", "درصد سود وقتی بهای تمام‌شده صفر است باید صفر باشد") {
            val losing = AssetEntity("a3", "سهم", "سهام", "سهم", 1.0, 100.0, 80.0, "main")
            val lost = portfolioTotals(listOf(losing))
            eqApprox(-20.0, lost.profitLoss, "زیان سهم")
            eqApprox(-20.0, lost.profitLossPercent, "درصد زیان سهم")
            val free = AssetEntity("a4", "هدیه", "سایر", "عدد", 3.0, 0.0, 10.0, "main")
            val freeTotals = portfolioTotals(listOf(free))
            eqApprox(30.0, freeTotals.totalValue, "ارزش دارایی رایگان")
            eqApprox(0.0, freeTotals.totalCost, "بهای دارایی رایگان")
            eqApprox(0.0, freeTotals.profitLossPercent, "درصد سود با بهای صفر")
            val empty = portfolioTotals(emptyList())
            eqApprox(0.0, empty.totalValue, "ارزش پرتفوی خالی")
            eqApprox(0.0, empty.profitLossPercent, "درصد سود پرتفوی خالی")
            "زیان 20-، سود دارایی رایگان 30 با درصد 0"
        },
        TestCase("دارایی: اعتبارسنجی ورودی", "مقدار باید مثبت و قیمت خرید نامنفی باشد") {
            eq(true, isValidAssetInput(2.0, 100.0), "ورودی معتبر دارایی")
            eq(false, isValidAssetInput(0.0, 100.0), "مقدار صفر دارایی")
            eq(false, isValidAssetInput(2.0, -1.0), "قیمت خرید منفی")
            "فقط ورودی معتبر پذیرفته شد"
        },
        TestCase("طلب و بدهی: جمع تسویه‌نشده", "تسویه‌شده‌ها در جمع باز نمی‌آیند") {
            val debts = listOf(
                DebtEntity("d1", "علی", 300_000, true),
                DebtEntity("d2", "رضا", 200_000, true),
                DebtEntity("d3", "سارا", 150_000, false),
                DebtEntity("d4", "مریم", 999_000, true, settled = true),
                DebtEntity("d5", "حسن", 888_000, false, settled = true)
            )
            eq(Pair(500_000L, 150_000L), unsettledTotals(debts), "جمع طلب و بدهی باز")
            "طلب باز 500000، بدهی باز 150000"
        },
        TestCase("ماهانه: فیلتر ماه و جمع", "فقط تراکنش‌های همان ماه شمسی، بدون تاریخ کنار می‌رود") {
            val aban = JalaliDate.jalaliToMillis(1405, 8, 13)
            val aban20 = JalaliDate.jalaliToMillis(1405, 8, 20)
            val aban25 = JalaliDate.jalaliToMillis(1405, 8, 25)
            val mehr = JalaliDate.jalaliToMillis(1405, 7, 20)
            val all = listOf(
                tx("t1", "حقوق", 500_000, true, "حقوق", aban),
                tx("t2", "خرید", 200_000, false, "خوراک", aban20),
                tx("t3", "خرید", 50_000, false, "خوراک", aban25),
                tx("t4", "خرید مهر", 999_999, false, "خوراک", mehr),
                tx("t5", "بدون تاریخ", 777_777, true, "سایر", 0L)
            )
            val month = transactionsInMonth(all, "1405-08")
            eq(3, month.size, "تعداد تراکنش‌های آبان ۱۴۰۵")
            val totals = monthTotals(month)
            eq(500_000L, totals.income, "درآمد آبان ۱۴۰۵")
            eq(250_000L, totals.expense, "هزینه آبان ۱۴۰۵")
            "درآمد 500000، هزینه 250000"
        },
        TestCase("ماهانه: هزینه هر دسته", "هزینه دسته‌ها از بیشترین به کمترین مرتب می‌شود") {
            val day = JalaliDate.jalaliToMillis(1405, 8, 13)
            val month = listOf(
                tx("c1", "خرید", 200_000, false, "خوراک", day),
                tx("c2", "خرید", 50_000, false, "خوراک", day),
                tx("c3", "تاکسی", 100_000, false, "حمل", day),
                tx("c4", "حقوق", 500_000, true, "حقوق", day)
            )
            eq(
                listOf("خوراک" to 250_000L, "حمل" to 100_000L),
                spentByCategory(month),
                "هزینه دسته‌های ماه"
            )
            "خوراک 250000، حمل 100000"
        },
        TestCase("رادار: دسته جدید و افزایش", "دسته تازه اول، بعد بیشترین افزایش") {
            val previous = listOf(
                tx("r1", "خرید", 100_000, false, "خوراک", 1L),
                tx("r2", "تاکسی", 50_000, false, "حمل", 1L)
            )
            val current = listOf(
                tx("r3", "خرید", 120_000, false, "خوراک", 1L),
                tx("r4", "دارو", 30_000, false, "درمان", 1L)
            )
            val insights = spendingInsights(current, previous)
            eq(2, insights.size, "تعداد بینش‌های رادار")
            eq(RadarKind.NEW, insights[0].kind, "نوع بینش اول رادار")
            eq(RadarKind.UP, insights[1].kind, "نوع بینش دوم رادار")
            "درمان تازه، خوراک بیشتر شده"
        },
        TestCase("رادار: کاهش مجموع و ماه خالی", "کاهش مجموع یک خط کلی می‌سازد؛ ماه خالی بینشی ندارد") {
            val previous = listOf(tx("r5", "خرید", 200_000, false, "خوراک", 1L))
            val current = listOf(tx("r6", "خرید", 100_000, false, "خوراک", 1L))
            val insights = spendingInsights(current, previous)
            eq(1, insights.size, "تعداد بینش کاهش مجموع")
            eq(RadarKind.DOWN, insights[0].kind, "نوع بینش کاهش مجموع")
            eq(emptyList<RadarInsight>(), spendingInsights(emptyList<MoneyTransaction>(), previous), "بینش ماه خالی")
            "فقط خط کاهش مجموع"
        },
        TestCase("شمسی: تبدیل میلادی و نمایش", "مرزهای تقویم و قالب نمایش تاریخ") {
            eq(Triple(1405, 1, 1), JalaliDate.gregorianToJalali(2026, 3, 21), "نوروز ۱۴۰۵")
            eq(Triple(1405, 8, 13), JalaliDate.gregorianToJalali(2026, 11, 4), "۱۳ آبان ۱۴۰۵")
            val millis = JalaliDate.jalaliToMillis(1405, 8, 13)
            eq("13 آبان 1405", JalaliDate.formatJalali(millis), "نمایش ۱۳ آبان ۱۴۰۵")
            eq("1405-08", JalaliDate.jalaliYearMonthOf(millis), "کلید ماه آبان ۱۴۰۵")
            eq("آبان 1405", JalaliDate.jalaliMonthDisplay("1405-08"), "نام ماه آبان ۱۴۰۵")
            eq("1404-12", JalaliDate.previousJalaliYearMonth("1405-01"), "ماه قبل از فروردین ۱۴۰۵")
            "13 آبان 1405"
        },
        TestCase("شمسی: کبیسه و طول ماه", "اسفند سال کبیسه ۳۰ روز وگرنه ۲۹ روز است") {
            eq(true, JalaliDate.isLeapJalaliYear(1403), "کبیسه بودن ۱۴۰۳")
            eq(false, JalaliDate.isLeapJalaliYear(1404), "کبیسه بودن ۱۴۰۴")
            eq(30, JalaliDate.daysInJalaliMonth(1403, 12), "طول اسفند ۱۴۰۳")
            eq(29, JalaliDate.daysInJalaliMonth(1404, 12), "طول اسفند ۱۴۰۴")
            eq("بدون تاریخ", JalaliDate.formatJalali(0L), "نمایش تاریخ صفر")
            "۱۴۰۳ کبیسه، ۱۴۰۴ غیرکبیسه"
        },
        TestCase("پیامک: برداشت", "مبلغ برداشت با ارقام فارسی خوانده می‌شود") {
            val parsed = parseBankSms("BANK", "برداشت ۵۰۰٬۰۰۰ تومان از حساب شما")
            eq(500_000L, parsed?.amount, "مبلغ برداشت پیامکی")
            eq(false, parsed?.income, "جهت برداشت پیامکی")
            "برداشت 500000 تومان"
        },
        TestCase("پیامک: واریز و ریال", "واریز درآمد است و ریال به تومان تبدیل می‌شود") {
            val income = parseBankSms("BANK", "واریز ۲٬۰۰۰٬۰۰۰ تومان به حساب شما انجام شد")
            eq(2_000_000L, income?.amount, "مبلغ واریز پیامکی")
            eq(true, income?.income, "جهت واریز پیامکی")
            val rial = parseBankSms("BANK", "برداشت ۵٬۰۰۰٬۰۰۰ ریال از حساب")
            eq(500_000L, rial?.amount, "تبدیل ریال پیامکی به تومان")
            eq(false, rial?.income, "جهت برداشت ریالی")
            "واریز 2000000، برداشت ریالی 500000"
        },
        TestCase("پیامک: موجودی و پیام مبهم", "عدد بعد از مانده موجودی است و پیام بی‌جهت ثبت نمی‌شود") {
            val withBalance = parseBankSms(
                "BANK",
                "مانده حساب شما ۱۲٬۳۴۵٬۶۷۸ تومان است. برداشت مبلغ ۲۵۰٬۰۰۰ تومان با موفقیت انجام شد"
            )
            eq(250_000L, withBalance?.amount, "مبلغ تراکنش بعد از مانده")
            eq(false, withBalance?.income, "جهت تراکنش بعد از مانده")
            eq(null, parseBankSms("BANK", "مانده: ۱۰٬۰۰۰٬۰۰۰ تومان"), "پیام فقط موجودی")
            eq(null, parseBankSms("BANK", "۵۰۰٬۰۰۰ تومان به حساب نشست"), "پیام بدون کلمه جهت")
            "موجودی نادیده گرفته شد"
        },
        TestCase("صدا: عددهای رقمی", "هزار و میلیون و ریال در گفتار به تومان تبدیل می‌شوند") {
            eq(500_000L, parseSpokenAmount("۵۰۰ هزار تومان"), "گفتار ۵۰۰ هزار تومان")
            eq(2_000_000L, parseSpokenAmount("۲ میلیون تومان"), "گفتار ۲ میلیون تومان")
            eq(100_000L, parseSpokenAmount("۱۰۰٬۰۰۰ تومان"), "گفتار ۱۰۰ هزار تومان")
            eq(500_000L, parseSpokenAmount("۵٬۰۰۰٬۰۰۰ ریال"), "گفتار ریالی")
            "۵۰۰ هزار = 500000"
        },
        TestCase("صدا: عدد کلمه‌ای", "نسخه اول عدد کاملاً کلمه‌ای را نمی‌فهمد و null می‌دهد") {
            eq(null, parseSpokenAmount("پانصد هزار تومان"), "گفتار عدد کلمه‌ای")
            "محدودیت شناخته‌شده نسخه اول"
        },
        TestCase("قالب پول و ارقام", "مبلغ با تومان می‌آید و ارقام فارسی و عربی یکسان می‌شوند") {
            val formatted = money(1_000_000)
            check(formatted.endsWith("تومان")) { "پسوند تومان: $formatted" }
            val digits = normalizeDigits(formatted).filter { it.isDigit() }
            eq("1000000", digits, "ارقام مبلغ یک میلیونی")
            eq("123", normalizeDigits("۱۲۳"), "ارقام فارسی")
            eq("123", normalizeDigits("١٢٣"), "ارقام عربی")
            eq("12a3", normalizeDigits("12a3"), "ارقام لاتین بدون تغییر")
            formatted
        },
        TestCase("الگوی پیامک: تطابق", "فرستنده زیررشته‌ای و کلمه کلیدی باید هر دو بنشینند") {
            val bank = SmsPatternEntity("p1", "BANK", "برداشت، خرید", true, false)
            val shop = SmsPatternEntity("p2", "SHOP", "", false, false)
            eq(bank, findMatchingPattern(listOf(bank, shop), "BANK-MELLAT", "برداشت ۵۰۰٬۰۰۰ تومان"), "تطابق فرستنده و کلمه")
            eq(null, findMatchingPattern(listOf(bank), "OTHER", "برداشت ۵۰۰٬۰۰۰ تومان"), "عدم تطابق فرستنده")
            eq(null, findMatchingPattern(listOf(bank), "BANK-MELLAT", "واریز حقوق"), "عدم تطابق کلمه کلیدی")
            eq(shop, findMatchingPattern(listOf(bank, shop), "SHOP-ONLINE", "هر متنی"), "الگوی بدون کلمه کلیدی")
            "فرستنده + کلمه کلیدی"
        },
        TestCase("الگوی پیامک: سد مالی و جهت", "الگوی غیرمالی جلوی ثبت را می‌گیرد و جهت الگو اولویت دارد") {
            val bank = SmsPatternEntity("p1", "BANK", "برداشت", true, false)
            val shop = SmsPatternEntity("p2", "SHOP", "", false, false)
            eq(true, shouldRecordSms(null), "ثبت بدون الگو")
            eq(false, shouldRecordSms(shop), "سد الگوی غیرمالی")
            eq(true, shouldRecordSms(bank), "ثبت با الگوی مالی")
            eq(false, resolveSmsIncome(bank, true), "جهت الگوی هزینه بر پارس درآمدی")
            eq(true, resolveSmsIncome(null, true), "جهت پارس بدون الگو")
            "غیرمالی ثبت نمی‌شود"
        },
        TestCase("اکسل: سرآیند فایل", "خروجی اکسل باید یک فایل زیپ معتبر و ناته باشد") {
            val bytes = transactionsToXlsx(
                listOf(
                    tx(
                        "x1",
                        "خرید تست",
                        100_000,
                        false,
                        "خوراک",
                        JalaliDate.jalaliToMillis(1405, 8, 13)
                    )
                )
            )
            check(bytes.isNotEmpty()) { "خروجی اکسل خالی است" }
            eq(0x50.toByte(), bytes[0], "بایت اول زیپ")
            eq(0x4B.toByte(), bytes[1], "بایت دوم زیپ")
            "${bytes.size} بایت با سرآیند PK"
        }
    )

    fun runAll(): List<SelfTestResult> = cases.indices.map(::runOne)

    fun runOne(index: Int): SelfTestResult {
        val test = cases[index]
        return try {
            SelfTestResult(test.title, test.description, true, test.block())
        } catch (t: Throwable) {
            SelfTestResult(
                title = test.title,
                description = test.description,
                passed = false,
                details = t.message ?: t::class.java.simpleName
            )
        }
    }
}
