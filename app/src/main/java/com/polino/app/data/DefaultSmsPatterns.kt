package com.polino.app.data

/**
 * الگوهای پیش‌فرض پیامک بانکی (مرحله ۱۲۰): برای هر بانک یک الگوی
 * واریز (isIncome=true) و یک الگوی برداشت (isIncome=false) با همان
 * فرستنده؛ کلمات کلیدی دقیقاً همان فهرست‌های SmsParser هستند
 * (واریز/دریافت برای واریز، برداشت/کسر/خرید/پرداخت/انتقال برای
 * برداشت) تا جهت الگو با جهت پارس هم‌خوان باشد.
 *
 * بذر فقط الگوهایی را درج می‌کند که شناسه‌شان در جدول نیست
 * ([missingDefaultSmsPatterns])؛ الگوی دستی کاربر با همان فرستنده
 * هرگز بازنویسی یا حذف نمی‌شود و با پرچم SharedPreferences در
 * مخزن فقط یک‌بار اجرا می‌شود.
 *
 * منابع فرستنده‌ها — هیچ فرستنده یا کلیدواژه‌ای حدس زده نشده است:
 * - چیپ‌های «پیش‌فرض بانک‌های پرکاربرد» خود برنامه در شیت «افزودن
 *   شماره پیامک بانکی» (اسکرین‌شات‌های کاربر، ۲۰۲۶-۱۱-۰۶): سامان
 *   200000، پاسارگاد 10000000، ملی 300017، تجارت 200070، ملت
 *   200033، پارسیان 300078. فرستنده الفبایی B.Pasargad هم فرستنده
 *   متصل حساب خود کاربر است.
 * - فهرست یکپارچه ملی‌پیامک (melipayamak.com، «شماره پیامک های مهم
 *   و ضروری»، ۲۰۲۳-۰۳-۲۹): سپه 200020، سینا 200038، اقتصاد نوین
 *   200050، صادرات 200060، کشاورزی 2000911، رفاه 300066، سرمایه
 *   100088، مسکن 300014، توسعه تعاون 1000700. این فهرست در چهار
 *   بانکی که با چیپ‌های برنامه هم‌پوشانی دارد (سامان، ملت، تجارت،
 *   ملی) دقیقاً همان شماره‌ها را می‌دهد.
 * - اطلاعیه رسمی بانک توسعه صادرات ایران (purson.ir): سرشماره
 *   پیامک‌های واریز و برداشت به 30009430 تغییر یافته است.
 * - اطلاعیه رسمی بانک ایران زمین (ایلنا/پرسون): سرشماره 500045
 *   مختص واریز و برداشت است.
 *
 * نکته‌های منبع: برای پاسارگاد و پارسیان، فهرست ملی‌پیامک شماره‌های
 * دیگری هم می‌دهد (پاسارگاد 1000900 و 50009000؛ پارسیان 300055،
 * 50001099، 300054 و 30007) که خط‌های خدماتی دیگر بانک‌اند، نه
 * فرستنده پیامک تراکنش؛ برای این دو بانک شماره چیپ برنامه (فرستنده
 * تراکنش) استفاده شده است. برای سپه (200021، 200022)، رفاه
 * (300044) و توسعه تعاون (100070007) هم فقط شماره اول فهرست که
 * خط اصلی خدمات است بذر می‌شود. بانک‌های شهر، دی، گردشگری، آینده،
 * خاورمیانه، پست بانک، صنعت و معدن و کارآفرین در هیچ منبع قابل
 * اتکایی سرشماره تراکنش‌شان پیدا نشد و جا مانده‌اند.
 */
private val DEPOSIT_KEYWORDS = "واریز، دریافت"
private val WITHDRAWAL_KEYWORDS = "برداشت، کسر، خرید، پرداخت، انتقال"

/** یک بانک و سرشماره پیامک تراکنش‌هایش. */
private data class BankSmsSender(val slug: String, val sender: String)

private val BANK_SMS_SENDERS = listOf(
    BankSmsSender("saman", "200000"),
    BankSmsSender("pasargad", "10000000"),
    BankSmsSender("pasargad-alpha", "B.Pasargad"),
    BankSmsSender("melli", "300017"),
    BankSmsSender("tejarat", "200070"),
    BankSmsSender("mellat", "200033"),
    BankSmsSender("parsian", "300078"),
    BankSmsSender("sepah", "200020"),
    BankSmsSender("sina", "200038"),
    BankSmsSender("eghtesad-novin", "200050"),
    BankSmsSender("saderat", "200060"),
    BankSmsSender("keshavarzi", "2000911"),
    BankSmsSender("refah", "300066"),
    BankSmsSender("sarmaye", "100088"),
    BankSmsSender("maskan", "300014"),
    BankSmsSender("taavon", "1000700"),
    BankSmsSender("tosee-saderat", "30009430"),
    BankSmsSender("iran-zamin", "500045")
)

/** همه الگوهای پیش‌فرض: برای هر فرستنده یک واریز و یک برداشت. */
internal val DEFAULT_SMS_PATTERNS: List<SmsPatternEntity> =
    BANK_SMS_SENDERS.flatMap { bank ->
        listOf(
            SmsPatternEntity(
                id = "default-${bank.slug}-deposit",
                sender = bank.sender,
                keywords = DEPOSIT_KEYWORDS,
                isFinancial = true,
                isIncome = true
            ),
            SmsPatternEntity(
                id = "default-${bank.slug}-withdrawal",
                sender = bank.sender,
                keywords = WITHDRAWAL_KEYWORDS,
                isFinancial = true,
                isIncome = false
            )
        )
    }

/**
 * الگوهای پیش‌فرضی که هنوز در جدول نیستند — فقط بر اساس شناسه
 * مقایسه می‌شود، پس الگوی دستی کاربر با همان فرستنده هرگز در خروجی
 * نیست و بازنویسی نمی‌شود. تابع خالص است تا بدون پایگاه داده
 * تست شود.
 */
internal fun missingDefaultSmsPatterns(
    existing: List<SmsPatternEntity>
): List<SmsPatternEntity> {
    val existingIds = existing.map { it.id }.toSet()
    return DEFAULT_SMS_PATTERNS.filter { it.id !in existingIds }
}
