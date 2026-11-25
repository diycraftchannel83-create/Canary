package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Aviary
import com.example.data.model.AviaryInvite
import com.example.data.model.BreedingClutch
import com.example.data.model.BreedingPair
import com.example.data.model.BreedingStatus
import com.example.data.model.Canary
import com.example.data.model.CanarySex
import com.example.data.model.ClutchStatus
import com.example.data.model.HealthLog
import com.example.data.model.HealthStatus
import com.example.data.model.InventoryStatus
import com.example.data.model.OwnershipStatus
import com.example.data.model.OwnershipSource
import com.example.data.model.PairStatus
import com.example.data.model.SupplementTask
import com.example.data.model.UserProfile
import com.example.utils.IncubationCalculator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class BreedingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    private val initMutex = Mutex()

    suspend fun ensureUserAndAviaryInitialized(
        displayName: String,
        email: String
    ): Result<String> = initMutex.withLock {
        var path = "users"
        try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                val existingAviaryId = userSnap.getString("activeAviaryId").orEmpty()
                if (existingAviaryId.isNotBlank()) {
                    val existingAviaryOk = try {
                        val avSnap = db.collection("aviaries").document(existingAviaryId).get().await()
                        val members = (avSnap.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                        avSnap.exists() && uid in members
                    } catch (_: Exception) {
                        false
                    }
                    if (existingAviaryOk) {
                        return@withLock Result.success(existingAviaryId)
                    }
                }
            }

            path = "aviaries"
            val memberAviaries = db.collection("aviaries")
                .whereArrayContains("memberUids", uid)
                .limit(1)
                .get()
                .await()
            val existingMemberAviaryId = memberAviaries.documents.firstOrNull()?.id.orEmpty()
            if (existingMemberAviaryId.isNotBlank()) {
                path = userRef.path
                val cleanName = displayName.ifBlank { "Peternak Kenari" }.take(60)
                if (!userSnap.exists()) {
                    userRef.set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to cleanName,
                            "email" to email.take(120),
                            "activeAviaryId" to existingMemberAviaryId,
                            "supplementReminderEnabled" to true,
                            "supplementHour" to 7L,
                            "supplementMinute" to 0L,
                            "incubationReminderEnabled" to true,
                            "healthAlertThreshold" to 25L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    userRef.update(
                        mapOf(
                            "activeAviaryId" to existingMemberAviaryId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                return@withLock Result.success(existingMemberAviaryId)
            }

            val cleanName = displayName.ifBlank { "Peternak Kenari" }.take(60)
            val inviteCode = generateInviteCode()
            val defaultAviaryId = "aviary_${uid.take(16)}"
            val aviaryPayload = mapOf(
                "ownerId" to uid,
                "name" to "Aviary $cleanName".take(60),
                "farmName" to "",
                "farmAddress" to "",
                "farmPhone" to "",
                "inviteCode" to inviteCode,
                "memberUids" to listOf(uid),
                "memberLabels" to listOf(cleanName),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            var resolvedAviaryId = defaultAviaryId
            path = "aviaries/$resolvedAviaryId"
            try {
                db.collection("aviaries").document(resolvedAviaryId).set(aviaryPayload).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                resolvedAviaryId = "aviary_${uid.take(10)}_${System.currentTimeMillis()}"
                path = "aviaries/$resolvedAviaryId"
                db.collection("aviaries").document(resolvedAviaryId).set(aviaryPayload).await()
            }

            path = "aviary_invites/$inviteCode"
            db.collection("aviary_invites").document(inviteCode).set(
                mapOf(
                    "aviaryId" to resolvedAviaryId,
                    "aviaryName" to "Aviary $cleanName".take(60),
                    "ownerId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            path = userRef.path
            if (!userSnap.exists()) {
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "email" to email.take(120),
                        "activeAviaryId" to resolvedAviaryId,
                        "supplementReminderEnabled" to true,
                        "supplementHour" to 7L,
                        "supplementMinute" to 0L,
                        "incubationReminderEnabled" to true,
                        "healthAlertThreshold" to 25L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                userRef.update(
                    mapOf(
                        "activeAviaryId" to resolvedAviaryId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(resolvedAviaryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeUserAviaries(): Flow<List<Aviary>> = flow {
        val uid = requireUserId()
        val path = "aviaries"
        emitAll(
            db.collection("aviaries")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { it.toObjects(Aviary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeCanaries(aviaryId: String): Flow<List<Canary>> = flow {
        val uid = requireUserId()
        if (aviaryId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "aviaries/$aviaryId/canaries"
        emitAll(
            db.collection("aviaries").document(aviaryId).collection("canaries")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Canary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.ringCode }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePairs(aviaryId: String): Flow<List<BreedingPair>> = flow {
        val uid = requireUserId()
        if (aviaryId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "aviaries/$aviaryId/pairs"
        emitAll(
            db.collection("aviaries").document(aviaryId).collection("pairs")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BreedingPair::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.pairedDateEpochMs }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeClutches(aviaryId: String): Flow<List<BreedingClutch>> = flow {
        val uid = requireUserId()
        if (aviaryId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "aviaries/$aviaryId/clutches"
        emitAll(
            db.collection("aviaries").document(aviaryId).collection("clutches")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BreedingClutch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.incubationStartEpochMs ?: it.createdAt?.seconds?.times(1000L) ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeHealthLogs(aviaryId: String): Flow<List<HealthLog>> = flow {
        val uid = requireUserId()
        if (aviaryId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "aviaries/$aviaryId/health_logs"
        emitAll(
            db.collection("aviaries").document(aviaryId).collection("health_logs")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(HealthLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.recordedDateEpochMs }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeSupplementTasks(aviaryId: String): Flow<List<SupplementTask>> = flow {
        val uid = requireUserId()
        if (aviaryId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "aviaries/$aviaryId/supplement_logs"
        emitAll(
            db.collection("aviaries").document(aviaryId).collection("supplement_logs")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupplementTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.timeLabel }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveCanary(
        aviaryId: String,
        memberUids: List<String>,
        existingId: String?,
        existingHasPhoto: Boolean,
        ringCode: String,
        name: String,
        sex: CanarySex,
        breed: String,
        featherColor: String,
        hatchDateEpochMs: Long,
        sireId: String?,
        damId: String?,
        sireRing: String?,
        damRing: String?,
        cageCode: String,
        breedingStatus: BreedingStatus,
        healthStatus: HealthStatus,
        healthScore: Int,
        notes: String,
        lineageNotes: String,
        recordedClutchCount: Int,
        recordedEggCount: Int,
        recordedHatchedCount: Int,
        recordedFledgedCount: Int,
        productionHistoryNotes: String,
        newPhotoBase64: String?
    ): Result<Unit> {
        var docPath = "aviaries/$aviaryId/canaries"
        return try {
            val uid = requireUserId()
            require(aviaryId.isNotBlank()) { "Aviary belum dipilih" }
            require(ringCode.isNotBlank()) { "Kode ring wajib diisi" }
            require(name.isNotBlank()) { "Nama kenari wajib diisi" }
            require(breed.isNotBlank()) { "Ras/strain kenari wajib diisi" }

            val colRef = db.collection("aviaries").document(aviaryId).collection("canaries")
            val effectiveMembers = (memberUids + uid).distinct().take(20)
            val cleanPhoto = newPhotoBase64?.takeIf { it.length >= 100 }?.take(350_000)
            if (existingId.isNullOrBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                val model = Canary(
                    aviaryId = aviaryId,
                    userId = uid,
                    memberUids = effectiveMembers,
                    ringCode = ringCode,
                    name = name,
                    sex = sex.name,
                    breed = breed,
                    featherColor = featherColor,
                    hatchDateEpochMs = hatchDateEpochMs,
                    sireId = sireId,
                    damId = damId,
                    sireRing = sireRing,
                    damRing = damRing,
                    cageCode = cageCode,
                    breedingStatus = breedingStatus.name,
                    healthStatus = healthStatus.name,
                    healthScore = healthScore.toLong(),
                    notes = notes,
                    lineageNotes = lineageNotes,
                    recordedClutchCount = recordedClutchCount.toLong().coerceIn(0L, 500L),
                    recordedEggCount = recordedEggCount.toLong().coerceIn(0L, 2000L),
                    recordedHatchedCount = recordedHatchedCount.toLong().coerceIn(0L, 2000L),
                    recordedFledgedCount = recordedFledgedCount.toLong().coerceIn(0L, 2000L),
                    productionHistoryNotes = productionHistoryNotes,
                    photoBase64 = cleanPhoto,
                    photoCapturedAtEpochMs = if (cleanPhoto != null) System.currentTimeMillis() else null
                )
                docRef.set(model.toCreateMap()).await()
            } else {
                val docRef = colRef.document(existingId)
                docPath = docRef.path
                val updates = mutableMapOf<String, Any>(
                    "ringCode" to ringCode.trim().take(30),
                    "name" to name.trim().take(50),
                    "sex" to sex.name,
                    "breed" to breed.trim().take(40),
                    "featherColor" to featherColor.trim().take(40),
                    "hatchDateEpochMs" to hatchDateEpochMs.coerceAtLeast(0L),
                    "cageCode" to cageCode.trim().take(20),
                    "breedingStatus" to breedingStatus.name,
                    "healthStatus" to healthStatus.name,
                    "healthScore" to healthScore.toLong().coerceIn(0L, 100L),
                    "notes" to notes.trim().take(500),
                    "lineageNotes" to lineageNotes.trim().take(300),
                    "recordedClutchCount" to recordedClutchCount.toLong().coerceIn(0L, 500L),
                    "recordedEggCount" to recordedEggCount.toLong().coerceIn(0L, 2000L),
                    "recordedHatchedCount" to recordedHatchedCount.toLong().coerceIn(0L, 2000L),
                    "recordedFledgedCount" to recordedFledgedCount.toLong().coerceIn(0L, 2000L),
                    "productionHistoryNotes" to productionHistoryNotes.trim().take(500),
                    "memberUids" to effectiveMembers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                sireId?.takeIf { it.isNotBlank() }?.let { updates["sireId"] = it.take(128) }
                damId?.takeIf { it.isNotBlank() }?.let { updates["damId"] = it.take(128) }
                sireRing?.takeIf { it.isNotBlank() }?.let { updates["sireRing"] = it.take(30) }
                damRing?.takeIf { it.isNotBlank() }?.let { updates["damRing"] = it.take(30) }
                // Write-once photo upload: only permitted when no photo has been uploaded yet
                if (!existingHasPhoto && cleanPhoto != null) {
                    updates["photoBase64"] = cleanPhoto
                    updates["photoCapturedAtEpochMs"] = System.currentTimeMillis()
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createInventoryCanary(
        aviaryId: String,
        memberUids: List<String>,
        ringCode: String,
        name: String,
        sex: CanarySex,
        breed: String,
        featherColor: String,
        hatchDateEpochMs: Long,
        cageCode: String,
        inventoryStatus: InventoryStatus,
        ownershipStatus: OwnershipStatus,
        ownershipSource: OwnershipSource,
        acquiredDateEpochMs: Long,
        previousOwnerName: String,
        purchasePrice: Long,
        pedigreeConfidence: String,
        sireRing: String,
        damRing: String,
        lineageNotes: String,
        pedigreeDocumentNotes: String
    ): Result<Unit> {
        val path = "aviaries/$aviaryId/canaries"
        return try {
            val uid = requireUserId()
            require(aviaryId.isNotBlank()) { "Aviary belum dipilih" }
            require(ringCode.isNotBlank()) { "Kode ring wajib diisi" }
            require(name.isNotBlank()) { "Nama burung wajib diisi" }
            require(breed.isNotBlank()) { "Ras/strain wajib diisi" }
            val ref = db.collection("aviaries").document(aviaryId).collection("canaries").document()
            val bird = Canary(
                aviaryId = aviaryId,
                userId = uid,
                memberUids = (memberUids + uid).distinct().take(20),
                ringCode = ringCode.trim(),
                name = name.trim(),
                sex = sex.name,
                breed = breed.trim(),
                featherColor = featherColor.trim(),
                hatchDateEpochMs = hatchDateEpochMs.coerceAtLeast(0L),
                sireRing = sireRing.trim().takeIf { it.isNotBlank() },
                damRing = damRing.trim().takeIf { it.isNotBlank() },
                cageCode = cageCode.trim(),
                inventoryStatus = inventoryStatus.name,
                ownershipStatus = ownershipStatus.name,
                ownershipSource = ownershipSource.name,
                acquiredDateEpochMs = acquiredDateEpochMs.coerceAtLeast(0L),
                previousOwnerName = previousOwnerName.trim(),
                purchasePrice = purchasePrice.coerceAtLeast(0L),
                pedigreeConfidence = pedigreeConfidence,
                lineageNotes = lineageNotes.trim(),
                pedigreeDocumentNotes = pedigreeDocumentNotes.trim()
            )
            ref.set(bird.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCanaryInventoryMetadata(aviaryId: String, canaryId: String, inventoryStatus: String, ownershipStatus: String, ownershipSource: String, acquiredDateEpochMs: Long, previousOwnerName: String, purchasePrice: Long, pedigreeConfidence: String, pedigreeDocumentNotes: String): Result<Unit> {
        val path = "aviaries/$aviaryId/canaries/$canaryId"
        return try {
            require(aviaryId.isNotBlank() && canaryId.isNotBlank())
            requireUserId()
            db.collection("aviaries").document(aviaryId).collection("canaries").document(canaryId).update(
                mapOf(
                    "inventoryStatus" to inventoryStatus.take(30),
                    "ownershipStatus" to ownershipStatus.take(30),
                    "ownershipSource" to ownershipSource.take(30),
                    "acquiredDateEpochMs" to acquiredDateEpochMs.coerceAtLeast(0L),
                    "previousOwnerName" to previousOwnerName.trim().take(100),
                    "purchasePrice" to purchasePrice.coerceAtLeast(0L),
                    "pedigreeConfidence" to pedigreeConfidence.take(20),
                    "pedigreeDocumentNotes" to pedigreeDocumentNotes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCanary(aviaryId: String, canaryId: String): Result<Unit> {
        val docPath = "aviaries/$aviaryId/canaries/$canaryId"
        return try {
            requireUserId()
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createPair(
        aviaryId: String,
        memberUids: List<String>,
        cageCode: String,
        sire: Canary,
        dam: Canary,
        pairedDateEpochMs: Long,
        notes: String
    ): Result<Unit> {
        var docPath = "aviaries/$aviaryId/pairs"
        return try {
            val uid = requireUserId()
            require(cageCode.isNotBlank()) { "Kode kandang wajib diisi" }
            val docRef = db.collection("aviaries").document(aviaryId).collection("pairs").document()
            docPath = docRef.path
            val pair = BreedingPair(
                aviaryId = aviaryId,
                userId = uid,
                memberUids = (memberUids + uid).distinct().take(20),
                cageCode = cageCode,
                sireId = sire.id,
                sireName = sire.name,
                sireRing = sire.ringCode,
                damId = dam.id,
                damName = dam.name,
                damRing = dam.ringCode,
                pairedDateEpochMs = pairedDateEpochMs,
                status = PairStatus.ACTIVE.name,
                notes = notes
            )
            docRef.set(pair.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePairStatus(aviaryId: String, pairId: String, status: PairStatus): Result<Unit> {
        val docPath = "aviaries/$aviaryId/pairs/$pairId"
        return try {
            requireUserId()
            db.document(docPath).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveClutch(
        aviaryId: String,
        memberUids: List<String>,
        existingClutchId: String?,
        pair: BreedingPair,
        clutchNumber: Int,
        eggCount: Int,
        fertileCount: Int,
        hatchedCount: Int,
        fledgedCount: Int,
        incubationStartEpochMs: Long?,
        actualHatchEpochMs: Long?,
        status: ClutchStatus,
        notes: String
    ): Result<String> {
        var docPath = "aviaries/$aviaryId/clutches"
        return try {
            val uid = requireUserId()
            val expectedHatch = incubationStartEpochMs?.let {
                IncubationCalculator.calculate(it).expectedHatchEpochMs
            }
            val colRef = db.collection("aviaries").document(aviaryId).collection("clutches")
            val effectiveMembers = (memberUids + uid).distinct().take(20)
            val targetId: String
            if (existingClutchId.isNullOrBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                targetId = docRef.id
                val model = BreedingClutch(
                    aviaryId = aviaryId,
                    userId = uid,
                    memberUids = effectiveMembers,
                    pairId = pair.id,
                    sireId = pair.sireId,
                    damId = pair.damId,
                    cageCode = pair.cageCode,
                    clutchNumber = clutchNumber.toLong(),
                    eggCount = eggCount.toLong(),
                    fertileCount = fertileCount.toLong(),
                    hatchedCount = hatchedCount.toLong(),
                    fledgedCount = fledgedCount.toLong(),
                    incubationStartEpochMs = incubationStartEpochMs,
                    expectedHatchEpochMs = expectedHatch,
                    actualHatchEpochMs = actualHatchEpochMs,
                    status = status.name,
                    notes = notes
                )
                docRef.set(model.toCreateMap()).await()
            } else {
                targetId = existingClutchId
                val docRef = colRef.document(existingClutchId)
                docPath = docRef.path
                val updates = mutableMapOf<String, Any>(
                    "eggCount" to eggCount.toLong().coerceIn(0L, 12L),
                    "fertileCount" to fertileCount.toLong().coerceIn(0L, 12L),
                    "hatchedCount" to hatchedCount.toLong().coerceIn(0L, 12L),
                    "fledgedCount" to fledgedCount.toLong().coerceIn(0L, 12L),
                    "status" to status.name,
                    "notes" to notes.trim().take(300),
                    "memberUids" to effectiveMembers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                incubationStartEpochMs?.let {
                    updates["incubationStartEpochMs"] = it
                    updates["expectedHatchEpochMs"] = IncubationCalculator.calculate(it).expectedHatchEpochMs
                }
                actualHatchEpochMs?.let {
                    updates["actualHatchEpochMs"] = it
                }
                docRef.update(updates).await()
            }
            Result.success(targetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteClutch(aviaryId: String, clutchId: String): Result<Unit> {
        val docPath = "aviaries/$aviaryId/clutches/$clutchId"
        return try {
            requireUserId()
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordHealthCheck(
        aviaryId: String,
        memberUids: List<String>,
        canary: Canary,
        newScore: Int,
        healthStatus: HealthStatus,
        symptoms: String,
        actionTaken: String,
        dropThreshold: Int
    ): Result<Boolean> {
        var docPath = "aviaries/$aviaryId/health_logs"
        return try {
            val uid = requireUserId()
            val prevScore = canary.healthScore.toInt()
            val drop = prevScore - newScore
            val alertTriggered = drop >= dropThreshold || newScore < 55 || healthStatus == HealthStatus.CRITICAL

            val effectiveMembers = (memberUids + uid).distinct().take(20)
            val logRef = db.collection("aviaries").document(aviaryId).collection("health_logs").document()
            docPath = logRef.path
            val log = HealthLog(
                aviaryId = aviaryId,
                userId = uid,
                memberUids = effectiveMembers,
                canaryId = canary.id,
                canaryName = canary.name,
                canaryRing = canary.ringCode,
                healthScore = newScore.toLong(),
                previousScore = prevScore.toLong(),
                healthStatus = healthStatus.name,
                symptoms = symptoms,
                actionTaken = actionTaken,
                recordedDateEpochMs = System.currentTimeMillis(),
                alertTriggered = alertTriggered
            )
            logRef.set(log.toCreateMap()).await()

            val canaryRef = db.collection("aviaries").document(aviaryId).collection("canaries").document(canary.id)
            canaryRef.update(
                mapOf(
                    "healthScore" to newScore.toLong().coerceIn(0L, 100L),
                    "healthStatus" to healthStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(alertTriggered)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addSupplementTask(
        aviaryId: String,
        memberUids: List<String>,
        title: String,
        dosage: String,
        targetGroup: String,
        timeLabel: String
    ): Result<Unit> {
        var docPath = "aviaries/$aviaryId/supplement_logs"
        return try {
            val uid = requireUserId()
            require(title.isNotBlank()) { "Nama suplemen wajib diisi" }
            val docRef = db.collection("aviaries").document(aviaryId).collection("supplement_logs").document()
            docPath = docRef.path
            val task = SupplementTask(
                aviaryId = aviaryId,
                userId = uid,
                memberUids = (memberUids + uid).distinct().take(20),
                title = title,
                dosage = dosage,
                targetGroup = targetGroup.ifBlank { "Semua Indukan" },
                timeLabel = timeLabel.ifBlank { "07:00" },
                lastCompletedDate = "",
                active = true
            )
            docRef.set(task.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSupplementDoneToday(
        aviaryId: String,
        taskId: String,
        currentlyDoneToday: Boolean
    ): Result<Unit> {
        val docPath = "aviaries/$aviaryId/supplement_logs/$taskId"
        return try {
            requireUserId()
            val nextDate = if (currentlyDoneToday) "" else IncubationCalculator.todayIsoDate()
            db.document(docPath).update(
                mapOf(
                    "lastCompletedDate" to nextDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSupplementTask(aviaryId: String, taskId: String): Result<Unit> {
        val docPath = "aviaries/$aviaryId/supplement_logs/$taskId"
        return try {
            requireUserId()
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(
        supplementEnabled: Boolean,
        supplementHour: Int,
        supplementMinute: Int,
        incubationEnabled: Boolean,
        healthAlertThreshold: Int
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "supplementReminderEnabled" to supplementEnabled,
                    "supplementHour" to supplementHour.toLong().coerceIn(0L, 23L),
                    "supplementMinute" to supplementMinute.toLong().coerceIn(0L, 59L),
                    "incubationReminderEnabled" to incubationEnabled,
                    "healthAlertThreshold" to healthAlertThreshold.toLong().coerceIn(5L, 60L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateAviaryName(aviaryId: String, newName: String): Result<Unit> {
        val docPath = "aviaries/$aviaryId"
        return try {
            requireUserId()
            require(newName.trim().length >= 2) { "Nama aviary minimal 2 karakter" }
            db.document(docPath).update(
                mapOf(
                    "name" to newName.trim().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun uploadFarmLogo(aviaryId: String, imageUri: Uri, mimeType: String): Result<String> {
        val docPath = "aviaries/$aviaryId"
        return try {
            val uid = requireUserId()
            val aviaryRef = db.collection("aviaries").document(aviaryId)
            val aviarySnapshot = aviaryRef.get().await()
            val members = (aviarySnapshot.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
            require(aviarySnapshot.exists() && uid in members) {
                "Anda bukan anggota farm ini."
            }
            val storageRef = FirebaseStorage.getInstance().reference
                .child("aviaries/$aviaryId/farm_logo/logo")
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType.takeIf { it.startsWith("image/") } ?: "image/jpeg")
                .build()
            storageRef.putFile(imageUri, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            try {
                aviaryRef.update(
                    mapOf(
                        "farmLogoUrl" to downloadUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                try { storageRef.delete().await() } catch (_: Exception) {}
                throw e
            }
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFarmLogo(aviaryId: String): Result<Unit> {
        val docPath = "aviaries/$aviaryId"
        return try {
            val uid = requireUserId()
            val aviaryRef = db.collection("aviaries").document(aviaryId)
            val aviarySnapshot = aviaryRef.get().await()
            val members = (aviarySnapshot.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
            require(aviarySnapshot.exists() && uid in members) {
                "Anda bukan anggota farm ini."
            }
            val storageRef = FirebaseStorage.getInstance().reference
                .child("aviaries/$aviaryId/farm_logo/logo")
            try { storageRef.delete().await() } catch (_: Exception) { /* tolerate missing logo object */ }
            aviaryRef.update(
                mapOf(
                    "farmLogoUrl" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateFarmSettings(
        aviaryId: String,
        farmName: String,
        farmAddress: String,
        farmPhone: String
    ): Result<Unit> {
        val docPath = "aviaries/$aviaryId"
        return try {
            requireUserId()
            require(farmName.trim().length >= 2) { "Nama farm minimal 2 karakter" }
            db.document(docPath).update(
                mapOf(
                    "farmName" to farmName.trim().take(80),
                    "farmAddress" to farmAddress.trim().take(200),
                    "farmPhone" to farmPhone.trim().take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun switchActiveAviary(aviaryId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "activeAviaryId" to aviaryId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinAviaryByInviteCode(rawCode: String, memberDisplayName: String): Result<String> {
        val code = rawCode.trim().uppercase().filter { it.isLetterOrDigit() }
        var docPath = "aviary_invites/$code"
        return try {
            val uid = requireUserId()
            require(code.length in 4..16) { "Kode undangan tidak valid" }
            val inviteSnap = db.collection("aviary_invites").document(code).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("Kode undangan '$code' tidak ditemukan."))
            }
            val invite = inviteSnap.toObject(AviaryInvite::class.java)
                ?: return Result.failure(IllegalStateException("Data undangan tidak valid."))

            val aviaryRef = db.collection("aviaries").document(invite.aviaryId)
            docPath = aviaryRef.path
            val label = memberDisplayName.ifBlank { "Anggota Tim" }.take(40)
            aviaryRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "memberLabels" to FieldValue.arrayUnion(label),
                    "inviteCode" to code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "activeAviaryId" to invite.aviaryId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(invite.aviaryName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncMemberUidsAcrossSubcollections(aviaryId: String, memberUids: List<String>): Result<Unit> {
        var docPath = "aviaries/$aviaryId"
        return try {
            val uid = requireUserId()
            if (aviaryId.isBlank() || memberUids.isEmpty()) return Result.success(Unit)
            val subcollections = listOf("canaries", "pairs", "clutches", "health_logs", "supplement_logs")
            val targetMembers = memberUids.distinct().take(20)

            for (sub in subcollections) {
                docPath = "aviaries/$aviaryId/$sub"
                val snap = db.collection("aviaries").document(aviaryId).collection(sub)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                val docsToUpdate = snap.documents.filter { doc ->
                    val existing = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    !existing.containsAll(targetMembers) || existing.size != targetMembers.size
                }
                if (docsToUpdate.isNotEmpty()) {
                    docsToUpdate.chunked(400).forEach { chunk ->
                        val batch = db.batch()
                        chunk.forEach { doc ->
                            batch.update(
                                doc.reference,
                                mapOf(
                                    "memberUids" to targetMembers,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                        batch.commit().await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            append("KNR")
            repeat(5) {
                append(chars[Random.nextInt(chars.length)])
            }
        }
    }
}
