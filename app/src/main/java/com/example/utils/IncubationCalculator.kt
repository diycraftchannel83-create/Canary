package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.example.data.model.Canary

object IncubationCalculator {
    const val INCUBATION_DAYS = 21L
    const val DAY_MS = 86_400_000L

    data class IncubationTimeline(
        val startEpochMs: Long,
        val expectedHatchEpochMs: Long,
        val candlingDay7EpochMs: Long,
        val embryoCheckDay14EpochMs: Long,
        val hatchPrepDay20EpochMs: Long,
        val elapsedDays: Int,
        val remainingDays: Int,
        val progress: Float,
        val currentPhaseLabel: String
    )

    fun calculate(startEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): IncubationTimeline {
        val startMidnight = normalizeToStartOfDay(startEpochMs)
        val nowMidnight = normalizeToStartOfDay(nowEpochMs)
        val expected = startMidnight + INCUBATION_DAYS * DAY_MS
        val day7 = startMidnight + 7L * DAY_MS
        val day14 = startMidnight + 14L * DAY_MS
        val day20 = startMidnight + 20L * DAY_MS

        val rawElapsed = TimeUnit.MILLISECONDS.toDays(nowMidnight - startMidnight).toInt()
        val elapsed = rawElapsed.coerceIn(0, INCUBATION_DAYS.toInt())
        val remaining = (INCUBATION_DAYS.toInt() - rawElapsed).coerceAtLeast(0)
        val progress = (rawElapsed.toFloat() / INCUBATION_DAYS.toFloat()).coerceIn(0f, 1f)

        val phase = when {
            rawElapsed < 0 -> "Terjadwal Mengeram"
            rawElapsed < 7 -> "Fase Awal (Hari ke-${rawElapsed + 1} / 21)"
            rawElapsed == 7 -> "Jadwal Teropong Telur (Hari ke-7)"
            rawElapsed < 14 -> "Perkembangan Embrio (Hari ke-$rawElapsed / 21)"
            rawElapsed == 14 -> "Cek Embrio Tahap 2 (Hari ke-14)"
            rawElapsed < 20 -> "Fase Pematangan (Hari ke-$rawElapsed / 21)"
            rawElapsed == 20 -> "H-1 Persiapan Menetas (Hari ke-20)"
            rawElapsed == 21 -> "Estimasi Hari Menetas (Hari ke-21)!"
            else -> "Melewati H+21 (${rawElapsed - 21} hari)"
        }

        return IncubationTimeline(
            startEpochMs = startMidnight,
            expectedHatchEpochMs = expected,
            candlingDay7EpochMs = day7,
            embryoCheckDay14EpochMs = day14,
            hatchPrepDay20EpochMs = day20,
            elapsedDays = elapsed,
            remainingDays = remaining,
            progress = progress,
            currentPhaseLabel = phase
        )
    }

    fun normalizeToStartOfDay(epochMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatDate(epochMs: Long): String {
        if (epochMs <= 0L) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(epochMs))
    }

    fun formatShortDate(epochMs: Long): String {
        if (epochMs <= 0L) return "-"
        val sdf = SimpleDateFormat("dd/MM/yy", Locale("id", "ID"))
        return sdf.format(Date(epochMs))
    }

    fun todayIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}

object GenealogyAnalyzer {
    data class PedigreeNode(
        val canary: Canary?,
        val ringFallback: String?,
        val roleLabel: String
    )

    data class PedigreeTree(
        val subject: Canary,
        val sire: Canary?,
        val dam: Canary?,
        val paternalGrandsire: Canary?,
        val paternalGranddam: Canary?,
        val maternalGrandsire: Canary?,
        val maternalGranddam: Canary?,
        val offspring: List<Canary>
    )

    fun buildPedigreeTree(subject: Canary, allCanaries: List<Canary>): PedigreeTree {
        val byId = allCanaries.associateBy { it.id }
        val sire = subject.sireId?.let { byId[it] }
        val dam = subject.damId?.let { byId[it] }
        val pGrandsire = sire?.sireId?.let { byId[it] }
        val pGranddam = sire?.damId?.let { byId[it] }
        val mGrandsire = dam?.sireId?.let { byId[it] }
        val mGranddam = dam?.damId?.let { byId[it] }
        val offspring = allCanaries.filter { it.sireId == subject.id || it.damId == subject.id }

        return PedigreeTree(
            subject = subject,
            sire = sire,
            dam = dam,
            paternalGrandsire = pGrandsire,
            paternalGranddam = pGranddam,
            maternalGrandsire = mGrandsire,
            maternalGranddam = mGranddam,
            offspring = offspring
        )
    }

    /**
     * Checks up to 3 generations of ancestors to warn breeders about potential inbreeding.
     */
    fun findSharedAncestors(
        sire: Canary,
        dam: Canary,
        allCanaries: List<Canary>
    ): List<Canary> {
        val byId = allCanaries.associateBy { it.id }
        val sireAncestors = collectAncestors(sire, byId, depth = 3)
        val damAncestors = collectAncestors(dam, byId, depth = 3)
        val sharedIds = sireAncestors.intersect(damAncestors)
        return sharedIds.mapNotNull { byId[it] }
    }

    private fun collectAncestors(
        bird: Canary,
        byId: Map<String, Canary>,
        depth: Int
    ): Set<String> {
        if (depth <= 0) return emptySet()
        val result = mutableSetOf<String>()
        bird.sireId?.let { sId ->
            result.add(sId)
            byId[sId]?.let { result.addAll(collectAncestors(it, byId, depth - 1)) }
        }
        bird.damId?.let { dId ->
            result.add(dId)
            byId[dId]?.let { result.addAll(collectAncestors(it, byId, depth - 1)) }
        }
        return result
    }
}
