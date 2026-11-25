package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BreedingClutch
import com.example.data.model.CanarySex
import com.example.data.model.ClutchStatus
import com.example.data.model.HealthStatus
import com.example.data.model.PairStatus
import com.example.data.model.SupplementTask
import com.example.export.MonthlyProductivityStat
import com.example.ui.components.MonthlyProductivityChartCard
import com.example.ui.theme.CanaryGold
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.CriticalCrimsonBg
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.MonitorAmber
import com.example.ui.viewmodel.AviaryDashboardData
import com.example.utils.IncubationCalculator

@Composable
fun DashboardScreen(
    data: AviaryDashboardData,
    monthlyStats: List<MonthlyProductivityStat>,
    selectedCanaryFilterId: String?,
    hasNotificationPermission: Boolean,
    isBusy: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSelectCanaryFilter: (String?) -> Unit,
    onToggleSupplementToday: (SupplementTask, Boolean) -> Unit,
    onSaveFarmSettings: (aviaryId: String?, farmName: String, farmAddress: String, farmPhone: String, onDone: () -> Unit) -> Unit,
    onUploadFarmLogo: (aviaryId: String, imageUri: Uri, mimeType: String, onDone: () -> Unit) -> Unit,
    onDeleteFarmLogo: (aviaryId: String, onDone: () -> Unit) -> Unit,
    onNavigateToCare: () -> Unit,
    onNavigateToBreeding: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onSignOut: () -> Unit
) {
    var showFarmSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val farmLogoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val aviaryId = data.activeAviary?.id.orEmpty()
        if (uri != null && aviaryId.isNotBlank()) {
            onUploadFarmLogo(aviaryId, uri, context.contentResolver.getType(uri) ?: "image/jpeg") { }
        }
    }
    val activeIncubations = data.clutches.filter {
        it.statusEnum == ClutchStatus.INCUBATING && (it.incubationStartEpochMs ?: 0L) > 0L
    }
    val criticalCanaries = data.canaries.filter { it.healthStatusEnum == HealthStatus.CRITICAL }
    val recentAlerts = data.healthLogs.filter { it.alertTriggered }.take(3)
    val totalEggs = data.clutches.sumOf { it.eggCount.toInt() }
    val totalHatched = data.clutches.sumOf { it.hatchedCount.toInt() }
    val hatchRatePct = if (totalEggs > 0) (totalHatched * 100 / totalEggs) else 0
    val maleCount = data.canaries.count { it.sexEnum == CanarySex.MALE }
    val femaleCount = data.canaries.count { it.sexEnum == CanarySex.FEMALE }
    val activePairCount = data.pairs.count { it.statusEnum == PairStatus.ACTIVE }
    val todayIso = IncubationCalculator.todayIsoDate()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Aviary Header
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    ForestEmerald.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudDone,
                                            contentDescription = "Sinkronisasi cloud aktif",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Cloud Sync Aktif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                Surface(
                                    color = CanaryGold.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Group,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${data.activeAviary?.memberUids?.size ?: 1} Tim",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showFarmSettingsDialog = true },
                                    modifier = Modifier.testTag("open_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Pengaturan Profil Farm",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = "Keluar akun Google",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Kenari Tasikmalaya",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Silsilah Breeding Kenari",
                                style = MaterialTheme.typography.labelLarge,
                                color = CanaryGold
                            )
                            val av = data.activeAviary
                            val farmTitle = av?.farmName?.takeIf { it.isNotBlank() } ?: av?.name.orEmpty()
                            if (farmTitle.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!av?.farmLogoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = av?.farmLogoUrl,
                                            contentDescription = "Logo $farmTitle",
                                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.16f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Storefront,
                                                    contentDescription = "Logo default farm",
                                                    tint = CanaryGold,
                                                    modifier = Modifier.size(25.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = farmTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                }
                            }
                            if (!av?.farmAddress.isNullOrBlank() || !av?.farmPhone.isNullOrBlank()) {
                                Text(
                                    text = listOfNotNull(
                                        av.farmAddress.takeIf { it.isNotBlank() },
                                        av.farmPhone.takeIf { it.isNotBlank() }?.let { "Telp: $it" }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clickable { showFarmSettingsDialog = true }
                                    .testTag("dashboard_farm_settings_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (av?.farmAddress.isNullOrBlank() && av?.farmPhone.isNullOrBlank()) {
                                            "Menu Setting Farm (Atur Nama, Alamat & Telepon)"
                                        } else {
                                            "Setting Profil Farm"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = "Halo, ${data.profile?.displayName ?: "Peternak"} • Google Cloud Terhubung",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Notification Permission Banner if not granted
        if (!hasNotificationPermission) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aktifkan Izin Notifikasi",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Diperlukan untuk pengingat pengeraman 21 hari, suplemen harian & peringatan dini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = onRequestNotificationPermission,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("enable_notifications_button")
                        ) {
                            Text("Izinkan")
                        }
                    }
                }
            }
        }

        // Early Health Warning Banner
        if (criticalCanaries.isNotEmpty() || recentAlerts.isNotEmpty()) {
            item {
                Surface(
                    color = CriticalCrimsonBg,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCare() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CriticalCrimson),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Peringatan dini kesehatan",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Peringatan Dini Kesehatan (${criticalCanaries.size} Indukan Kritis)",
                                style = MaterialTheme.typography.titleSmall,
                                color = CriticalCrimson,
                                fontWeight = FontWeight.Bold
                            )
                            val detailText = if (criticalCanaries.isNotEmpty()) {
                                criticalCanaries.joinToString(", ") { "${it.ringCode} (${it.healthScore})" }
                            } else {
                                "${recentAlerts.first().canaryName} turun dari ${recentAlerts.first().previousScore} ke ${recentAlerts.first().healthScore}"
                            }
                            Text(
                                text = "$detailText. Ketuk untuk tindakan karantina & suplemen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CriticalCrimson.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Bento KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Populasi Indukan",
                        value = "${data.canaries.size}",
                        subtitle = "$maleCount Jantan • $femaleCount Betina",
                        icon = Icons.Rounded.Pets,
                        accent = ForestEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Pasangan Aktif",
                        value = "$activePairCount",
                        subtitle = "${data.pairs.size} total riwayat jodohan",
                        icon = Icons.Rounded.Favorite,
                        accent = CanaryGold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Sedang Mengeram",
                        value = "${activeIncubations.size}",
                        subtitle = "Estimasi siklus 21 hari",
                        icon = Icons.Rounded.Egg,
                        accent = MonitorAmber,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Daya Tetas (Hatch)",
                        value = "$hatchRatePct%",
                        subtitle = "$totalHatched dari $totalEggs telur",
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accent = HealthGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Active 21-Day Incubation Countdowns
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pantauan Pengeraman (21 Hari)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Hitung mundur penetasan telur aktif",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onNavigateToBreeding,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Kelola")
                        }
                    }

                    if (activeIncubations.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada indukan yang sedang mengeram. Catat tanggal mulai mengeram di tab Produksi untuk menghitung estimasi 21 hari.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        activeIncubations.forEach { clutch ->
                            IncubationCountdownItem(clutch = clutch)
                        }
                    }
                }
            }
        }

        // Monthly Productivity Chart with per-indukan filter
        item {
            MonthlyProductivityChartCard(
                stats = monthlyStats,
                canaries = data.canaries,
                selectedCanaryId = selectedCanaryFilterId,
                onSelectCanaryFilter = onSelectCanaryFilter
            )
        }

        // Today's Supplement Checklist
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Suplemen Harian Hari Ini",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Ketuk untuk menandai sudah diberikan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onNavigateToCare,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Jadwal")
                        }
                    }

                    if (data.supplements.isEmpty()) {
                        Text(
                            text = "Belum ada jadwal suplemen harian. Tambahkan di tab Perawatan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        data.supplements.take(4).forEach { task ->
                            val doneToday = task.lastCompletedDate == todayIso
                            Surface(
                                color = if (doneToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleSupplementToday(task, doneToday) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (doneToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = if (doneToday) "Sudah diberikan" else "Belum diberikan",
                                        tint = if (doneToday) HealthGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${task.timeLabel} • ${task.dosage.ifBlank { task.targetGroup }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Export PDF & Excel Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pelaporan Otomatis (PDF & Excel)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Unduh atau bagikan laporan lengkap silsilah, analitik bulanan, dan log kesehatan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportPdf,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_export_pdf_btn")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor PDF")
                        }
                        Button(
                            onClick = onExportExcel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_export_excel_btn")
                        ) {
                            Icon(Icons.Rounded.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor Excel")
                        }
                    }
                }
            }
        }
    }

    if (showFarmSettingsDialog) {
        val av = data.activeAviary
        var farmName by remember(av?.id, av?.farmName) {
            mutableStateOf(av?.farmName?.ifBlank { av.name } ?: "Kenari Tasikmalaya Farm")
        }
        var farmAddress by remember(av?.id, av?.farmAddress) { mutableStateOf(av?.farmAddress ?: "") }
        var farmPhone by remember(av?.id, av?.farmPhone) { mutableStateOf(av?.farmPhone ?: "") }

        AlertDialog(
            onDismissRequest = { showFarmSettingsDialog = false },
            title = { Text("Pengaturan Profil Farm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Lengkapi identitas farm Anda agar tampil di dasbor serta kop laporan PDF dan Excel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!av?.farmLogoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = av?.farmLogoUrl,
                                contentDescription = "Logo farm tersimpan di cloud",
                                modifier = Modifier.size(64.dp).clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Storefront, contentDescription = "Logo default", modifier = Modifier.size(30.dp))
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { farmLogoPicker.launch(arrayOf("image/*")) },
                                enabled = !isBusy && !av?.id.isNullOrBlank()
                            ) {
                                Text(if (av?.farmLogoUrl.isNullOrBlank()) "Pilih Logo Farm" else "Ganti Logo")
                            }
                            if (!av?.farmLogoUrl.isNullOrBlank()) {
                                TextButton(
                                    onClick = {
                                        av?.id?.let { onDeleteFarmLogo(it) { } }
                                    },
                                    enabled = !isBusy
                                ) { Text("Hapus Logo") }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = { Text("Nama Farm *") },
                        leadingIcon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_farm_name_input")
                    )
                    OutlinedTextField(
                        value = farmAddress,
                        onValueChange = { farmAddress = it },
                        label = { Text("Alamat Farm") },
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_farm_address_input")
                    )
                    OutlinedTextField(
                        value = farmPhone,
                        onValueChange = { farmPhone = it },
                        label = { Text("Nomor Telepon Farm") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_farm_phone_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveFarmSettings(av?.id, farmName, farmAddress, farmPhone) {
                            showFarmSettingsDialog = false
                        }
                    },
                    enabled = !isBusy && farmName.trim().length >= 2,
                    modifier = Modifier.testTag("dialog_save_farm_btn")
                ) {
                    Text(if (isBusy) "Menyimpan..." else "Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFarmSettingsDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncubationCountdownItem(clutch: BreedingClutch) {
    val startMs = clutch.incubationStartEpochMs ?: return
    val timeline = IncubationCalculator.calculate(startMs)

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kandang ${clutch.cageCode} • Periode #${clutch.clutchNumber}",
                    style = MaterialTheme.typography.titleSmall
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (timeline.remainingDays == 0) "HARI MENETAS" else "H-${timeline.remainingDays}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { timeline.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeline.currentPhaseLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Est: ${IncubationCalculator.formatShortDate(timeline.expectedHatchEpochMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
