package com.polino.app.dashboard

import com.polino.app.data.ChequeReminderEntity

/** تعداد یادآورهای چک/قبض تسویه‌نشده — ورودی کارت «یادآورها» در هاب مدیریت. */
internal fun openChequeCount(cheques: List<ChequeReminderEntity>): Int =
    cheques.count { !it.settled }

/**
 * تعداد کل یادآورهای هاب مدیریت: اقساط ثبت‌شده + یادآورهای چک/قبض باز.
 * تسویه‌شده‌ها شمرده نمی‌شوند.
 */
internal fun hubReminderCount(installmentCount: Int, cheques: List<ChequeReminderEntity>): Int =
    installmentCount + openChequeCount(cheques)
