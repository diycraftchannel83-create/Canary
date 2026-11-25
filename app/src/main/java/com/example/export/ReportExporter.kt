package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BreedingClutch
import com.example.data.model.BreedingPair
import com.example.data.model.Canary
import com.example.data.model.HealthLog
import com.example.utils.IncubationCalculator
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class MonthlyProductivityStat(
    val monthKey: String,
    val monthLabel: String,
    val eggs: Int,
    val fertile: Int,
    val hatched: Int,
    val fledged: Int
)

object ReportExporter {

    fun exportPdfReport(
        context: Context,
        aviaryName: String,
        farmAddress: String = "",
        farmPhone: String = "",
        canaries: List<Canary>,
        pairs: List<BreedingPair>,
        clutches: List<BreedingClutch>,
        monthlyStats: List<MonthlyProductivityStat>,
        healthLogs: List<HealthLog>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val outFile = File(reportsDir, "Laporan_Kenari_$stamp.pdf")

        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val titlePaint = Paint().apply {
            color = Color.rgb(27, 67, 50)
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(82, 94, 85)
            textSize = 10f
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(27, 67, 50)
            textSize = 13f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(25, 34, 28)
            textSize = 9.5f
        }
        val linePaint = Paint().apply {
            color = Color.rgb(213, 204, 185)
            strokeWidth = 1f
        }

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = margin + 10f

        fun newPageIfNeeded(requiredHeight: Float) {
            if (y + requiredHeight > pageHeight - margin) {
                pdf.finishPage(page)
                pageNum += 1
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = margin
            }
        }

        // Header
        canvas.drawText("KENARI TASIKMALAYA - SILSILAH & LAPORAN", margin, y, titlePaint)
        y += 18f
        val generatedAt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
        canvas.drawText("Farm / Aviary: $aviaryName  |  Dicetak: $generatedAt", margin, y, subtitlePaint)
        if (farmAddress.isNotBlank() || farmPhone.isNotBlank()) {
            y += 13f
            val contactLine = listOfNotNull(
                farmAddress.takeIf { it.isNotBlank() }?.let { "Alamat: $it" },
                farmPhone.takeIf { it.isNotBlank() }?.let { "Telp: $it" }
            ).joinToString("  |  ")
            canvas.drawText(contactLine, margin, y, subtitlePaint)
        }
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f

        // Summary KPIs
        val totalEggs = clutches.sumOf { it.eggCount.toInt() }
        val totalHatched = clutches.sumOf { it.hatchedCount.toInt() }
        val hatchRate = if (totalEggs > 0) (totalHatched * 100 / totalEggs) else 0
        canvas.drawText("Ringkasan Aviary", margin, y, sectionPaint)
        y += 16f
        canvas.drawText(
            "Total Indukan: ${canaries.size} (Jantan: ${canaries.count { it.sex == "MALE" }}, Betina: ${canaries.count { it.sex == "FEMALE" }})   |   Pasangan: ${pairs.size}",
            margin, y, bodyPaint
        )
        y += 14f
        canvas.drawText(
            "Total Periode/Clutch: ${clutches.size}   |   Total Telur: $totalEggs   |   Menetas: $totalHatched ($hatchRate%)",
            margin, y, bodyPaint
        )
        y += 22f

        // Monthly productivity table
        newPageIfNeeded(60f)
        canvas.drawText("Produktivitas Bulanan", margin, y, sectionPaint)
        y += 15f
        canvas.drawText("Bulan          Telur     Fertil    Menetas   Sapih", margin, y, bodyPaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 12f
        monthlyStats.forEach { stat ->
            newPageIfNeeded(18f)
            val row = String.format(
                Locale.US,
                "%-14s %-9d %-9d %-9d %-9d",
                stat.monthLabel.take(12), stat.eggs, stat.fertile, stat.hatched, stat.fledged
            )
            canvas.drawText(row, margin, y, bodyPaint)
            y += 14f
        }
        y += 12f

        // Canary genealogy list
        newPageIfNeeded(50f)
        canvas.drawText("Daftar Indukan & Silsilah (${canaries.size})", margin, y, sectionPaint)
        y += 15f
        val byId = canaries.associateBy { it.id }
        canaries.forEach { bird ->
            newPageIfNeeded(40f)
            val sireLabel = bird.sireId?.let { byId[it]?.ringCode } ?: bird.sireRing ?: "-"
            val damLabel = bird.damId?.let { byId[it]?.ringCode } ?: bird.damRing ?: "-"
            canvas.drawText(
                "${bird.ringCode} | ${bird.name} (${bird.sexEnum.labelId}) - ${bird.breed} | Skor Kesehatan: ${bird.healthScore}",
                margin, y, bodyPaint
            )
            y += 12f
            canvas.drawText(
                "   Silsilah: Jantan $sireLabel | Betina $damLabel${if (bird.lineageNotes.isNotBlank()) " (${bird.lineageNotes})" else ""} | Kandang: ${bird.cageCode.ifBlank { "-" }}",
                margin, y, subtitlePaint
            )
            y += 12f
            if (bird.recordedClutchCount > 0 || bird.recordedEggCount > 0 || bird.productionHistoryNotes.isNotBlank()) {
                canvas.drawText(
                    "   Riwayat Produksi: ${bird.recordedClutchCount} Periode, ${bird.recordedEggCount} Telur, ${bird.recordedHatchedCount} Menetas, ${bird.recordedFledgedCount} Sapih${if (bird.productionHistoryNotes.isNotBlank()) " • ${bird.productionHistoryNotes}" else ""}",
                    margin, y, subtitlePaint
                )
                y += 12f
            }
            y += 4f
        }
        y += 10f

        // Breeding Clutches
        newPageIfNeeded(50f)
        canvas.drawText("Riwayat Pengeraman & Produksi (${clutches.size})", margin, y, sectionPaint)
        y += 15f
        clutches.forEach { clutch ->
            newPageIfNeeded(28f)
            val startStr = clutch.incubationStartEpochMs?.let { IncubationCalculator.formatShortDate(it) } ?: "-"
            val hatchStr = clutch.expectedHatchEpochMs?.let { IncubationCalculator.formatShortDate(it) } ?: "-"
            canvas.drawText(
                "Kandang ${clutch.cageCode} #${clutch.clutchNumber} [${clutch.statusEnum.labelId}] | Telur: ${clutch.eggCount}, Fertil: ${clutch.fertileCount}, Menetas: ${clutch.hatchedCount}, Sapih: ${clutch.fledgedCount}",
                margin, y, bodyPaint
            )
            y += 12f
            canvas.drawText(
                "   Mulai Mengeram: $startStr  |  Estimasi Menetas (21 Hari): $hatchStr",
                margin, y, subtitlePaint
            )
            y += 14f
        }

        // Recent Health Alerts
        if (healthLogs.isNotEmpty()) {
            y += 10f
            newPageIfNeeded(50f)
            canvas.drawText("Riwayat Kesehatan & Peringatan Dini", margin, y, sectionPaint)
            y += 15f
            healthLogs.take(20).forEach { log ->
                newPageIfNeeded(24f)
                val dateStr = IncubationCalculator.formatShortDate(log.recordedDateEpochMs)
                val flag = if (log.alertTriggered) " [PERINGATAN DINI]" else ""
                canvas.drawText(
                    "$dateStr | ${log.canaryRing} ${log.canaryName}: Skor ${log.previousScore} -> ${log.healthScore}$flag",
                    margin, y, bodyPaint
                )
                y += 12f
                if (log.symptoms.isNotBlank()) {
                    canvas.drawText("   Gejala: ${log.symptoms} | Tindakan: ${log.actionTaken}", margin, y, subtitlePaint)
                    y += 13f
                }
            }
        }

        pdf.finishPage(page)
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        return outFile
    }

    /**
     * Generates a genuine multi-sheet OOXML .xlsx Excel workbook using standard ZipOutputStream.
     */
    fun exportExcelWorkbook(
        context: Context,
        aviaryName: String,
        farmAddress: String = "",
        farmPhone: String = "",
        canaries: List<Canary>,
        pairs: List<BreedingPair>,
        clutches: List<BreedingClutch>,
        monthlyStats: List<MonthlyProductivityStat>,
        healthLogs: List<HealthLog>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val outFile = File(reportsDir, "Laporan_Kenari_$stamp.xlsx")
        FileOutputStream(outFile).use { fos ->
            writeXlsxToStream(fos, aviaryName, farmAddress, farmPhone, canaries, pairs, clutches, monthlyStats, healthLogs)
        }
        return outFile
    }

    fun writeXlsxToStream(
        outputStream: OutputStream,
        aviaryName: String,
        farmAddress: String = "",
        farmPhone: String = "",
        canaries: List<Canary>,
        pairs: List<BreedingPair>,
        clutches: List<BreedingClutch>,
        monthlyStats: List<MonthlyProductivityStat>,
        healthLogs: List<HealthLog>
    ) {
        val byId = canaries.associateBy { it.id }

        val sheet1Rows = mutableListOf<List<String>>(
            listOf("Kenari Tasikmalaya - $aviaryName"),
            listOf("Alamat: ${farmAddress.ifBlank { "-" }}", "Telepon: ${farmPhone.ifBlank { "-" }}"),
            listOf("Bulan", "Telur", "Fertil", "Menetas", "Anakan Sapih")
        )
        monthlyStats.forEach { s ->
            sheet1Rows.add(listOf(s.monthLabel, s.eggs.toString(), s.fertile.toString(), s.hatched.toString(), s.fledged.toString()))
        }

        val sheet2Rows = mutableListOf<List<String>>(
            listOf("Kode Ring", "Nama", "Jenis Kelamin", "Strain/Ras", "Warna Bulu", "Induk Jantan", "Induk Betina", "Keterangan Silsilah", "Periode Tercatat", "Total Telur", "Total Menetas", "Total Sapih", "Catatan Produksi", "Kandang", "Status Breeding", "Status Kesehatan", "Skor Kesehatan", "Catatan")
        )
        canaries.forEach { c ->
            val sire = c.sireId?.let { byId[it]?.ringCode } ?: c.sireRing ?: "-"
            val dam = c.damId?.let { byId[it]?.ringCode } ?: c.damRing ?: "-"
            sheet2Rows.add(
                listOf(
                    c.ringCode, c.name, c.sexEnum.labelId, c.breed, c.featherColor,
                    sire, dam, c.lineageNotes,
                    c.recordedClutchCount.toString(), c.recordedEggCount.toString(),
                    c.recordedHatchedCount.toString(), c.recordedFledgedCount.toString(),
                    c.productionHistoryNotes, c.cageCode, c.breedingStatusEnum.labelId,
                    c.healthStatusEnum.labelId, c.healthScore.toString(), c.notes
                )
            )
        }

        val sheet3Rows = mutableListOf<List<String>>(
            listOf("Kandang", "Pasangan", "Periode Ke", "Jumlah Telur", "Fertil", "Menetas", "Sapih", "Mulai Mengeram", "Estimasi Menetas (21 Hari)", "Status", "Catatan")
        )
        val pairsById = pairs.associateBy { it.id }
        clutches.forEach { cl ->
            val pair = pairsById[cl.pairId]
            val pairLabel = if (pair != null) "${pair.sireRing} x ${pair.damRing}" else "${cl.sireId} x ${cl.damId}"
            val incStart = cl.incubationStartEpochMs?.let { IncubationCalculator.formatDate(it) } ?: "-"
            val expHatch = cl.expectedHatchEpochMs?.let { IncubationCalculator.formatDate(it) } ?: "-"
            sheet3Rows.add(
                listOf(
                    cl.cageCode, pairLabel, cl.clutchNumber.toString(),
                    cl.eggCount.toString(), cl.fertileCount.toString(),
                    cl.hatchedCount.toString(), cl.fledgedCount.toString(),
                    incStart, expHatch, cl.statusEnum.labelId, cl.notes
                )
            )
        }

        val sheet4Rows = mutableListOf<List<String>>(
            listOf("Tanggal", "Ring", "Nama Kenari", "Skor Sebelumnya", "Skor Baru", "Status", "Peringatan Dini", "Gejala", "Tindakan")
        )
        healthLogs.forEach { h ->
            sheet4Rows.add(
                listOf(
                    IncubationCalculator.formatDate(h.recordedDateEpochMs),
                    h.canaryRing, h.canaryName, h.previousScore.toString(),
                    h.healthScore.toString(), h.healthStatusEnum.labelId,
                    if (h.alertTriggered) "YA" else "TIDAK", h.symptoms, h.actionTaken
                )
            )
        }

        ZipOutputStream(outputStream).use { zip ->
            fun addEntry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            addEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
            )
            addEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )
            addEntry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Analitik Bulanan" sheetId="1" r:id="rId1"/>
    <sheet name="Silsilah Indukan" sheetId="2" r:id="rId2"/>
    <sheet name="Pengeraman 21 Hari" sheetId="3" r:id="rId3"/>
    <sheet name="Log Kesehatan" sheetId="4" r:id="rId4"/>
  </sheets>
</workbook>"""
            )
            addEntry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
</Relationships>"""
            )
            addEntry("xl/worksheets/sheet1.xml", buildWorksheetXml(sheet1Rows))
            addEntry("xl/worksheets/sheet2.xml", buildWorksheetXml(sheet2Rows))
            addEntry("xl/worksheets/sheet3.xml", buildWorksheetXml(sheet3Rows))
            addEntry("xl/worksheets/sheet4.xml", buildWorksheetXml(sheet4Rows))
        }
    }

    private fun buildWorksheetXml(rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rIdx, cols ->
            val rowNum = rIdx + 1
            sb.append("""<row r="$rowNum">""")
            cols.forEachIndexed { cIdx, rawVal ->
                val cellRef = "${colLetters(cIdx)}$rowNum"
                val num = rawVal.toDoubleOrNull()
                if (num != null && rIdx > 0 && !rawVal.startsWith("0") || rawVal == "0") {
                    sb.append("""<c r="$cellRef"><v>$rawVal</v></c>""")
                } else {
                    sb.append("""<c r="$cellRef" t="inlineStr"><is><t>${escapeXml(rawVal)}</t></is></c>""")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colLetters(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A' + (n % 26)))
            n = n / 26 - 1
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    fun shareExportedFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}
