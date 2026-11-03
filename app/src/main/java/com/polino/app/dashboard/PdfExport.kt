package com.polino.app.dashboard

import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.io.OutputStream

/**
 * گزارش متنی چندخطی را به PDF تبدیل می‌کند. متن فارسی با StaticLayout
 * چیده می‌شود تا حروف به‌هم‌چسبیده و راست‌به‌چپ درست دربیایند؛ خروجی
 * در صورت بلند بودن متن، روی چند صفحه A4 پخش می‌شود.
 */
internal fun writePdfReport(outputStream: OutputStream, lines: List<String>) {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40
    val contentWidth = pageWidth - margin * 2
    val contentHeight = pageHeight - margin * 2
    val paint = TextPaint().apply {
        textSize = 16f
        isAntiAlias = true
    }
    val text = lines.joinToString("\n")
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
        .setLineSpacing(0f, 1.3f)
        .setIncludePad(true)
        .build()
    val document = PdfDocument()
    val pageCount = maxOf(1, (layout.height + contentHeight - 1) / contentHeight)
    for (pageIndex in 0 until pageCount) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.save()
        canvas.translate(margin.toFloat(), margin.toFloat() - pageIndex * contentHeight)
        layout.draw(canvas)
        canvas.restore()
        document.finishPage(page)
    }
    document.writeTo(outputStream)
    document.close()
}
