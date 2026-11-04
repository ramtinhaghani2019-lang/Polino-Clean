package com.polino.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupTest {

    @Test
    fun autoBackupFileName_roundTripsThroughParser() {
        val millis = 1793776000000L
        assertEquals("polino-auto-backup-1793776000000.json", autoBackupFileName(millis))
        assertEquals(millis, autoBackupCreatedMillisFromName(autoBackupFileName(millis)))
    }

    @Test
    fun autoBackupCreatedMillisFromName_rejectsOtherNames() {
        assertNull(autoBackupCreatedMillisFromName("polino-backup-1793776000000.json"))
        assertNull(autoBackupCreatedMillisFromName("polino-auto-backup-abc.json"))
        assertNull(autoBackupCreatedMillisFromName("random.json"))
        assertNull(autoBackupCreatedMillisFromName(""))
    }

    @Test
    fun retentionCutoff_isExactlySevenDays() {
        val now = 1793776000000L
        assertEquals(now - 7L * 86400000L, retentionCutoffMillis(now))
    }

    @Test
    fun isAutoBackupExpired_boundary() {
        val now = 1793776000000L
        val cutoff = retentionCutoffMillis(now)
        assertTrue(isAutoBackupExpired(cutoff - 1, now))
        assertFalse(isAutoBackupExpired(cutoff, now))
        assertFalse(isAutoBackupExpired(cutoff + 1, now))
        assertFalse(isAutoBackupExpired(now + 1000, now))
    }

    @Test
    fun epochDay_boundaries() {
        assertEquals(0L, epochDay(0L))
        assertEquals(0L, epochDay(86399999L))
        assertEquals(1L, epochDay(86400000L))
    }

    @Test
    fun shouldRunAutoBackup_onlyOnNewDay() {
        assertTrue(shouldRunAutoBackup(null, 100L))
        assertFalse(shouldRunAutoBackup(100L, 100L))
        assertTrue(shouldRunAutoBackup(99L, 100L))
    }

    @Test
    fun mergeBackupLists_newestFirstMixedSources() {
        val manual = listOf(BackupFileInfo("polino-backup-1000.json", 10L, 1000L))
        val auto = listOf(
            DocumentsBackup(BackupFileInfo("polino-auto-backup-3000.json", 20L, 3000L), null),
            DocumentsBackup(BackupFileInfo("polino-auto-backup-2000.json", 30L, 2000L), null)
        )
        val merged = mergeBackupLists(manual, auto)
        assertEquals(listOf(3000L, 2000L, 1000L), merged.map { it.info.createdMillis })
        assertEquals(listOf(true, true, false), merged.map { it.isAuto })
        assertEquals(listOf(null, null, null), merged.map { it.documentUri })
    }

    @Test
    fun mergeBackupLists_emptyStaysEmpty() {
        assertTrue(mergeBackupLists(emptyList(), emptyList()).isEmpty())
    }
}
