package com.polino.app.backup

import android.content.Context
import java.io.File
import java.util.Locale

/** یک فایل پشتیبان ساخته‌شده توسط خود پولینو در پوشه پشتیبان‌های برنامه. */
data class BackupFileInfo(
    val name: String,
    val sizeBytes: Long,
    val createdMillis: Long
)

private val BACKUP_NAME_REGEX = Regex("^polino-backup-(\\d+)\\.json$")

/**
 * نام فایل پشتیبان برای زمان ساخت؛ زمان به‌صورت میلی‌ثانیه در نام می‌نشیند
 * تا مرتب‌سازی و نمایش تاریخ از خود نام ممکن شود.
 */
fun backupFileName(createdMillis: Long): String = "polino-backup-$createdMillis.json"

/** زمان ساخت را از نام فایل می‌خواند؛ نام‌های غیر پشتیبان null برمی‌گردانند. */
fun backupCreatedMillisFromName(name: String): Long? {
    val match = BACKUP_NAME_REGEX.matchEntire(name) ?: return null
    return match.groupValues[1].toLongOrNull()
}

/** اطلاعات فایل را فقط وقتی نامش الگوی پشتیبان است می‌سازد. */
fun toBackupFileInfo(name: String, sizeBytes: Long): BackupFileInfo? {
    val created = backupCreatedMillisFromName(name) ?: return null
    return BackupFileInfo(name, sizeBytes, created)
}

/** جدیدترین اول، بعد نام برای ترتیب قطعی. */
fun sortedBackupFiles(files: List<BackupFileInfo>): List<BackupFileInfo> =
    files.sortedWith(compareByDescending<BackupFileInfo> { it.createdMillis }.thenBy { it.name })

/** اندازه خوانا: بایت / کیلوبایت / مگابایت با یک رقم اعشار. */
fun backupDisplaySize(sizeBytes: Long): String {
    return when {
        sizeBytes < 1024 -> "$sizeBytes بایت"
        sizeBytes < 1024 * 1024 -> "%.1f کیلوبایت".format(Locale.US, sizeBytes / 1024.0)
        else -> "%.1f مگابایت".format(Locale.US, sizeBytes / (1024.0 * 1024.0))
    }
}

/** پوشه پشتیبان‌های خود برنامه (حافظه داخلی، بدون نیاز به دسترسی). */
fun backupDir(context: Context): File = File(context.filesDir, "backups").apply { mkdirs() }

/** فهرست پشتیبان‌های خود برنامه، جدیدترین اول. */
fun listBackupFiles(context: Context): List<BackupFileInfo> {
    val dir = backupDir(context)
    val files = dir.listFiles() ?: return emptyList()
    return sortedBackupFiles(files.mapNotNull { toBackupFileInfo(it.name, it.length()) })
}

/** محتوای پشتیبان را با نام تاریخ‌دار در پوشه پشتیبان‌ها می‌نویسد. */
fun writeBackupFile(context: Context, json: String, createdMillis: Long): File {
    val file = File(backupDir(context), backupFileName(createdMillis))
    file.writeText(json, Charsets.UTF_8)
    return file
}
