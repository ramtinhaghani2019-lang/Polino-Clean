package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupV2Test {

    @Test
    fun fullBackup_roundTripsAllSections() {
        val data = BackupData(
            transactions = listOf(
                MoneyTransaction("t1", "خرید", 50000, false, "خوراک", 1000L, "main", "قرض به علی")
            ),
            budgets = listOf(BudgetEntity("1405-08", "خوراک", 200000)),
            categories = listOf(CategoryEntity("کرایه", false)),
            accounts = listOf(AccountEntity("main", "اصلی"), AccountEntity("a2", "پس‌انداز")),
            recurring = listOf(
                RecurringEntity("r1", "حقوق", 1000000, true, "حقوق", "main", 1, "1405-08", null),
                RecurringEntity("r2", "اجاره", 500000, false, "مسکن", "main", 5, "1405-08", "1405-08")
            ),
            goals = listOf(GoalEntity("g1", "سفر", 5000000, "a2"))
        )
        val parsed = parseBackup(fullBackupToJson(data))
        assertTrue(parsed != null)
        assertEquals(data, parsed)
    }

    @Test
    fun legacyArray_isNotFullBackup() {
        val legacy = transactionsToJson(
            listOf(MoneyTransaction("t1", "خرید", 50000, false, "خوراک", 1000L))
        )
        assertNull(parseBackup(legacy))
        // اما مسیر قدیمی همان فایل را همچنان می‌خواند.
        assertEquals(1, parseLegacyTransactions(legacy).size)
    }

    @Test
    fun fullBackup_legacyReaderStillReadsTransactions() {
        val data = BackupData(
            transactions = listOf(
                MoneyTransaction("t1", "خرید", 50000, false, "خوراک", 1000L, "main", "یادداشت")
            ),
            budgets = listOf(BudgetEntity("1405-08", "خوراک", 200000)),
            categories = emptyList(),
            accounts = emptyList(),
            recurring = emptyList(),
            goals = emptyList()
        )
        val legacyRead = parseLegacyTransactions(fullBackupToJson(data))
        assertEquals(1, legacyRead.size)
        assertEquals("خرید", legacyRead[0].title)
    }

    @Test
    fun garbage_isNotFullBackup() {
        assertNull(parseBackup("not json"))
        assertNull(parseBackup(""))
        assertNull(parseBackup("[]"))
    }
}
