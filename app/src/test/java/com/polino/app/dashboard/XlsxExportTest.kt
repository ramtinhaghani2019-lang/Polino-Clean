package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XlsxExportTest {

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
    fun xlsx_containsAllPartsAndValidXml() {
        val tx = listOf(
            MoneyTransaction("1", "حقوق", 5000000, true, "حقوق", 1762300000000, "main", ""),
            MoneyTransaction("2", "خرید <ویژه> & نان", 120000, false, "خوراک", 1762300000000, "main", "یادداشت")
        )
        val entries = readEntries(transactionsToXlsx(tx))
        assertEquals(
            setOf(
                "[Content_Types].xml",
                "_rels/.rels",
                "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels",
                "xl/worksheets/sheet1.xml",
                "xl/sharedStrings.xml",
                "xl/styles.xml"
            ),
            entries.keys
        )
        entries.values.forEach { parseXml(it) }
        val shared = entries.getValue("xl/sharedStrings.xml")
        assertTrue(shared.contains("count=\"16\""))
        assertTrue(shared.contains("حقوق"))
        assertTrue(shared.contains("خرید &lt;ویژه&gt; &amp; نان"))
        val sheet = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("<v>5000000</v>"))
        assertTrue(sheet.contains("s=\"1\""))
    }

    @Test
    fun xlsx_empty_hasHeaderRowOnly() {
        val entries = readEntries(transactionsToXlsx(emptyList()))
        val sheet = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("<row r=\"1\">"))
        assertTrue(!sheet.contains("<row r=\"2\">"))
    }
}
