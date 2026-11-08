package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportHelper {

    fun generateAndSharePdf(
        context: Context,
        title: String,
        period: String,
        headers: List<String>,
        rows: List<List<String>>,
        summaryMetrics: List<Pair<String, String>>
    ) {
        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // Standard A4 width in points
            val pageHeight = 842 // Standard A4 height in points
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // 1. Header Banner
            paint.color = Color.parseColor("#1B5E20") // Rich Dairy Green
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("MilkMate Dairy OS", 30f, 40f, paint)

            paint.textSize = 13f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(title, 30f, 62f, paint)

            paint.textSize = 10f
            paint.color = Color.parseColor("#E8F5E9")
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Generated: $dateStr • Period: $period", 30f, 78f, paint)

            var yPos = 120f

            // 2. Summary Metric Cards
            if (summaryMetrics.isNotEmpty()) {
                val boxWidth = (pageWidth - 60f - (summaryMetrics.size - 1) * 10f) / summaryMetrics.size
                summaryMetrics.forEachIndexed { index, (label, value) ->
                    val xPos = 30f + index * (boxWidth + 10f)

                    paint.color = Color.parseColor("#F1F8E9")
                    canvas.drawRoundRect(xPos, yPos, xPos + boxWidth, yPos + 45f, 6f, 6f, paint)

                    paint.color = Color.parseColor("#2E7D32")
                    paint.textSize = 14f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(value, xPos + 10f, yPos + 22f, paint)

                    paint.color = Color.parseColor("#555555")
                    paint.textSize = 9f
                    paint.typeface = Typeface.DEFAULT
                    canvas.drawText(label, xPos + 10f, yPos + 38f, paint)
                }
                yPos += 65f
            }

            // 3. Table Header
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawRect(30f, yPos, pageWidth - 30f, yPos + 26f, paint)

            paint.color = Color.parseColor("#222222")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val colWidth = (pageWidth - 60f) / headers.size.coerceAtLeast(1)
            headers.forEachIndexed { i, h ->
                canvas.drawText(h, 35f + (i * colWidth), yPos + 17f, paint)
            }
            yPos += 26f

            // 4. Table Rows with Multi-Page Support
            val rowsPerPage = 26
            val totalPages = (rows.size + rowsPerPage - 1) / rowsPerPage
            val actualPages = maxOf(1, totalPages)

            var currentPage = page
            var currentPageNum = 1

            for ((rowIndex, row) in rows.withIndex()) {
                if (rowIndex > 0 && rowIndex % rowsPerPage == 0) {
                    // Draw footer on previous page
                    paint.color = Color.parseColor("#888888")
                    paint.textSize = 8f
                    canvas.drawText("Page $currentPageNum of $actualPages • Verified by MilkMate Dairy OS", 30f, pageHeight - 20f, paint)

                    pdfDoc.finishPage(currentPage)
                    currentPageNum++

                    // Start new page
                    val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
                    currentPage = pdfDoc.startPage(newPageInfo)
                    val newCanvas = currentPage.canvas

                    // Mini Header on subsequent pages
                    paint.color = Color.parseColor("#1B5E20")
                    newCanvas.drawRect(0f, 0f, pageWidth.toFloat(), 40f, paint)
                    paint.color = Color.WHITE
                    paint.textSize = 12f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    newCanvas.drawText("$title (Page $currentPageNum)", 30f, 25f, paint)

                    // Re-draw table headers on new page
                    yPos = 60f
                    paint.color = Color.parseColor("#E0E0E0")
                    newCanvas.drawRect(30f, yPos, pageWidth - 30f, yPos + 24f, paint)
                    paint.color = Color.parseColor("#222222")
                    paint.textSize = 9f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    headers.forEachIndexed { i, h ->
                        newCanvas.drawText(h, 35f + (i * colWidth), yPos + 16f, paint)
                    }
                    yPos += 24f
                }

                val targetCanvas = currentPage.canvas
                if (rowIndex % 2 == 1) {
                    paint.color = Color.parseColor("#F9F9F9")
                    targetCanvas.drawRect(30f, yPos, pageWidth - 30f, yPos + 20f, paint)
                }

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 9f
                paint.color = Color.parseColor("#333333")
                row.forEachIndexed { colIndex, cell ->
                    val textToDraw = if (cell.length > 24) cell.take(22) + ".." else cell
                    targetCanvas.drawText(textToDraw, 35f + (colIndex * colWidth), yPos + 14f, paint)
                }

                paint.color = Color.parseColor("#EEEEEE")
                targetCanvas.drawLine(30f, yPos + 20f, pageWidth - 30f, yPos + 20f, paint)
                yPos += 20f
            }

            // Footer on last page
            val finalCanvas = currentPage.canvas
            paint.color = Color.parseColor("#888888")
            paint.textSize = 8f
            finalCanvas.drawText("Page $currentPageNum of $actualPages • Verified and generated by MilkMate Dairy OS • Confidential", 30f, pageHeight - 20f, paint)

            pdfDoc.finishPage(currentPage)

            // Save PDF to cache
            val file = File(context.cacheDir, "${title.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()

            // Share intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$title - MilkMate PDF Statement")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Download / Share PDF Report"))
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                android.widget.Toast.makeText(context, "Could not export PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    fun generateAndShareExcel(
        context: Context,
        title: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        try {
            val csvBuilder = java.lang.StringBuilder()
            csvBuilder.append("\uFEFF") // UTF-8 BOM for Microsoft Excel compatibility
            // Title Header row
            csvBuilder.append("\"").append(title).append("\"\n")
            csvBuilder.append("\"Generated: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())).append("\"\n\n")

            // Column Headers
            csvBuilder.append(headers.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")

            // Data Rows
            for (row in rows) {
                csvBuilder.append(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")
            }

            val file = File(context.cacheDir, "${title.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.csv")
            file.writeText(csvBuilder.toString(), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$title - MilkMate Excel Sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export to Excel / Sheets"))
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                android.widget.Toast.makeText(context, "Could not export Excel: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    fun shareWhatsApp(context: Context, text: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                this.action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share via WhatsApp"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
