# Polino — Clean Base v1

نسخه پایه تمیز پولینو برای شروع توسعه مرحله‌ای.

## هدف این نسخه

- پروژه Android استاندارد و مینیمال
- Jetpack Compose + Material 3
- رابط فارسی و RTL
- Test Center داخلی در نسخه Debug
- Unit Test برای منطق پایه مالی
- GitHub Actions برای تست و ساخت APK
- Gradle wrapper داخل ZIP تا پروژه ناقص آپلود نشود
- قفل‌کردن نسخه‌های Build Tool برای جلوگیری از تغییرات ناگهانی

## نسخه‌های پایه

- Android Gradle Plugin: 9.4.0
- Gradle: 9.6.0
- Kotlin / Compose Compiler Plugin: 2.4.21
- Compose BOM: 2026.09.00
- compileSdk: 37
- targetSdk: 36
- minSdk: 26
- JDK: 17

## مرکز تست

در Debug از صفحه اصلی وارد «مرکز تست» شوید. در Release این ورودی نمایش داده نمی‌شود.

تست‌های فعلی:

1. شناسه برنامه
2. Debug بودن نسخه تست
3. تبدیل تومان به ریال
4. محاسبه مانده پایه
5. رد مبلغ منفی

در مراحل بعد تست‌های دیتابیس، تراکنش، بودجه، پیامک بانکی، اعلان و Backup/Restore به همین مرکز اضافه می‌شوند.

## GitHub

Workflow موجود در `.github/workflows/android-build.yml` روی Push/Pull Request:

1. JDK 17 را آماده می‌کند.
2. Android SDK 37 را نصب می‌کند.
3. Gradle 9.6.0 را آماده می‌کند.
4. Unit Testها را اجرا می‌کند.
5. Debug APK را Build می‌کند.
6. APK را به عنوان Artifact ذخیره می‌کند.

CI عمداً Gradle را صریحاً نصب می‌کند؛ بنابراین حتی اگر Wrapper در آینده آسیب ببیند، خطای CI قابل تشخیص و مستقل خواهد بود.
