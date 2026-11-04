package com.polino.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupFilesTest {

    @Test
    fun backupFileName_roundTripsThroughParser() {
        val millis = 1793770000000L
        val name = backupFileName(millis)
        assertEquals("polino-backup-1793770000000.json", name)
        assertEquals(millis, backupCreatedMillisFromName(name))
    }

    @Test
    fun backupCreatedMillisFromName_rejectsNonBackupNames() {
        assertNull(backupCreatedMillisFromName("report.json"))
        assertNull(backupCreatedMillisFromName("polino-backup-abc.json"))
        assertNull(backupCreatedMillisFromName("polino-backup-123.txt"))
        assertNull(backupCreatedMillisFromName("polino-backup-.json"))
        assertNull(backupCreatedMillisFromName(""))
    }

    @Test
    fun toBackupFileInfo_returnsNullForNonBackupNames() {
        assertNull(toBackupFileInfo("notes.txt", 100L))
        val info = toBackupFileInfo("polino-backup-5000.json", 100L)
        assertEquals(5000L, info?.createdMillis)
        assertEquals(100L, info?.sizeBytes)
    }

    @Test
    fun sortedBackupFiles_newestFirstThenName() {
        val older = BackupFileInfo("polino-backup-1000.json", 10L, 1000L)
        val newer = BackupFileInfo("polino-backup-3000.json", 10L, 3000L)
        val middle = BackupFileInfo("polino-backup-2000.json", 10L, 2000L)
        val sorted = sortedBackupFiles(listOf(older, newer, middle))
        assertEquals(listOf(newer, middle, older), sorted)
    }

    @Test
    fun backupDisplaySize_formatsBytesKilobytesMegabytes() {
        assertEquals("512 بایت", backupDisplaySize(512L))
        assertEquals("1.0 کیلوبایت", backupDisplaySize(1024L))
        assertEquals("1.5 کیلوبایت", backupDisplaySize(1536L))
        assertEquals("2.0 مگابایت", backupDisplaySize(2L * 1024 * 1024))
    }
}
