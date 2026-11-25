package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CanarySex(val labelId: String) {
    MALE("Jantan"),
    FEMALE("Betina");

    companion object {
        fun fromRaw(raw: String?): CanarySex =
            entries.firstOrNull { it.name == raw } ?: MALE
    }
}

enum class BreedingStatus(val labelId: String) {
    ACTIVE("Aktif Breeding"),
    RESTING("Istirahat / Mabung"),
    RETIRED("Pensiun");

    companion object {
        fun fromRaw(raw: String?): BreedingStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class HealthStatus(val labelId: String) {
    HEALTHY("Sehat"),
    MONITORING("Pantau"),
    CRITICAL("Kritis");

    companion object {
        fun fromRaw(raw: String?): HealthStatus =
            entries.firstOrNull { it.name == raw } ?: HEALTHY
    }
}

enum class PairStatus(val labelId: String) {
    ACTIVE("Penjodohan Aktif"),
    RESTING("Istirahat"),
    ARCHIVED("Selesai");

    companion object {
        fun fromRaw(raw: String?): PairStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class ClutchStatus(val labelId: String) {
    LAYING("Bertelur"),
    INCUBATING("Mengeram"),
    HATCHED("Menetas"),
    COMPLETED("Selesai Sapih"),
    FAILED("Gagal / Zonk");

    companion object {
        fun fromRaw(raw: String?): ClutchStatus =
            entries.firstOrNull { it.name == raw } ?: LAYING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val activeAviaryId: String = "",
    val supplementReminderEnabled: Boolean = true,
    val supplementHour: Long = 7L,
    val supplementMinute: Long = 0L,
    val incubationReminderEnabled: Boolean = true,
    val healthAlertThreshold: Long = 25L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Aviary(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val farmName: String = "",
    val farmAddress: String = "",
    val farmPhone: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val memberLabels: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AviaryInvite(
    @DocumentId val id: String = "",
    val aviaryId: String = "",
    val aviaryName: String = "",
    val ownerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Canary(
    @DocumentId val id: String = "",
    val aviaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val ringCode: String = "",
    val name: String = "",
    val sex: String = CanarySex.MALE.name,
    val breed: String = "",
    val featherColor: String = "",
    val hatchDateEpochMs: Long = 0L,
    val sireId: String? = null,
    val damId: String? = null,
    val sireRing: String? = null,
    val damRing: String? = null,
    val cageCode: String = "",
    val breedingStatus: String = BreedingStatus.ACTIVE.name,
    val healthStatus: String = HealthStatus.HEALTHY.name,
    val healthScore: Long = 90L,
    val notes: String = "",
    val lineageNotes: String = "",
    val recordedClutchCount: Long = 0L,
    val recordedEggCount: Long = 0L,
    val recordedHatchedCount: Long = 0L,
    val recordedFledgedCount: Long = 0L,
    val productionHistoryNotes: String = "",
    val photoBase64: String? = null,
    val photoCapturedAtEpochMs: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val sexEnum: CanarySex get() = CanarySex.fromRaw(sex)
    val breedingStatusEnum: BreedingStatus get() = BreedingStatus.fromRaw(breedingStatus)
    val healthStatusEnum: HealthStatus get() = HealthStatus.fromRaw(healthStatus)
    val hasUploadedPhoto: Boolean get() = !photoBase64.isNullOrBlank()

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "aviaryId" to aviaryId,
            "userId" to userId,
            "memberUids" to memberUids,
            "ringCode" to ringCode.trim().take(30),
            "name" to name.trim().take(50),
            "sex" to sexEnum.name,
            "breed" to breed.trim().take(40),
            "featherColor" to featherColor.trim().take(40),
            "hatchDateEpochMs" to hatchDateEpochMs.coerceAtLeast(0L),
            "sireId" to sireId?.takeIf { it.isNotBlank() }?.take(128),
            "damId" to damId?.takeIf { it.isNotBlank() }?.take(128),
            "sireRing" to sireRing?.takeIf { it.isNotBlank() }?.take(30),
            "damRing" to damRing?.takeIf { it.isNotBlank() }?.take(30),
            "cageCode" to cageCode.trim().take(20),
            "breedingStatus" to breedingStatusEnum.name,
            "healthStatus" to healthStatusEnum.name,
            "healthScore" to healthScore.coerceIn(0L, 100L),
            "notes" to notes.trim().take(500),
            "lineageNotes" to lineageNotes.trim().take(300),
            "recordedClutchCount" to recordedClutchCount.coerceIn(0L, 500L),
            "recordedEggCount" to recordedEggCount.coerceIn(0L, 2000L),
            "recordedHatchedCount" to recordedHatchedCount.coerceIn(0L, 2000L),
            "recordedFledgedCount" to recordedFledgedCount.coerceIn(0L, 2000L),
            "productionHistoryNotes" to productionHistoryNotes.trim().take(500),
            "photoBase64" to photoBase64?.takeIf { it.length >= 100 }?.take(350_000),
            "photoCapturedAtEpochMs" to if (!photoBase64.isNullOrBlank()) photoCapturedAtEpochMs?.coerceAtLeast(0L) else null,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class BreedingPair(
    @DocumentId val id: String = "",
    val aviaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val cageCode: String = "",
    val sireId: String = "",
    val sireName: String = "",
    val sireRing: String = "",
    val damId: String = "",
    val damName: String = "",
    val damRing: String = "",
    val pairedDateEpochMs: Long = 0L,
    val status: String = PairStatus.ACTIVE.name,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: PairStatus get() = PairStatus.fromRaw(status)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "aviaryId" to aviaryId,
        "userId" to userId,
        "memberUids" to memberUids,
        "cageCode" to cageCode.trim().take(20),
        "sireId" to sireId.take(128),
        "sireName" to sireName.trim().take(50),
        "sireRing" to sireRing.trim().take(30),
        "damId" to damId.take(128),
        "damName" to damName.trim().take(50),
        "damRing" to damRing.trim().take(30),
        "pairedDateEpochMs" to pairedDateEpochMs.coerceAtLeast(0L),
        "status" to statusEnum.name,
        "notes" to notes.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class BreedingClutch(
    @DocumentId val id: String = "",
    val aviaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val pairId: String = "",
    val sireId: String = "",
    val damId: String = "",
    val cageCode: String = "",
    val clutchNumber: Long = 1L,
    val eggCount: Long = 0L,
    val fertileCount: Long = 0L,
    val hatchedCount: Long = 0L,
    val fledgedCount: Long = 0L,
    val incubationStartEpochMs: Long? = null,
    val expectedHatchEpochMs: Long? = null,
    val actualHatchEpochMs: Long? = null,
    val status: String = ClutchStatus.LAYING.name,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: ClutchStatus get() = ClutchStatus.fromRaw(status)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "aviaryId" to aviaryId,
            "userId" to userId,
            "memberUids" to memberUids,
            "pairId" to pairId.take(128),
            "sireId" to sireId.take(128),
            "damId" to damId.take(128),
            "cageCode" to cageCode.trim().take(20),
            "clutchNumber" to clutchNumber.coerceIn(1L, 50L),
            "eggCount" to eggCount.coerceIn(0L, 12L),
            "fertileCount" to fertileCount.coerceIn(0L, 12L),
            "hatchedCount" to hatchedCount.coerceIn(0L, 12L),
            "fledgedCount" to fledgedCount.coerceIn(0L, 12L),
            "incubationStartEpochMs" to incubationStartEpochMs?.coerceAtLeast(0L),
            "expectedHatchEpochMs" to expectedHatchEpochMs?.coerceAtLeast(0L),
            "actualHatchEpochMs" to actualHatchEpochMs?.coerceAtLeast(0L),
            "status" to statusEnum.name,
            "notes" to notes.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class HealthLog(
    @DocumentId val id: String = "",
    val aviaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val canaryId: String = "",
    val canaryName: String = "",
    val canaryRing: String = "",
    val healthScore: Long = 90L,
    val previousScore: Long = 90L,
    val healthStatus: String = HealthStatus.HEALTHY.name,
    val symptoms: String = "",
    val actionTaken: String = "",
    val recordedDateEpochMs: Long = 0L,
    val alertTriggered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val healthStatusEnum: HealthStatus get() = HealthStatus.fromRaw(healthStatus)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "aviaryId" to aviaryId,
        "userId" to userId,
        "memberUids" to memberUids,
        "canaryId" to canaryId.take(128),
        "canaryName" to canaryName.trim().take(50),
        "canaryRing" to canaryRing.trim().take(30),
        "healthScore" to healthScore.coerceIn(0L, 100L),
        "previousScore" to previousScore.coerceIn(0L, 100L),
        "healthStatus" to healthStatusEnum.name,
        "symptoms" to symptoms.trim().take(200),
        "actionTaken" to actionTaken.trim().take(200),
        "recordedDateEpochMs" to recordedDateEpochMs.coerceAtLeast(0L),
        "alertTriggered" to alertTriggered,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SupplementTask(
    @DocumentId val id: String = "",
    val aviaryId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val dosage: String = "",
    val targetGroup: String = "",
    val timeLabel: String = "07:00",
    val lastCompletedDate: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "aviaryId" to aviaryId,
        "userId" to userId,
        "memberUids" to memberUids,
        "title" to title.trim().take(80),
        "dosage" to dosage.trim().take(60),
        "targetGroup" to targetGroup.trim().take(60),
        "timeLabel" to timeLabel.trim().take(10),
        "lastCompletedDate" to lastCompletedDate.trim().take(12),
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
