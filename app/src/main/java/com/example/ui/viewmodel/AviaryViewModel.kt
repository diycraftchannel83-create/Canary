package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobManager
import com.example.data.model.Aviary
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
import com.example.data.repository.BreedingRepository
import com.example.export.MonthlyProductivityStat
import com.example.export.ReportExporter
import com.example.notifications.BreedingNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class AviaryDashboardData(
    val profile: UserProfile?,
    val aviaries: List<Aviary>,
    val activeAviary: Aviary?,
    val canaries: List<Canary>,
    val pairs: List<BreedingPair>,
    val clutches: List<BreedingClutch>,
    val healthLogs: List<HealthLog>,
    val supplements: List<SupplementTask>
)

@OptIn(ExperimentalCoroutinesApi::class)
class AviaryViewModel(
    private val repository: BreedingRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedCanaryFilterId = MutableStateFlow<String?>(null)
    val selectedCanaryFilterId: StateFlow<String?> = _selectedCanaryFilterId.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser
        viewModelScope.launch {
            repository.ensureUserAndAviaryInitialized(
                displayName = currentUser?.displayName ?: "Peternak",
                email = currentUser?.email ?: ""
            ).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Gagal inisialisasi aviary cloud."
            }
        }
    }

    private val profileFlow = repository.observeUserProfile()
    private val aviariesFlow = repository.observeUserAviaries()

    private val activeAviaryFlow = combine(profileFlow, aviariesFlow) { profile, aviaries ->
        val activeId = profile?.activeAviaryId
        aviaries.firstOrNull { it.id == activeId } ?: aviaries.firstOrNull()
    }.distinctUntilChanged { old, new ->
        old?.id == new?.id &&
            old?.memberUids == new?.memberUids &&
            old?.name == new?.name &&
            old?.farmName == new?.farmName &&
            old?.farmAddress == new?.farmAddress &&
            old?.farmPhone == new?.farmPhone &&
            old?.farmLogoUrl == new?.farmLogoUrl
    }

    val dashboardState: StateFlow<UiState<AviaryDashboardData>> = activeAviaryFlow
        .flatMapLatest { activeAviary ->
            val aviaryId = activeAviary?.id.orEmpty()
            if (aviaryId.isBlank()) {
                combine(profileFlow, aviariesFlow) { profile, aviaries ->
                    AviaryDashboardData(
                        profile = profile,
                        aviaries = aviaries,
                        activeAviary = null,
                        canaries = emptyList(),
                        pairs = emptyList(),
                        clutches = emptyList(),
                        healthLogs = emptyList(),
                        supplements = emptyList()
                    )
                }
            } else {
                // Keep subcollection memberUids synchronized when new team members join
                viewModelScope.launch {
                    repository.syncMemberUidsAcrossSubcollections(aviaryId, activeAviary?.memberUids.orEmpty())
                }
                combine(
                    profileFlow,
                    aviariesFlow,
                    repository.observeCanaries(aviaryId),
                    repository.observePairs(aviaryId),
                    repository.observeClutches(aviaryId),
                    repository.observeHealthLogs(aviaryId),
                    repository.observeSupplementTasks(aviaryId)
                ) { values ->
                    val profile = values[0] as UserProfile?
                    @Suppress("UNCHECKED_CAST")
                    val aviaries = values[1] as List<Aviary>
                    @Suppress("UNCHECKED_CAST")
                    val canaries = values[2] as List<Canary>
                    @Suppress("UNCHECKED_CAST")
                    val pairs = values[3] as List<BreedingPair>
                    @Suppress("UNCHECKED_CAST")
                    val clutches = values[4] as List<BreedingClutch>
                    @Suppress("UNCHECKED_CAST")
                    val healthLogs = values[5] as List<HealthLog>
                    @Suppress("UNCHECKED_CAST")
                    val supplements = values[6] as List<SupplementTask>

                    AviaryDashboardData(
                        profile = profile,
                        aviaries = aviaries,
                        activeAviary = aviaries.firstOrNull { it.id == aviaryId } ?: activeAviary,
                        canaries = canaries,
                        pairs = pairs,
                        clutches = clutches,
                        healthLogs = healthLogs,
                        supplements = supplements
                    )
                }
            }
        }
        .map<AviaryDashboardData, UiState<AviaryDashboardData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing aviary dashboard", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat data aviary."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun setCanaryFilter(canaryId: String?) {
        _selectedCanaryFilterId.value = canaryId
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun computeMonthlyProductivity(
        clutches: List<BreedingClutch>,
        filterCanaryId: String?
    ): List<MonthlyProductivityStat> {
        val filtered = if (filterCanaryId.isNullOrBlank()) {
            clutches
        } else {
            clutches.filter { it.sireId == filterCanaryId || it.damId == filterCanaryId }
        }

        val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val labelFmt = SimpleDateFormat("MMM yy", Locale("id", "ID"))

        val monthSlots = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -5)
        }
        repeat(6) {
            val d = cal.time
            monthSlots.add(keyFmt.format(d) to labelFmt.format(d))
            cal.add(Calendar.MONTH, 1)
        }

        val grouped = filtered.groupBy { clutch ->
            val refMs = clutch.incubationStartEpochMs
                ?: clutch.actualHatchEpochMs
                ?: clutch.createdAt?.toDate()?.time
                ?: System.currentTimeMillis()
            keyFmt.format(Date(refMs))
        }

        return monthSlots.map { (key, label) ->
            val items = grouped[key].orEmpty()
            MonthlyProductivityStat(
                monthKey = key,
                monthLabel = label,
                eggs = items.sumOf { it.eggCount.toInt() },
                fertile = items.sumOf { it.fertileCount.toInt() },
                hatched = items.sumOf { it.hatchedCount.toInt() },
                fledged = items.sumOf { it.fledgedCount.toInt() }
            )
        }
    }

    fun saveCanary(
        aviary: Aviary,
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
        newPhotoBase64: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCanary(
                aviaryId = aviary.id,
                memberUids = aviary.memberUids,
                existingId = existingId,
                existingHasPhoto = existingHasPhoto,
                ringCode = ringCode,
                name = name,
                sex = sex,
                breed = breed,
                featherColor = featherColor,
                hatchDateEpochMs = hatchDateEpochMs,
                sireId = sireId,
                damId = damId,
                sireRing = sireRing,
                damRing = damRing,
                cageCode = cageCode,
                breedingStatus = breedingStatus,
                healthStatus = healthStatus,
                healthScore = healthScore,
                notes = notes,
                lineageNotes = lineageNotes,
                recordedClutchCount = recordedClutchCount,
                recordedEggCount = recordedEggCount,
                recordedHatchedCount = recordedHatchedCount,
                recordedFledgedCount = recordedFledgedCount,
                productionHistoryNotes = productionHistoryNotes,
                newPhotoBase64 = newPhotoBase64
            )
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = if (existingId == null) "Indukan berhasil ditambahkan ke Firestore." else "Data indukan diperbarui."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal menyimpan data indukan."
            }
        }
    }

    fun createInventoryCanary(aviary: Aviary, ringCode: String, name: String, sex: CanarySex, breed: String, featherColor: String, hatchDateEpochMs: Long, cageCode: String, inventoryStatus: InventoryStatus, ownershipStatus: OwnershipStatus, ownershipSource: OwnershipSource, acquiredDateEpochMs: Long, previousOwnerName: String, purchasePrice: Long, pedigreeConfidence: String, sireRing: String, damRing: String, lineageNotes: String, pedigreeDocumentNotes: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createInventoryCanary(aviary.id, aviary.memberUids, ringCode, name, sex, breed, featherColor, hatchDateEpochMs, cageCode, inventoryStatus, ownershipStatus, ownershipSource, acquiredDateEpochMs, previousOwnerName, purchasePrice, pedigreeConfidence, sireRing, damRing, lineageNotes, pedigreeDocumentNotes)
            _isBusy.value = false
            result.onSuccess { _actionMessage.value = "Burung baru berhasil ditambahkan ke inventaris." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal menambahkan burung." }
        }
    }

    fun updateInventoryMetadata(aviaryId: String, canaryId: String, inventoryStatus: String, ownershipStatus: String, ownershipSource: String, acquiredDateEpochMs: Long, previousOwnerName: String, purchasePrice: Long, pedigreeConfidence: String, pedigreeDocumentNotes: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateCanaryInventoryMetadata(aviaryId, canaryId, inventoryStatus, ownershipStatus, ownershipSource, acquiredDateEpochMs, previousOwnerName, purchasePrice, pedigreeConfidence, pedigreeDocumentNotes)
            _isBusy.value = false
            result.onSuccess { _actionMessage.value = "Metadata inventaris dan silsilah diperbarui." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal memperbarui metadata inventaris." }
        }
    }

    fun deleteCanary(aviaryId: String, canaryId: String) {
        viewModelScope.launch {
            repository.deleteCanary(aviaryId, canaryId)
                .onSuccess { _actionMessage.value = "Indukan dihapus." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal menghapus indukan." }
        }
    }

    fun createPair(
        aviary: Aviary,
        cageCode: String,
        sire: Canary,
        dam: Canary,
        pairedDateEpochMs: Long,
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createPair(
                aviaryId = aviary.id,
                memberUids = aviary.memberUids,
                cageCode = cageCode,
                sire = sire,
                dam = dam,
                pairedDateEpochMs = pairedDateEpochMs,
                notes = notes
            )
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Pasangan ${sire.ringCode} x ${dam.ringCode} dibuat."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal membuat pasangan."
            }
        }
    }

    fun updatePairStatus(aviaryId: String, pairId: String, status: PairStatus) {
        viewModelScope.launch {
            repository.updatePairStatus(aviaryId, pairId, status)
                .onSuccess { _actionMessage.value = "Status pasangan diperbarui." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal memperbarui status." }
        }
    }

    fun saveClutch(
        context: Context,
        aviary: Aviary,
        profile: UserProfile?,
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
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveClutch(
                aviaryId = aviary.id,
                memberUids = aviary.memberUids,
                existingClutchId = existingClutchId,
                pair = pair,
                clutchNumber = clutchNumber,
                eggCount = eggCount,
                fertileCount = fertileCount,
                hatchedCount = hatchedCount,
                fledgedCount = fledgedCount,
                incubationStartEpochMs = incubationStartEpochMs,
                actualHatchEpochMs = actualHatchEpochMs,
                status = status,
                notes = notes
            )
            _isBusy.value = false
            res.onSuccess { savedId ->
                if (incubationStartEpochMs != null && status == ClutchStatus.INCUBATING) {
                    BreedingNotificationHelper.scheduleIncubationAlerts(
                        context = context,
                        clutchId = savedId,
                        cageCode = pair.cageCode,
                        incubationStartEpochMs = incubationStartEpochMs,
                        enabled = profile?.incubationReminderEnabled ?: true
                    )
                }
                _actionMessage.value = "Data produksi periode #$clutchNumber tersimpan."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal menyimpan catatan produksi."
            }
        }
    }

    fun deleteClutch(aviaryId: String, clutchId: String) {
        viewModelScope.launch {
            repository.deleteClutch(aviaryId, clutchId)
                .onSuccess { _actionMessage.value = "Catatan periode dihapus." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal menghapus periode." }
        }
    }

    fun recordHealthCheck(
        context: Context,
        aviary: Aviary,
        profile: UserProfile?,
        canary: Canary,
        newScore: Int,
        healthStatus: HealthStatus,
        symptoms: String,
        actionTaken: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val threshold = (profile?.healthAlertThreshold ?: 25L).toInt()
            val prevScore = canary.healthScore.toInt()
            val res = repository.recordHealthCheck(
                aviaryId = aviary.id,
                memberUids = aviary.memberUids,
                canary = canary,
                newScore = newScore,
                healthStatus = healthStatus,
                symptoms = symptoms,
                actionTaken = actionTaken,
                dropThreshold = threshold
            )
            _isBusy.value = false
            res.onSuccess { alertTriggered ->
                if (alertTriggered) {
                    BreedingNotificationHelper.triggerHealthEarlyWarning(
                        context = context,
                        canaryName = canary.name,
                        ringCode = canary.ringCode,
                        previousScore = prevScore,
                        newScore = newScore,
                        symptoms = symptoms
                    )
                    _actionMessage.value = "PERINGATAN DINI: Kesehatan ${canary.name} menurun drastis!"
                } else {
                    _actionMessage.value = "Pemeriksaan kesehatan ${canary.name} dicatat."
                }
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal mencatat pemeriksaan kesehatan."
            }
        }
    }

    fun addSupplementTask(
        aviary: Aviary,
        title: String,
        dosage: String,
        targetGroup: String,
        timeLabel: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addSupplementTask(
                aviaryId = aviary.id,
                memberUids = aviary.memberUids,
                title = title,
                dosage = dosage,
                targetGroup = targetGroup,
                timeLabel = timeLabel
            )
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Jadwal suplemen harian ditambahkan."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal menambahkan suplemen."
            }
        }
    }

    fun toggleSupplementDoneToday(aviaryId: String, task: SupplementTask, doneToday: Boolean) {
        viewModelScope.launch {
            repository.toggleSupplementDoneToday(aviaryId, task.id, doneToday)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal memperbarui suplemen." }
        }
    }

    fun deleteSupplementTask(aviaryId: String, taskId: String) {
        viewModelScope.launch {
            repository.deleteSupplementTask(aviaryId, taskId)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal menghapus suplemen." }
        }
    }

    fun updateNotificationPreferences(
        context: Context,
        supplementEnabled: Boolean,
        supplementHour: Int,
        supplementMinute: Int,
        incubationEnabled: Boolean,
        healthAlertThreshold: Int
    ) {
        viewModelScope.launch {
            repository.updateNotificationPreferences(
                supplementEnabled = supplementEnabled,
                supplementHour = supplementHour,
                supplementMinute = supplementMinute,
                incubationEnabled = incubationEnabled,
                healthAlertThreshold = healthAlertThreshold
            ).onSuccess {
                BreedingNotificationHelper.scheduleDailySupplementReminder(
                    context = context,
                    enabled = supplementEnabled,
                    hour = supplementHour,
                    minute = supplementMinute
                )
                _actionMessage.value = "Pengaturan notifikasi otomatis disimpan."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal menyimpan pengaturan notifikasi."
            }
        }
    }

    fun renameAviary(aviaryId: String, newName: String) {
        viewModelScope.launch {
            repository.updateAviaryName(aviaryId, newName)
                .onSuccess { _actionMessage.value = "Nama aviary diperbarui." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal mengubah nama aviary." }
        }
    }

    fun updateFarmSettings(
        aviaryId: String?,
        farmName: String,
        farmAddress: String,
        farmPhone: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val resolvedId = if (!aviaryId.isNullOrBlank()) {
                aviaryId
            } else {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val initRes = repository.ensureUserAndAviaryInitialized(
                    displayName = currentUser?.displayName ?: "Peternak",
                    email = currentUser?.email ?: ""
                )
                initRes.onFailure { err ->
                    _isBusy.value = false
                    _actionMessage.value = err.localizedMessage ?: "Gagal menyiapkan workspace aviary."
                    return@launch
                }.getOrNull()
            }
            if (resolvedId.isNullOrBlank()) {
                _isBusy.value = false
                _actionMessage.value = "Workspace aviary belum siap. Coba lagi sebentar."
                return@launch
            }
            val res = repository.updateFarmSettings(resolvedId, farmName, farmAddress, farmPhone)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Profil Farm tersimpan ke Cloud."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal menyimpan pengaturan farm."
            }
        }
    }

    fun uploadFarmLogo(aviaryId: String, imageUri: Uri, mimeType: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.uploadFarmLogo(aviaryId, imageUri, mimeType)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Logo Farm berhasil diunggah dan disinkronkan ke tim."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal mengunggah logo farm."
            }
        }
    }

    fun deleteFarmLogo(aviaryId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteFarmLogo(aviaryId)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Logo Farm dihapus. Logo default digunakan kembali."
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Gagal menghapus logo farm."
            }
        }
    }

    fun switchAviary(aviaryId: String) {
        viewModelScope.launch {
            repository.switchActiveAviary(aviaryId)
                .onSuccess { _actionMessage.value = "Beralih ke workspace aviary." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Gagal beralih aviary." }
        }
    }

    fun joinAviaryByCode(code: String, displayName: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.joinAviaryByInviteCode(code, displayName)
            _isBusy.value = false
            res.onSuccess { aviaryName ->
                _actionMessage.value = "Berhasil bergabung ke tim $aviaryName!"
                onDone()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Kode undangan tidak ditemukan atau gagal bergabung."
            }
        }
    }

    fun exportAndShareReport(
        context: Context,
        data: AviaryDashboardData,
        isExcel: Boolean
    ) {
        viewModelScope.launch {
            try {
                val aviary = data.activeAviary
                val aviaryName = aviary?.farmName?.takeIf { it.isNotBlank() }
                    ?: aviary?.name
                    ?: "Kenari Tasikmalaya"
                val farmAddress = aviary?.farmAddress.orEmpty()
                val farmPhone = aviary?.farmPhone.orEmpty()
                val stats = computeMonthlyProductivity(data.clutches, null)
                val file: File = if (isExcel) {
                    ReportExporter.exportExcelWorkbook(
                        context = context,
                        aviaryName = aviaryName,
                        farmAddress = farmAddress,
                        farmPhone = farmPhone,
                        canaries = data.canaries,
                        pairs = data.pairs,
                        clutches = data.clutches,
                        monthlyStats = stats,
                        healthLogs = data.healthLogs
                    )
                } else {
                    ReportExporter.exportPdfReport(
                        context = context,
                        aviaryName = aviaryName,
                        farmAddress = farmAddress,
                        farmPhone = farmPhone,
                        canaries = data.canaries,
                        pairs = data.pairs,
                        clutches = data.clutches,
                        monthlyStats = stats,
                        healthLogs = data.healthLogs
                    )
                }
                val mime = if (isExcel) {
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                } else {
                    "application/pdf"
                }
                val label = if (isExcel) "Bagikan Laporan Excel (.xlsx)" else "Bagikan Laporan PDF"
                AdMobManager.suppressNextAppOpen()
                ReportExporter.shareExportedFile(context, file, mime, label)
            } catch (e: Exception) {
                _actionMessage.value = "Gagal membuat laporan: ${e.localizedMessage}"
            }
        }
    }

    private companion object {
        const val TAG = "AviaryViewModel"
    }
}
