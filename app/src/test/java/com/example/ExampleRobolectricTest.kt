package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Canary
import com.example.data.model.CanarySex
import com.example.export.MonthlyProductivityStat
import com.example.export.ReportExporter
import com.example.utils.GenealogyAnalyzer
import com.example.utils.IncubationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name and package id match rebranded Kenari Tasikmalaya`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kenari Tasikmalaya", appName)
        assertEquals("com.winsmediatama.kenaritasik", context.packageName)
    }

    @Test
    fun `admob unit ids match requested App Open and Rewarded Interstitial IDs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(
            "ca-app-pub-1254774552944426/9533896500",
            context.getString(R.string.admob_app_open_unit_id)
        )
        assertEquals(
            "ca-app-pub-1254774552944426/5741489179",
            context.getString(R.string.admob_rewarded_interstitial_unit_id)
        )
    }

    @Test
    fun `incubation calculator computes 21 day milestones accurately`() {
        val day0 = IncubationCalculator.normalizeToStartOfDay(1_732_000_000_000L)
        val day10 = day0 + 10L * IncubationCalculator.DAY_MS
        val timeline = IncubationCalculator.calculate(day0, nowEpochMs = day10)

        assertEquals(day0 + 21L * IncubationCalculator.DAY_MS, timeline.expectedHatchEpochMs)
        assertEquals(day0 + 7L * IncubationCalculator.DAY_MS, timeline.candlingDay7EpochMs)
        assertEquals(10, timeline.elapsedDays)
        assertEquals(11, timeline.remainingDays)
    }

    @Test
    fun `genealogy analyzer detects shared ancestors and builds tree`() {
        val grandSire = Canary(id = "g1", ringCode = "R-G1", name = "Grand", sex = CanarySex.MALE.name)
        val sire = Canary(id = "s1", ringCode = "R-S1", name = "Sire", sex = CanarySex.MALE.name, sireId = "g1")
        val dam = Canary(id = "d1", ringCode = "R-D1", name = "Dam", sex = CanarySex.FEMALE.name, sireId = "g1")
        val child = Canary(id = "c1", ringCode = "R-C1", name = "Child", sex = CanarySex.MALE.name, sireId = "s1", damId = "d1")
        val all = listOf(grandSire, sire, dam, child)

        val shared = GenealogyAnalyzer.findSharedAncestors(sire, dam, all)
        assertEquals(1, shared.size)
        assertEquals("g1", shared.first().id)

        val tree = GenealogyAnalyzer.buildPedigreeTree(child, all)
        assertEquals("s1", tree.sire?.id)
        assertEquals("d1", tree.dam?.id)
        assertEquals("g1", tree.paternalGrandsire?.id)
        assertEquals("g1", tree.maternalGrandsire?.id)
    }

    @Test
    fun `excel exporter produces valid multi-sheet xlsx zip`() {
        val out = ByteArrayOutputStream()
        ReportExporter.writeXlsxToStream(
            outputStream = out,
            aviaryName = "Test Aviary",
            canaries = listOf(Canary(id = "1", ringCode = "KNR-01", name = "Gold", breed = "AF")),
            pairs = emptyList(),
            clutches = emptyList(),
            monthlyStats = listOf(MonthlyProductivityStat("2026-11", "Nov 26", 4, 3, 3, 2)),
            healthLogs = emptyList()
        )
        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                entries.add(e.name)
                e = zis.nextEntry
            }
        }
        assertTrue(entries.contains("xl/workbook.xml"))
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"))
        assertTrue(entries.contains("xl/worksheets/sheet4.xml"))
    }

    @Test
    fun `canary model preserves uploaded photo, lineage, and production history fields in create map`() {
        val samplePhoto = "A".repeat(140)
        val bird = Canary(
            id = "c1",
            aviaryId = "av1",
            userId = "u1",
            memberUids = listOf("u1"),
            ringCode = "KNR-01",
            name = "Gold",
            breed = "AF",
            lineageNotes = "Trah juara F1 seri impor",
            recordedClutchCount = 4L,
            recordedEggCount = 16L,
            recordedHatchedCount = 13L,
            recordedFledgedCount = 11L,
            productionHistoryNotes = "Meloloh mandiri stabil",
            photoBase64 = samplePhoto,
            photoCapturedAtEpochMs = 1_732_500_000_000L
        )
        assertTrue(bird.hasUploadedPhoto)
        val map = bird.toCreateMap()
        assertEquals("Trah juara F1 seri impor", map["lineageNotes"])
        assertEquals(4L, map["recordedClutchCount"])
        assertEquals(16L, map["recordedEggCount"])
        assertEquals(13L, map["recordedHatchedCount"])
        assertEquals(11L, map["recordedFledgedCount"])
        assertEquals("Meloloh mandiri stabil", map["productionHistoryNotes"])
        assertEquals(samplePhoto, map["photoBase64"])
        assertEquals(1_732_500_000_000L, map["photoCapturedAtEpochMs"])
    }
}
