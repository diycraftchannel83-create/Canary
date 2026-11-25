package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ads.AdMobManager
import com.example.data.model.BreedingClutch
import com.example.data.model.BreedingStatus
import com.example.data.model.Canary
import com.example.data.model.CanarySex
import com.example.data.model.HealthStatus
import com.example.ui.components.PedigreeTreeBottomSheet
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.CriticalCrimsonBg
import com.example.ui.theme.FemaleRose
import com.example.ui.theme.FemaleRoseBg
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthGreenBg
import com.example.ui.theme.MaleBlue
import com.example.ui.theme.MaleBlueBg
import com.example.ui.theme.MonitorAmber
import com.example.ui.theme.MonitorAmberBg
import com.example.ui.viewmodel.AviaryDashboardData
import com.example.utils.CanaryPhotoUtils
import com.example.utils.IncubationCalculator
import kotlin.math.roundToInt

@Composable
fun GenealogyScreen(
    data: AviaryDashboardData,
    isBusy: Boolean,
    onSaveCanary: (
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
        externalSirePhotoBase64: String?,
        externalDamPhotoBase64: String?,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteCanary: (String) -> Unit,
    onOpenHealthCheck: (Canary) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sexFilter by remember { mutableStateOf<CanarySex?>(null) }
    var statusFilter by remember { mutableStateOf<BreedingStatus?>(null) }
    var editingCanary by remember { mutableStateOf<Canary?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var pedigreeSubject by remember { mutableStateOf<Canary?>(null) }
    var photoPreviewSubject by remember { mutableStateOf<Canary?>(null) }
    var pendingDeleteCanary by remember { mutableStateOf<Canary?>(null) }

    val byId = remember(data.canaries) { data.canaries.associateBy { it.id } }

    val filtered = remember(data.canaries, searchQuery, sexFilter, statusFilter) {
        data.canaries.filter { bird ->
            val matchesSex = sexFilter == null || bird.sexEnum == sexFilter
            val matchesStatus = statusFilter == null || bird.breedingStatusEnum == statusFilter
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                bird.name.lowercase().contains(q) ||
                bird.ringCode.lowercase().contains(q) ||
                bird.breed.lowercase().contains(q) ||
                bird.cageCode.lowercase().contains(q) ||
                bird.lineageNotes.lowercase().contains(q) ||
                bird.productionHistoryNotes.lowercase().contains(q)
            matchesSex && matchesStatus && matchesQuery
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & filter bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari kode ring, nama, strain, kandang, silsilah...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("canary_search_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sexFilter == null,
                        onClick = { sexFilter = null },
                        label = { Text("Semua (${data.canaries.size})") }
                    )
                    FilterChip(
                        selected = sexFilter == CanarySex.MALE,
                        onClick = { sexFilter = CanarySex.MALE },
                        label = { Text("Jantan (${data.canaries.count { it.sexEnum == CanarySex.MALE }})") }
                    )
                    FilterChip(
                        selected = sexFilter == CanarySex.FEMALE,
                        onClick = { sexFilter = CanarySex.FEMALE },
                        label = { Text("Betina (${data.canaries.count { it.sexEnum == CanarySex.FEMALE }})") }
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("Semua Status") }
                    )
                    BreedingStatus.entries.forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text(st.labelId) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_pedigree_empty_1795574369204),
                            contentDescription = "Ilustrasi silsilah kenari",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(170.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Text(
                            text = if (data.canaries.isEmpty()) "Belum Ada Indukan Terdaftar" else "Tidak Ada Hasil Cocok",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (data.canaries.isEmpty()) {
                                "Daftarkan indukan jantan & betina beserta kode ring dan asal usulnya untuk membangun pohon silsilah."
                            } else {
                                "Ubah kata kunci pencarian atau filter jenis kelamin."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (data.canaries.isEmpty()) {
                            Button(
                                onClick = {
                                    editingCanary = null
                                    showFormDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("empty_add_canary_btn")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tambah Indukan Pertama")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { canary ->
                        val liveClutches = remember(data.clutches, canary.id) {
                            data.clutches.filter { it.sireId == canary.id || it.damId == canary.id }
                        }
                        CanaryCard(
                            canary = canary,
                            sire = canary.sireId?.let { byId[it] },
                            dam = canary.damId?.let { byId[it] },
                            liveClutches = liveClutches,
                            onViewPedigree = { pedigreeSubject = canary },
                            onViewPhoto = { photoPreviewSubject = canary },
                            onHealthCheck = { onOpenHealthCheck(canary) },
                            onEdit = {
                                editingCanary = canary
                                showFormDialog = true
                            },
                            onDelete = { pendingDeleteCanary = canary }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingCanary = null
                showFormDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("fab_add_canary")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Tambah kenari")
        }
    }

    if (showFormDialog) {
        CanaryFormDialog(
            initial = editingCanary,
            allCanaries = data.canaries,
            isBusy = isBusy,
            onDismiss = { showFormDialog = false },
            onSubmit = { ring, name, sex, breed, color, hatchMs, sId, dId, sRing, dRing, cage, bStatus, hStatus, hScore, notes, lineageNotes, clutchCnt, eggCnt, hatchCnt, fledgeCnt, prodNotes, newPhotoBase64, externalSirePhotoBase64, externalDamPhotoBase64 ->
                onSaveCanary(
                    editingCanary?.id,
                    editingCanary?.hasUploadedPhoto == true,
                    ring, name, sex, breed, color, hatchMs,
                    sId, dId, sRing, dRing, cage, bStatus, hStatus, hScore, notes,
                    lineageNotes, clutchCnt, eggCnt, hatchCnt, fledgeCnt, prodNotes,
                    newPhotoBase64, externalSirePhotoBase64, externalDamPhotoBase64
                ) {
                    showFormDialog = false
                }
            }
        )
    }

    pedigreeSubject?.let { subject ->
        PedigreeTreeBottomSheet(
            subject = subject,
            allCanaries = data.canaries,
            pairs = data.pairs,
            clutches = data.clutches,
            onSelectCanary = { next -> pedigreeSubject = next },
            onEditCanary = { bird ->
                pedigreeSubject = null
                editingCanary = bird
                showFormDialog = true
            },
            onDismiss = { pedigreeSubject = null }
        )
    }

    photoPreviewSubject?.let { subject ->
        CanaryPhotoPreviewDialog(
            canary = subject,
            onDismiss = { photoPreviewSubject = null }
        )
    }

    pendingDeleteCanary?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCanary = null },
            title = { Text("Hapus Data Indukan?") },
            text = {
                Text("Anda yakin ingin menghapus ${target.name} (${target.ringCode}) dari Firestore? Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCanary(target.id)
                        pendingDeleteCanary = null
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCanary = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun CanaryCard(
    canary: Canary,
    sire: Canary?,
    dam: Canary?,
    liveClutches: List<BreedingClutch>,
    onViewPedigree: () -> Unit,
    onViewPhoto: () -> Unit,
    onHealthCheck: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isMale = canary.sexEnum == CanarySex.MALE
    val sexBg = if (isMale) MaleBlueBg else FemaleRoseBg
    val sexFg = if (isMale) MaleBlue else FemaleRose
    val photoBitmap = remember(canary.photoBase64) {
        CanaryPhotoUtils.decodeBase64ToImageBitmap(canary.photoBase64)
    }

    val (healthBg, healthFg) = when (canary.healthStatusEnum) {
        HealthStatus.HEALTHY -> HealthGreenBg to HealthGreen
        HealthStatus.MONITORING -> MonitorAmberBg to MonitorAmber
        HealthStatus.CRITICAL -> CriticalCrimsonBg to CriticalCrimson
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewPedigree)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = sexBg, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isMale) Icons.Rounded.Male else Icons.Rounded.Female,
                                contentDescription = canary.sexEnum.labelId,
                                tint = sexFg,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = canary.sexEnum.labelId,
                                style = MaterialTheme.typography.labelMedium,
                                color = sexFg
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = canary.ringCode,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(color = healthBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "${canary.healthStatusEnum.labelId} • ${canary.healthScore}",
                        style = MaterialTheme.typography.labelMedium,
                        color = healthFg,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (photoBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onViewPhoto)
                    ) {
                        Image(
                            bitmap = photoBitmap,
                            contentDescription = "Foto indukan asli ${canary.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                                .padding(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Foto terkunci permanen",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(64.dp)
                            .clickable(onClick = onEdit)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Ambil foto kamera",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Kamera",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = canary.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${canary.breed}${if (canary.featherColor.isNotBlank()) " • ${canary.featherColor}" else ""}${if (canary.cageCode.isNotBlank()) " • Kandang ${canary.cageCode}" else ""} • ${canary.breedingStatusEnum.labelId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (photoBitmap != null) {
                        Text(
                            text = "Foto indukan asli terkunci permanen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Genealogy Lineage Strip
            val sireText = sire?.let { "${it.ringCode} (${it.name})" } ?: canary.sireRing?.takeIf { it.isNotBlank() } ?: "-"
            val damText = dam?.let { "${it.ringCode} (${it.name})" } ?: canary.damRing?.takeIf { it.isNotBlank() } ?: "-"
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Silsilah • Jantan: $sireText  |  Betina: $damText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (canary.lineageNotes.isNotBlank()) {
                        Text(
                            text = "Jalur Trah: ${canary.lineageNotes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Production History Summary Strip
            val totalClutches = canary.recordedClutchCount.toInt() + liveClutches.size
            val totalEggs = canary.recordedEggCount.toInt() + liveClutches.sumOf { it.eggCount.toInt() }
            val totalHatched = canary.recordedHatchedCount.toInt() + liveClutches.sumOf { it.hatchedCount.toInt() }
            val totalFledged = canary.recordedFledgedCount.toInt() + liveClutches.sumOf { it.fledgedCount.toInt() }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Egg,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Produksi: $totalClutches periode • $totalEggs telur • $totalHatched menetas • $totalFledged mandiri",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (canary.productionHistoryNotes.isNotBlank()) {
                        Text(
                            text = canary.productionHistoryNotes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewPedigree,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Silsilah")
                    }
                    OutlinedButton(
                        onClick = onHealthCheck,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kesehatan")
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit kenari")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Hapus kenari",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanaryPhotoPreviewDialog(
    canary: Canary,
    onDismiss: () -> Unit
) {
    val photoBitmap = remember(canary.photoBase64) {
        CanaryPhotoUtils.decodeBase64ToImageBitmap(canary.photoBase64)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto Indukan Asli • ${canary.ringCode}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap,
                        contentDescription = "Foto asli ${canary.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                Text(
                    text = "${canary.name} (${canary.sexEnum.labelId} • ${canary.breed})",
                    style = MaterialTheme.typography.titleSmall
                )
                canary.photoCapturedAtEpochMs?.let { ts ->
                    Text(
                        text = "Diambil dari kamera: ${IncubationCalculator.formatDate(ts)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Foto indukan asli terkunci permanen dan tidak dapat diedit atau dihapus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanaryFormDialog(
    initial: Canary?,
    allCanaries: List<Canary>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
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
        externalSirePhotoBase64: String?,
        externalDamPhotoBase64: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val alreadyUploadedPhoto = initial?.hasUploadedPhoto == true
    val uploadedPhotoBitmap = remember(initial?.photoBase64) {
        CanaryPhotoUtils.decodeBase64ToImageBitmap(initial?.photoBase64)
    }

    var ringCode by remember { mutableStateOf(initial?.ringCode ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sex by remember { mutableStateOf(initial?.sexEnum ?: CanarySex.MALE) }
    var breed by remember { mutableStateOf(initial?.breed ?: "F1 Yorkshire") }
    var featherColor by remember { mutableStateOf(initial?.featherColor ?: "") }
    var cageCode by remember { mutableStateOf(initial?.cageCode ?: "") }
    var hatchDateMs by remember { mutableLongStateOf(initial?.hatchDateEpochMs?.takeIf { it > 0 } ?: System.currentTimeMillis()) }
    var selectedSireId by remember { mutableStateOf(initial?.sireId) }
    var selectedDamId by remember { mutableStateOf(initial?.damId) }
    var extSireRing by remember { mutableStateOf(initial?.sireRing ?: "") }
    var extDamRing by remember { mutableStateOf(initial?.damRing ?: "") }
    var extSirePhoto by remember(initial?.id) { mutableStateOf(initial?.externalSirePhotoBase64) }
    var extDamPhoto by remember(initial?.id) { mutableStateOf(initial?.externalDamPhotoBase64) }
    var breedingStatus by remember { mutableStateOf(initial?.breedingStatusEnum ?: BreedingStatus.ACTIVE) }
    var healthStatus by remember { mutableStateOf(initial?.healthStatusEnum ?: HealthStatus.HEALTHY) }
    var healthScore by remember { mutableIntStateOf((initial?.healthScore ?: 90L).toInt()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var lineageNotes by remember { mutableStateOf(initial?.lineageNotes ?: "") }
    var recordedClutchText by remember { mutableStateOf((initial?.recordedClutchCount ?: 0L).takeIf { it > 0L }?.toString() ?: "") }
    var recordedEggText by remember { mutableStateOf((initial?.recordedEggCount ?: 0L).takeIf { it > 0L }?.toString() ?: "") }
    var recordedHatchedText by remember { mutableStateOf((initial?.recordedHatchedCount ?: 0L).takeIf { it > 0L }?.toString() ?: "") }
    var recordedFledgedText by remember { mutableStateOf((initial?.recordedFledgedCount ?: 0L).takeIf { it > 0L }?.toString() ?: "") }
    var productionHistoryNotes by remember { mutableStateOf(initial?.productionHistoryNotes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var sireMenuOpen by remember { mutableStateOf(false) }
    var damMenuOpen by remember { mutableStateOf(false) }
    val sirePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { extSirePhoto = CanaryPhotoUtils.encodeCapturedPhotoToBase64(context, it) } }
    val damPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { extDamPhoto = CanaryPhotoUtils.encodeCapturedPhotoToBase64(context, it) } }

    var pendingPhotoBase64 by remember(initial?.id) { mutableStateOf<String?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val pendingPreviewBitmap = remember(pendingPhotoBase64) {
        CanaryPhotoUtils.decodeBase64ToImageBitmap(pendingPhotoBase64)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            val encoded = CanaryPhotoUtils.encodeCapturedPhotoToBase64(context, uri)
            if (encoded != null) {
                pendingPhotoBase64 = encoded
                cameraError = null
            } else {
                cameraError = "Gagal memproses foto kamera. Silakan coba lagi."
            }
        }
    }

    fun launchCameraCapture() {
        cameraError = null
        try {
            val (_, uri) = CanaryPhotoUtils.createCameraCaptureFile(context)
            pendingCaptureUri = uri
            AdMobManager.suppressNextAppOpen()
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            cameraError = "Kamera tidak tersedia: ${e.localizedMessage}"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            cameraError = "Izin kamera ditolak. Aktifkan izin kamera untuk memotret indukan asli."
        }
    }

    val maleCandidates = remember(allCanaries, initial) {
        allCanaries.filter { it.sexEnum == CanarySex.MALE && it.id != initial?.id }
    }
    val femaleCandidates = remember(allCanaries, initial) {
        allCanaries.filter { it.sexEnum == CanarySex.FEMALE && it.id != initial?.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Tambah Indukan Kenari" else "Edit Data Indukan")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ringCode,
                    onValueChange = { ringCode = it },
                    label = { Text("Kode Ring / Gelang *") },
                    placeholder = { Text("Contoh: KNR-2026-01") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("canary_ring_input")
                )
                Text("Jenis Kelamin", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CanarySex.entries.forEach { option ->
                        FilterChip(
                            selected = sex == option,
                            onClick = { sex = option },
                            label = { Text(option.labelId) }
                        )
                    }
                }

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Strain / Ras *") },
                    placeholder = { Text("Yorkshire, F1 YS, AF, Lokal, Red Factor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tanggal Menetas: ${IncubationCalculator.formatDate(hatchDateMs)}")
                }

                Text("Silsilah Indukan (Orang Tua)", style = MaterialTheme.typography.labelMedium)
                // Sire picker
                Box {
                    val selectedSire = maleCandidates.firstOrNull { it.id == selectedSireId }
                    OutlinedButton(
                        onClick = { sireMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedSire?.let { "Jantan: ${it.ringCode} (${it.name})" } ?: "Pilih Induk Jantan (Opsional)")
                    }
                    DropdownMenu(expanded = sireMenuOpen, onDismissRequest = { sireMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Tidak dipilih / Ring eksternal") },
                            onClick = {
                                selectedSireId = null
                                sireMenuOpen = false
                            }
                        )
                        maleCandidates.forEach { male ->
                            DropdownMenuItem(
                                text = { Text("${male.ringCode} • ${male.name}") },
                                onClick = {
                                    selectedSireId = male.id
                                    extSireRing = male.ringCode
                                    sireMenuOpen = false
                                }
                            )
                        }
                    }
                }
                if (selectedSireId == null) {
                    OutlinedTextField(
                        value = extSireRing,
                        onValueChange = { extSireRing = it },
                        label = { Text("Kode Ring Induk Jantan Eksternal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ParentPhotoField(title = "Foto Indukan Jantan Eksternal", photoBase64 = extSirePhoto, onPick = { sirePhotoPicker.launch("image/*") })
                }

                // Dam picker
                Box {
                    val selectedDam = femaleCandidates.firstOrNull { it.id == selectedDamId }
                    OutlinedButton(
                        onClick = { damMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedDam?.let { "Betina: ${it.ringCode} (${it.name})" } ?: "Pilih Induk Betina (Opsional)")
                    }
                    DropdownMenu(expanded = damMenuOpen, onDismissRequest = { damMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Tidak dipilih / Ring eksternal") },
                            onClick = {
                                selectedDamId = null
                                damMenuOpen = false
                            }
                        )
                        femaleCandidates.forEach { female ->
                            DropdownMenuItem(
                                text = { Text("${female.ringCode} • ${female.name}") },
                                onClick = {
                                    selectedDamId = female.id
                                    extDamRing = female.ringCode
                                    damMenuOpen = false
                                }
                            )
                        }
                    }
                }
                if (selectedDamId == null) {
                    OutlinedTextField(
                        value = extDamRing,
                        onValueChange = { extDamRing = it },
                        label = { Text("Kode Ring Induk Betina Eksternal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ParentPhotoField(title = "Foto Indukan Betina Eksternal", photoBase64 = extDamPhoto, onPick = { damPhotoPicker.launch("image/*") })
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        ringCode, name.ifBlank { ringCode }, sex, breed, featherColor, hatchDateMs,
                        selectedSireId, selectedDamId, extSireRing.ifBlank { null }, extDamRing.ifBlank { null },
                        cageCode, breedingStatus, healthStatus, healthScore, notes,
                        lineageNotes,
                        recordedClutchText.toIntOrNull() ?: 0,
                        recordedEggText.toIntOrNull() ?: 0,
                        recordedHatchedText.toIntOrNull() ?: 0,
                        recordedFledgedText.toIntOrNull() ?: 0,
                        productionHistoryNotes,
                        if (alreadyUploadedPhoto) null else pendingPhotoBase64,
                        extSirePhoto,
                        extDamPhoto
                    )
                },
                enabled = !isBusy && ringCode.isNotBlank() && breed.isNotBlank(),
                modifier = Modifier.testTag("save_canary_btn")
            ) {
                Text(if (isBusy) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = hatchDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { hatchDateMs = it }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ParentPhotoField(title: String, photoBase64: String?, onPick: () -> Unit) {
    val bitmap = remember(photoBase64) { CanaryPhotoUtils.decodeBase64ToImageBitmap(photoBase64) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        if (bitmap != null) Image(bitmap = bitmap, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)))
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (photoBase64.isNullOrBlank()) "Pilih Foto dari Galeri" else "Ganti Foto Indukan")
        }
    }
}
