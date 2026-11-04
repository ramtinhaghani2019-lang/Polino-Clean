package com.polino.app.dashboard

import com.polino.app.data.DebtEntity
import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class ExportSheetTest {

    private fun tx(
        id: String,
        title: String,
        amount: Long,
        income: Boolean,
        category: String,
        jy: Int,
        jm: Int,
        jd: Int
    ): MoneyTransaction = MoneyTransaction(
        id,
        title,
        amount,
        income,
        category,
        JalaliDate.jalaliToMillis(jy, jm, jd),
        "main",
        ""
    )

    private fun debt(
        id: String,
        person: String,
        amount: Long,
        receivable: Boolean,
        settled: Boolean
    ): DebtEntity = DebtEntity(id, person, amount, receivable, settled)

    private fun readEntries(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                result[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        return result
    }

    private fun parseXml(text: String) {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.newDocumentBuilder()
            .parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun persianDigits_convertsLatinDigitsOnly() {
        assertEquals("۱۴۰۵/۰۸/۰۱", persianDigits("1405/08/01"))
        assertEquals("abc۱۲۳", persianDigits("abc123"))
    }

    @Test
    fun bounds_thisMonth_isCurrentMonthOnly() {
        val bounds = exportRangeBounds(ExportRange.THIS_MONTH, "1405-08")
        assertEquals(listOf("1405-08"), bounds.months)
        assertEquals(JalaliDate.jalaliToMillis(1405, 8, 1), bounds.startMillis)
        assertEquals(JalaliDate.jalaliToMillis(1405, 9, 1), bounds.endExclusiveMillis)
        assertEquals("۱۴۰۵/۰۸/۰۱", bounds.fromLabel)
        assertEquals("۱۴۰۵/۰۸/۳۰", bounds.toLabel)
    }

    @Test
    fun bounds_previousMonth_rollsBackOneMonth() {
        val bounds = exportRangeBounds(ExportRange.PREVIOUS_MONTH, "1405-08")
        assertEquals(listOf("1405-07"), bounds.months)
        assertEquals(JalaliDate.jalaliToMillis(1405, 7, 1), bounds.startMillis)
        assertEquals(JalaliDate.jalaliToMillis(1405, 8, 1), bounds.endExclusiveMillis)
        assertEquals("۱۴۰۵/۰۷/۰۱", bounds.fromLabel)
        assertEquals("۱۴۰۵/۰۷/۳۰", bounds.toLabel)
    }

    @Test
    fun bounds_previousMonth_rollsBackYearAtFarvardin() {
        val bounds = exportRangeBounds(ExportRange.PREVIOUS_MONTH, "1405-01")
        assertEquals(listOf("1404-12"), bounds.months)
        assertEquals(JalaliDate.jalaliToMillis(1404, 12, 1), bounds.startMillis)
        assertEquals(JalaliDate.jalaliToMillis(1405, 1, 1), bounds.endExclusiveMillis)
    }

    @Test
    fun bounds_last3Months_spansThreeMonths() {
        val bounds = exportRangeBounds(ExportRange.LAST_3_MONTHS, "1405-08")
        assertEquals(listOf("1405-06", "1405-07", "1405-08"), bounds.months)
        assertEquals(JalaliDate.jalaliToMillis(1405, 6, 1), bounds.startMillis)
        assertEquals(JalaliDate.jalaliToMillis(1405, 9, 1), bounds.endExclusiveMillis)
        assertEquals("۱۴۰۵/۰۶/۰۱", bounds.fromLabel)
        assertEquals("۱۴۰۵/۰۸/۳۰", bounds.toLabel)
    }

    @Test
    fun bounds_last6Months_spansSixMonthsAcrossYear() {
        val bounds = exportRangeBounds(ExportRange.LAST_6_MONTHS, "1405-02")
        assertEquals(
            listOf("1404-09", "1404-10", "1404-11", "1404-12", "1405-01", "1405-02"),
            bounds.months
        )
        assertEquals(JalaliDate.jalaliToMillis(1404, 9, 1), bounds.startMillis)
        assertEquals(JalaliDate.jalaliToMillis(1405, 3, 1), bounds.endExclusiveMillis)
    }

    @Test
    fun sections_allContent_buildsFiveSectionsInPergarOrder() {
        val rangeTx = listOf(
            tx("1", "حقوق", 1000000, true, "حقوق", 1405, 8, 5),
            tx("2", "خرید", 200000, false, "خوراک", 1405, 8, 10),
            tx("3", "خرید", 150000, false, "خوراک", 1405, 8, 12)
        )
        val debts = listOf(
            debt("d1", "علی", 300000, true, true),
            debt("d2", "علی", 100000, true, false),
            debt("d3", "سارا", 50000, false, true),
            debt("d4", "سارا", 70000, false, false)
        )
        val sections = buildExportSections(rangeTx, debts, ExportContent())
        assertEquals(
            listOf("خلاصه مالی", "تراکنش‌ها", "دسته‌بندی‌ها", "طلب‌ها", "بدهی‌ها"),
            sections.map { it.name }
        )
        val summary = sections[0]
        assertEquals(listOf("شاخص", "مبلغ (تومان)"), summary.header)
        assertEquals(XlsxCell.Number(1000000), summary.rows[0][1])
        assertEquals(XlsxCell.Number(350000), summary.rows[1][1])
        assertEquals(XlsxCell.Number(650000), summary.rows[2][1])
        assertEquals(3, sections[1].rows.size)
        assertEquals(
            listOf(listOf(XlsxCell.Text("خوراک"), XlsxCell.Number(350000))),
            sections[2].rows
        )
        assertEquals(
            listOf(
                listOf(
                    XlsxCell.Text("علی"),
                    XlsxCell.Number(400000),
                    XlsxCell.Number(300000),
                    XlsxCell.Number(100000)
                )
            ),
            sections[3].rows
        )
        assertEquals(
            listOf(
                listOf(
                    XlsxCell.Text("سارا"),
                    XlsxCell.Number(120000),
                    XlsxCell.Number(50000),
                    XlsxCell.Number(70000)
                )
            ),
            sections[4].rows
        )
    }

    @Test
    fun sections_onlyTransactions_buildsSingleSection() {
        val sections = buildExportSections(
            emptyList(),
            emptyList(),
            ExportContent(
                summary = false,
                transactions = true,
                categories = false,
                receivables = false,
                payables = false
            )
        )
        assertEquals(listOf("تراکنش‌ها"), sections.map { it.name })
        assertEquals(0, sections[0].rows.size)
    }

    @Test
    fun sections_noContent_buildsNothing() {
        val sections = buildExportSections(
            emptyList(),
            emptyList(),
            ExportContent(
                summary = false,
                transactions = false,
                categories = false,
                receivables = false,
                payables = false
            )
        )
        assertTrue(sections.isEmpty())
    }

    @Test
    fun content_anySelected_reflectsAllFlags() {
        assertTrue(ExportContent().anySelected)
        assertFalse(
            ExportContent(
                summary = false,
                transactions = false,
                categories = false,
                receivables = false,
                payables = false
            ).anySelected
        )
    }

    @Test
    fun sectionsToXlsx_twoSections_hasBothSheetsAndValidXml() {
        val sections = listOf(
            XlsxSection(
                "خلاصه مالی",
                listOf("شاخص", "مبلغ (تومان)"),
                listOf(
                    listOf(XlsxCell.Text("درآمد"), XlsxCell.Number(1000000))
                )
            ),
            XlsxSection(
                "تراکنش‌ها",
                listOf("عنوان", "مبلغ", "نوع", "دسته", "تاریخ شمسی", "یادداشت"),
                emptyList()
            )
        )
        val entries = readEntries(sectionsToXlsx(sections))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))
        assertTrue(entries.containsKey("xl/sharedStrings.xml"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        assertTrue(entries["xl/workbook.xml"]!!.contains("خلاصه مالی"))
        assertTrue(entries["xl/workbook.xml"]!!.contains("تراکنش‌ها"))
        entries.values.forEach { parseXml(it) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun sectionsToXlsx_emptySections_rejected() {
        sectionsToXlsx(emptyList())
    }
}
