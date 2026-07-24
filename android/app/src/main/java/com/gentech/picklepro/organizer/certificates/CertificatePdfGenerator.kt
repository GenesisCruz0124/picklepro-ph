package com.gentech.picklepro.organizer.certificates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

enum class CertificateKind(val dbValue: String) {
    CHAMPION("champion"),
    RUNNER_UP("runner_up"),
    PARTICIPATION("participation"),
}

data class CertificateData(
    val kind: CertificateKind,
    val kindLabel: String,
    val awardedToLabel: String,
    val organizedByLabel: String,
    val recipientName: String,
    val tournamentName: String,
    val divisionName: String,
    val organizerName: String,
    val date: String,
    val logo: Bitmap?,
)

/**
 * On-device certificate PDF (spec §5.9) via the SDK's [PdfDocument] — no
 * external PDF library. Landscape A4 at 72dpi (842×595 pt), plain
 * Canvas/Paint text; a missing logo degrades to text-only rather than
 * blocking generation (tournament day may be offline, spec §5.10).
 */
object CertificatePdfGenerator {

    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595

    /** Writes the PDF into cacheDir/certificates and returns the file, ready for FileProvider sharing. */
    fun generate(context: Context, data: CertificateData): File {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas
        val centerX = PAGE_WIDTH / 2f

        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.rgb(0x2E, 0x7D, 0x32) // PickleGreen40
        }
        canvas.drawRect(RectF(24f, 24f, PAGE_WIDTH - 24f, PAGE_HEIGHT - 24f), borderPaint)

        data.logo?.let { logo ->
            val logoSize = 72f
            val scaled = Bitmap.createScaledBitmap(logo, logoSize.toInt(), logoSize.toInt(), true)
            canvas.drawBitmap(scaled, centerX - logoSize / 2, 44f, null)
        }

        val kindPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            color = Color.rgb(0x2E, 0x7D, 0x32)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 16f
            color = Color.DKGRAY
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            color = Color.BLACK
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 20f
            color = Color.BLACK
        }

        canvas.drawText(data.kindLabel, centerX, 170f, kindPaint)
        canvas.drawText(data.awardedToLabel, centerX, 230f, labelPaint)
        canvas.drawText(data.recipientName, centerX, 290f, namePaint)
        canvas.drawText(data.tournamentName, centerX, 360f, detailPaint)
        canvas.drawText(data.divisionName, centerX, 392f, detailPaint)
        canvas.drawText(data.date, centerX, 440f, labelPaint)
        canvas.drawText("${data.organizedByLabel} ${data.organizerName}", centerX, 500f, labelPaint)

        document.finishPage(page)

        val dir = File(context.cacheDir, "certificates").apply { mkdirs() }
        val safeName = data.recipientName.replace(Regex("[^A-Za-z0-9 ]"), "").replace(' ', '-').take(40)
        val file = File(dir, "${data.kind.dbValue}-$safeName.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }
}
