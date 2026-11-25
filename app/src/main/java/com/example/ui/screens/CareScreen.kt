package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Canary
import com.example.data.model.HealthStatus
import com.example.data.model.SupplementTask
import com.example.notifications.BreedingNotificationHelper
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.CriticalCrimsonBg
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthGreenBg
import com.example.ui.theme.MonitorAmber
import com.example.ui.theme.MonitorAmberBg
import com.example.ui.viewmodel.AviaryDashboardData
import com.example.utils.IncubationCalculator
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(
    data: AviaryDashboardData,
    isBusy: Boolean,
    initialHealthTarget: Canary?,
    onClearInitialHealthTarget: () -> Unit,
    onRecordHealthCheck: (
        canary: Canary,
        newScore: Int,
        status: HealthStatus,
        symptoms: String,
        actionTaken: String,
        onDone: () -> Unit
    ) -> Unit,
    onAddSupplement: (title: String, dosage: String, targetGroup: String, timeLabel: String, onDone: () -> Unit) -> Unit,
    onToggleSupplementToday: (SupplementTask, Boolean) -> Unit,
    onDeleteSupplement: (String) -> Unit,
    onUpdateNotificationPreferences: (
        supplementEnabled: Boolean,
        supplementHour: Int,
        supplementMinute: Int,
        incubationEnabled: Boolean,
        healthAlertThreshold: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var healthDialogCanary by remember(initialHealthTarget) { mutableStateOf(initialHealthTarget) }
    var showAddSupplementDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val todayIso = IncubationCalculator.todayIsoDate()
    val profile = data.profile

    var suppEnabled by remember(profile) { mutableStateOf(profile?.supplementReminderEnabled ?: true) }
    var suppHour by remember(profile) { mutableIntStateOf((profile?.supplementHour ?: 7L).toInt()) }
    var suppMinute by remember(profile) { mutableIntStateOf((profile?.supplementMinute ?: 0L).toInt()) }
    var incEnabled by remember(profile) { mutableStateOf(profile?.incubationReminderEnabled ?: true) }
    var dropThreshold by remember(profile) { mutableIntStateOf((profile?.healthAlertThreshold ?: 25L).toInt()) }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("Kesehatan")
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Rounded.Medication, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("Suplemen & Alarm")
            }
        }

        if (selectedTab == 0) {
            val watchlist = data.canaries.filter { it.healthStatusEnum != HealthStatus.HEALTHY }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Pemantauan Kesehatan & Peringatan Dini", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Sistem otomatis mengirim notifikasi peringatan dini jika skor kesehatan indukan turun ≥ $dropThreshold poin atau masuk status Kritis (< 55).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { healthDialogCanary = data.canaries.firstOrNull() },
                                enabled = data.canaries.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("record_health_check_btn")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Catat Pemeriksaan Kesehatan")
                            }
                        }
                    }
                }

                if (watchlist.isNotEmpty()) {
                    item {
                        Text(
                            text = "Perlu Perhatian Khusus (${watchlist.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = CriticalCrimson
                        )
                    }
                    items(watchlist, key = { "watch_${it.id}" }) { bird ->
                        val isCrit = bird.healthStatusEnum == HealthStatus.CRITICAL
                        Surface(
                            color = if (isCrit) CriticalCrimsonBg else MonitorAmberBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { healthDialogCanary = bird }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = if (isCrit) CriticalCrimson else MonitorAmber
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${bird.ringCode} • ${bird.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isCrit) CriticalCrimson else MonitorAmber
                                    )
                                    Text(
                                        text = "Status: ${bird.healthStatusEnum.labelId} • Skor: ${bird.healthScore}/100",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                OutlinedButton(onClick = { healthDialogCanary = bird }) {
                                    Text("Update")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Riwayat Pemeriksaan Kesehatan (${data.healthLogs.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                if (data.healthLogs.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada catatan pemeriksaan kesehatan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(data.healthLogs, key = { it.id }) { log ->
                        val (bg, fg) = when (log.healthStatusEnum) {
                            HealthStatus.HEALTHY -> HealthGreenBg to HealthGreen
                            HealthStatus.MONITORING -> MonitorAmberBg to MonitorAmber
                            HealthStatus.CRITICAL -> CriticalCrimsonBg to CriticalCrimson
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
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
                                        text = "${log.canaryRing} • ${log.canaryName}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "${log.previousScore} → ${log.healthScore}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = fg,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (log.alertTriggered) {
                                    Text(
                                        text = "Peringatan Dini Terpicu (Penurunan Signifikan)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CriticalCrimson
                                    )
                                }
                                if (log.symptoms.isNotBlank()) {
                                    Text(text = "Gejala: ${log.symptoms}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (log.actionTaken.isNotBlank()) {
                                    Text(
                                        text = "Tindakan: ${log.actionTaken}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = IncubationCalculator.formatDate(log.recordedDateEpochMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Daily Supplements List
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                Text("Daftar Suplemen Harian", style = MaterialTheme.typography.titleMedium)
                                Button(
                                    onClick = { showAddSupplementDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("add_supplement_btn")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah")
                                }
                            }

                            if (data.supplements.isEmpty()) {
                                Text(
                                    text = "Tambahkan jadwal vitamin, kalsium, atau eggfood harian.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                data.supplements.forEach { task ->
                                    val doneToday = task.lastCompletedDate == todayIso
                                    Surface(
                                        color = if (doneToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleSupplementToday(task, doneToday) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (doneToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (doneToday) HealthGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = task.title, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    text = "${task.timeLabel} • ${task.dosage} • ${task.targetGroup}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { onDeleteSupplement(task.id) }) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = "Hapus suplemen",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Auto Notification Preferences
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Pengaturan Notifikasi Otomatis", style = MaterialTheme.typography.titleMedium)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pengingat Suplemen Harian", style = MaterialTheme.typography.titleSmall)
                                    Text("Kirim notifikasi setiap hari pada jam terjadwal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = suppEnabled, onCheckedChange = { suppEnabled = it })
                            }

                            OutlinedButton(
                                onClick = { showTimePickerDialog = true },
                                enabled = suppEnabled,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(String.format("Jam Pengingat Harian: %02d:%02d", suppHour, suppMinute))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Notifikasi Pengeraman 21 Hari", style = MaterialTheme.typography.titleSmall)
                                    Text("Pengingat H+7 teropong, H+14 embrio, H+20 & H+21 menetas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = incEnabled, onCheckedChange = { incEnabled = it })
                            }

                            Column {
                                Text(
                                    text = "Ambang Peringatan Dini Penurunan Skor: $dropThreshold poin",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Slider(
                                    value = dropThreshold.toFloat(),
                                    onValueChange = { dropThreshold = it.roundToInt() },
                                    valueRange = 10f..50f
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onUpdateNotificationPreferences(
                                            suppEnabled, suppHour, suppMinute, incEnabled, dropThreshold
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Simpan Jadwal")
                                }
                                OutlinedButton(
                                    onClick = {
                                        BreedingNotificationHelper.showImmediateNotification(
                                            context = context,
                                            channelId = BreedingNotificationHelper.CHANNEL_SUPPLEMENT,
                                            notificationId = 9001,
                                            title = "Uji Notifikasi Kenari Tasikmalaya",
                                            body = "Notifikasi suplemen harian, pengeraman 21 hari & peringatan dini aktif."
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Uji")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    healthDialogCanary?.let { target ->
        HealthCheckDialog(
            canaries = data.canaries,
            initialCanary = target,
            isBusy = isBusy,
            onDismiss = {
                healthDialogCanary = null
                onClearInitialHealthTarget()
            },
            onSubmit = { bird, score, status, symptoms, action ->
                onRecordHealthCheck(bird, score, status, symptoms, action) {
                    healthDialogCanary = null
                    onClearInitialHealthTarget()
                }
            }
        )
    }

    if (showAddSupplementDialog) {
        AddSupplementDialog(
            isBusy = isBusy,
            onDismiss = { showAddSupplementDialog = false },
            onSubmit = { title, dosage, group, time ->
                onAddSupplement(title, dosage, group, time) {
                    showAddSupplementDialog = false
                }
            }
        )
    }

    if (showTimePickerDialog) {
        val timeState = rememberTimePickerState(initialHour = suppHour, initialMinute = suppMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Pilih Jam Pengingat Suplemen") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    suppHour = timeState.hour
                    suppMinute = timeState.minute
                    showTimePickerDialog = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun HealthCheckDialog(
    canaries: List<Canary>,
    initialCanary: Canary,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Canary, Int, HealthStatus, String, String) -> Unit
) {
    var selectedBird by remember { mutableStateOf(initialCanary) }
    var birdMenuOpen by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(initialCanary.healthScore.toInt()) }
    var status by remember { mutableStateOf(initialCanary.healthStatusEnum) }
    var symptoms by remember { mutableStateOf("") }
    var actionTaken by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pemeriksaan Kesehatan Kenari") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    OutlinedButton(onClick = { birdMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${selectedBird.ringCode} • ${selectedBird.name}")
                    }
                    DropdownMenu(expanded = birdMenuOpen, onDismissRequest = { birdMenuOpen = false }) {
                        canaries.forEach { bird ->
                            DropdownMenuItem(
                                text = { Text("${bird.ringCode} • ${bird.name} (Skor ${bird.healthScore})") },
                                onClick = {
                                    selectedBird = bird
                                    score = bird.healthScore.toInt()
                                    status = bird.healthStatusEnum
                                    birdMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Text("Skor Kesehatan Baru: $score / 100 (Sebelumnya: ${selectedBird.healthScore})", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = score.toFloat(),
                    onValueChange = {
                        score = it.roundToInt()
                        status = when {
                            score >= 75 -> HealthStatus.HEALTHY
                            score >= 55 -> HealthStatus.MONITORING
                            else -> HealthStatus.CRITICAL
                        }
                    },
                    valueRange = 0f..100f
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HealthStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.labelId) }
                        )
                    }
                }

                OutlinedTextField(
                    value = symptoms,
                    onValueChange = { symptoms = it },
                    label = { Text("Gejala / Kondisi Fisik") },
                    placeholder = { Text("Contoh: Bulu mengembang, suara serak, nyilet") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = actionTaken,
                    onValueChange = { actionTaken = it },
                    label = { Text("Tindakan Penanganan") },
                    placeholder = { Text("Contoh: Karantina lampu hangat + vitamin") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedBird, score, status, symptoms, actionTaken) },
                enabled = !isBusy,
                modifier = Modifier.testTag("save_health_log_btn")
            ) {
                Text(if (isBusy) "Menyimpan..." else "Simpan Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun AddSupplementDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, dosage: String, targetGroup: String, timeLabel: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var targetGroup by remember { mutableStateOf("Semua Indukan") }
    var timeLabel by remember { mutableStateOf("07:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Suplemen Harian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Suplemen / Vitamin *") },
                    placeholder = { Text("Contoh: Vitamin E Breeding Booster") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosis / Takaran") },
                    placeholder = { Text("Contoh: 3 tetes / 50ml air minum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetGroup,
                    onValueChange = { targetGroup = it },
                    label = { Text("Target Indukan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = timeLabel,
                    onValueChange = { timeLabel = it },
                    label = { Text("Jam Pemberian") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, dosage, targetGroup, timeLabel) },
                enabled = !isBusy && title.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
