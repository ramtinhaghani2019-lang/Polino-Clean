package com.polino.app.testcenter

import com.polino.app.BuildConfig

internal data class SelfTestResult(
    val title: String,
    val description: String,
    val passed: Boolean,
    val details: String
)

internal object PolinoSelfTests {

    private data class TestCase(
        val title: String,
        val description: String,
        val block: () -> String
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
        TestCase("تبدیل تومان به ریال", "۱۰ تومان باید برابر ۱۰۰ ریال باشد") {
            val result = tomanToRial(10)
            check(result == 100L)
            "$result ریال"
        },
        TestCase("محاسبه مانده", "تست پایه درآمد منهای هزینه") {
            val balance = calculateBalance(
                incomesRial = listOf(1_000_000L, 200_000L),
                expensesRial = listOf(250_000L, 50_000L)
            )
            check(balance == 900_000L)
            "$balance ریال"
        },
        TestCase("عدد منفی هزینه", "موتور پایه نباید هزینه منفی بپذیرد") {
            val rejected = runCatching { validateMoneyAmount(-1) }.isFailure
            check(rejected)
            "مقدار منفی رد شد"
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

    internal fun tomanToRial(toman: Long): Long {
        validateMoneyAmount(toman)
        return Math.multiplyExact(toman, 10L)
    }

    internal fun calculateBalance(incomesRial: List<Long>, expensesRial: List<Long>): Long {
        incomesRial.forEach(::validateMoneyAmount)
        expensesRial.forEach(::validateMoneyAmount)
        return Math.subtractExact(incomesRial.sum(), expensesRial.sum())
    }

    internal fun validateMoneyAmount(amount: Long) {
        require(amount >= 0) { "مبلغ نمی‌تواند منفی باشد" }
    }
}
