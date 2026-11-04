package com.polino.app.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.polino.app.data.BackupData
import com.polino.app.data.BudgetRepository
import com.polino.app.data.TransactionRepository
import com.polino.app.data.fullBackupToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** نگهداری پشتیبان خودکار: فقط ۷ روز. */
const val AUTO_BACKUP_RETENTION_DAYS = 7L

private const val AUTO_BACKUP_PREFS = "polino_backup_prefs"
private const val LAST_AUTO_BACKUP_DAY_KEY = "last_auto_backup_day"
private const val DOCUMENTS_RELATIVE_PATH = "Documents/PolinoBackups"
private const val DOCUMENTS_RELATIVE_PATH_QUERY = "Documents/PolinoBackups/"

private val AUTO_BACKUP_NAME_REGEX = Regex("^polino-auto-backup-(\\d+)\\.json$")

/** نام فایل پشتیبان خودکار برای زمان ساخت. */
fun autoBackupFileName(createdMillis: Long): String = "polino-auto-backup-$createdMillis.json"

/** زمان ساخت را از نام فایل خودکار می‌خواند؛ بقیه نام‌ها null. */
fun autoBackupCreatedMillisFromName(name: String): Long? {
    val match = AUTO_BACKUP_NAME_REGEX.matchEntire(name) ?: return null
    return match.groupValues[1].toLongOrNull()
}

/** روز میلادی (از epoch) برای مقایسه «همین امروز». */
fun epochDay(millis: Long): Long = millis / 86400000L

/** آستانه حذف: پشتیبان‌های قدیمی‌تر از این لحظه منقضی‌اند. */
fun retentionCutoffMillis(nowMillis: Long): Long =
    nowMillis - AUTO_BACKUP_RETENTION_DAYS * 86400000L

/** پشتیبان خودکار با زمان ساخت داده‌شده منقضی است؟ */
fun isAutoBackupExpired(createdMillis: Long, nowMillis: Long): Boolean =
    createdMillis < retentionCutoffMillis(nowMillis)

/** امروز هنوز پشتیبان خودکار ساخته نشده است؟ */
fun shouldRunAutoBackup(lastBackupDay: Long?, todayDay: Long): Boolean =
    lastBackupDay != todayDay

/** یک فایل پشتیبان خودکار در پوشه Documents با نشانی محتوایی‌اش. */
data class DocumentsBackup(val info: BackupFileInfo, val uri: Uri?)

/** یک ردیف فهرست یکپارچه: پشتیبان دستی یا خودکار. */
data class ListedBackup(
    val info: BackupFileInfo,
    val isAuto: Boolean,
    val documentUri: Uri?
)

/** ادغام دو فهرست؛ جدیدترین اول، بعد نام برای ترتیب قطعی. */
fun mergeBackupLists(
    manual: List<BackupFileInfo>,
    auto: List<DocumentsBackup>
): List<ListedBackup> {
    val merged = manual.map { ListedBackup(it, isAuto = false, documentUri = null) } +
        auto.map { ListedBackup(it.info, isAuto = true, documentUri = it.uri) }
    return merged.sortedWith(
        compareByDescending<ListedBackup> { it.info.createdMillis }.thenBy { it.info.name }
    )
}

private fun documentsCollectionUri(): Uri =
    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

/**
 * نوشتن پشتیبان خودکار در Documents/PolinoBackups از راه MediaStore
 * (بدون هیچ دسترسی)؛ در اندرویدهای قدیمی‌تر از ۱۰ ممکن نیست و null برمی‌گردد.
 */
suspend fun writeDocumentsAutoBackup(context: Context, json: String, createdMillis: Long): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, autoBackupFileName(createdMillis))
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, DOCUMENTS_RELATIVE_PATH)
            }
            val uri = context.contentResolver.insert(documentsCollectionUri(), values)
                ?: return@runCatching null
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } ?: return@runCatching null
            uri
        }.getOrNull()
    }
}

/** فهرست پشتیبان‌های خودکار در Documents/PolinoBackups. */
suspend fun listDocumentsAutoBackups(context: Context): List<DocumentsBackup> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
    return withContext(Dispatchers.IO) {
        runCatching {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
            )
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val args = arrayOf(DOCUMENTS_RELATIVE_PATH_QUERY)
            val found = mutableListOf<DocumentsBackup>()
            context.contentResolver.query(
                documentsCollectionUri(), projection, selection, args, null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                    val created = autoBackupCreatedMillisFromName(name) ?: continue
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    val id = if (idIndex >= 0) cursor.getLong(idIndex) else continue
                    found.add(
                        DocumentsBackup(
                            BackupFileInfo(name, size, created),
                            ContentUris.withAppendedId(documentsCollectionUri(), id)
                        )
                    )
                }
            }
            found
        }.getOrDefault(emptyList())
    }
}

/** حذف پشتیبان‌های خودکار منقضی (قدیمی‌تر از ۷ روز)؛ تعداد حذف‌شده برمی‌گردد. */
suspend fun pruneDocumentsAutoBackups(context: Context, nowMillis: Long): Int {
    val expired = listDocumentsAutoBackups(context).filter {
        isAutoBackupExpired(it.info.createdMillis, nowMillis)
    }
    var deleted = 0
    withContext(Dispatchers.IO) {
        expired.forEach { item ->
            val uri = item.uri ?: return@forEach
            runCatching { context.contentResolver.delete(uri, null, null) }
                .onSuccess { count -> if (count > 0) deleted++ }
        }
    }
    return deleted
}

/** خواندن متن پشتیبان خودکار از نشانی محتوایی‌اش. */
suspend fun readDocumentsBackupText(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        }.getOrDefault("")
    }
}

/**
 * اجرای پشتیبان‌گیری خودکار روزانه: اگر امروز هنوز ساخته نشده، یک نسخه
 * در Documents/PolinoBackups می‌نویسد و پشتیبان‌های منقضی را پاک می‌کند.
 * همه خطاها بلعیده می‌شوند تا شروع برنامه هرگز به‌خاطر پشتیبان نایستد.
 */
internal suspend fun runDailyAutoBackupIfNeeded(
    context: Context,
    transactionRepository: TransactionRepository,
    budgetRepository: BudgetRepository
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    withContext(Dispatchers.IO) {
        runCatching {
            val prefs = context.getSharedPreferences(AUTO_BACKUP_PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastDay = if (prefs.contains(LAST_AUTO_BACKUP_DAY_KEY)) {
                prefs.getLong(LAST_AUTO_BACKUP_DAY_KEY, -1L)
            } else {
                null
            }
            if (!shouldRunAutoBackup(lastDay, epochDay(now))) return@runCatching
            // هر اسکن، اول منقضی‌ها را پاک می‌کند.
            pruneDocumentsAutoBackups(context, now)
            transactionRepository.migrateIfNeeded()
            val data = BackupData(
                transactions = transactionRepository.getAll(),
                budgets = budgetRepository.getAllBudgets(),
                categories = transactionRepository.getCategories(),
                accounts = transactionRepository.getAccounts(),
                recurring = transactionRepository.getRecurring(),
                goals = transactionRepository.getGoals(),
                accountSmsSenders = transactionRepository.getAccountSmsLinks(),
                installments = transactionRepository.getInstallments(),
                plans = transactionRepository.getPlans()
            )
            val written = writeDocumentsAutoBackup(context, fullBackupToJson(data), now)
            if (written != null) {
                prefs.edit().putLong(LAST_AUTO_BACKUP_DAY_KEY, epochDay(now)).apply()
            }
        }
    }
}
