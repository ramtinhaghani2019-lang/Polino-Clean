package com.polino.app.dashboard

import com.polino.app.data.MoneyTransaction
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * خروجی اکسل واقعی (.xlsx) بدون وابستگی سنگین: فایل یک زیپ استاندارد از
 * XMLهاست که اکسل و لیبره‌آفیس بازش می‌کنند. ستون‌ها: عنوان، مبلغ، نوع،
 * دسته، تاریخ شمسی، یادداشت. سطر اول سرستون بولد است.
 */
internal fun transactionsToXlsx(transactions: List<MoneyTransaction>): ByteArray {
    val strings = ArrayList<String>()
    fun intern(value: String): Int {
        val existing = strings.indexOf(value)
        if (existing >= 0) return existing
        strings.add(value)
        return strings.lastIndex
    }
    val headerCells = listOf("عنوان", "مبلغ", "نوع", "دسته", "تاریخ شمسی", "یادداشت")
        .map { intern(it) }
    data class RowCells(
        val title: Int,
        val amount: Long,
        val type: Int,
        val category: Int,
        val date: Int,
        val note: Int
    )
    val rowCells = transactions.map { t ->
        RowCells(
            intern(t.title),
            t.amount,
            intern(if (t.income) "درآمد" else "هزینه"),
            intern(t.category),
            intern(JalaliDate.formatJalali(t.dateMillis)),
            intern(t.note)
        )
    }
    val totalStringRefs = headerCells.size + rowCells.size * 5
    val sharedStrings = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
        append("count=\"$totalStringRefs\" uniqueCount=\"${strings.size}\">")
        strings.forEach { append("<si><t>").append(xmlEscape(it)).append("</t></si>") }
        append("</sst>")
    }
    val sheet = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        append("<sheetData>")
        append("<row r=\"1\">")
        val cols = listOf("A", "B", "C", "D", "E", "F")
        headerCells.forEachIndexed { i, idx -> stringCell(cols[i], 1, idx, true) }
        append("</row>")
        rowCells.forEachIndexed { ri, rc ->
            val r = ri + 2
            append("<row r=\"$r\">")
            stringCell("A", r, rc.title, false)
            append("<c r=\"B$r\"><v>${rc.amount}</v></c>")
            stringCell("C", r, rc.type, false)
            stringCell("D", r, rc.category, false)
            stringCell("E", r, rc.date, false)
            stringCell("F", r, rc.note, false)
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        fun entry(name: String, content: String) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        entry("[Content_Types].xml", XLSX_CONTENT_TYPES)
        entry("_rels/.rels", XLSX_RELS)
        entry("xl/workbook.xml", XLSX_WORKBOOK)
        entry("xl/_rels/workbook.xml.rels", XLSX_WORKBOOK_RELS)
        entry("xl/worksheets/sheet1.xml", sheet)
        entry("xl/sharedStrings.xml", sharedStrings)
        entry("xl/styles.xml", XLSX_STYLES)
    }
    return out.toByteArray()
}

private fun StringBuilder.stringCell(col: String, row: Int, idx: Int, header: Boolean) {
    append("<c r=\"$col$row\" t=\"s\"")
    if (header) append(" s=\"1\"")
    append("><v>$idx</v></c>")
}

private fun xmlEscape(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private const val XLSX_CONTENT_TYPES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
    "<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>" +
    "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
    "</Types>"

private const val XLSX_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
    "</Relationships>"

private const val XLSX_WORKBOOK = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
    "<sheets><sheet name=\"تراکنش‌ها\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
    "</workbook>"

private const val XLSX_WORKBOOK_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
    "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>" +
    "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
    "</Relationships>"

private const val XLSX_STYLES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
    "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
    "<fonts><font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
    "<font><sz val=\"11\"/><b/><name val=\"Calibri\"/></font></fonts>" +
    "<fills><fill><patternFill patternType=\"none\"/></fill>" +
    "<fill><patternFill patternType=\"gray125\"/></fill></fills>" +
    "<borders><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
    "<cellStyleXfs><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
    "<cellXfs><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
    "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>" +
    "<cellStyles><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
    "</styleSheet>"
