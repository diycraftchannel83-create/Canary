package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BreedingClutch
import com.example.data.model.BreedingPair
import com.example.data.model.Canary
import com.example.data.model.CanarySex
import com.example.data.model.ClutchStatus
import com.example.data.model.InventoryStatus
import com.example.data.model.OwnershipStatus
import com.example.data.model.HealthStatus
import com.example.data.model.PairStatus
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.CriticalCrimsonBg
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthGreenBg
import com.example.ui.theme.MonitorAmber
import com.example.ui.theme.MonitorAmberBg
import com.example.ui.viewmodel.AviaryDashboardData
import com.example.utils.GenealogyAnalyzer
import com.example.utils.IncubationCalculator

@Composable
fun BreedingScreen(
    data: AviaryDashboardData,
    isBusy: Boolean,
    onCreatePair: (cageCode: String, sire: Canary, dam: Canary, pairedDateMs: Long, notes: String, onDone: () -> Unit) -> Unit,
    onUpdatePairStatus: (pairId: String, status: PairStatus) -> Unit,
    onSaveClutch: (
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
    ) -> Unit,
    onDeleteClutch: (String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showPairDialog by remember { mutableStateOf(false) }
    var activePairForNewClutch by remember { mutableStateOf<BreedingPair?>(null) }
    var editingClutchPairPair by remember { mutableStateOf<Pair<BreedingClutch, BreedingPair>?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SegmentedButton(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Rounded.Egg, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("Penjodohan & Telur")
            }
            SegmentedButton(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("Kalkulator 21 Hari")
            }
        }

        if (selectedSubTab == 0) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (data.pairs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text("Belum Ada Pasangan Breeding", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Buat pasangan jantan & betina untuk mulai mencatat telur, tanggal mulai mengeram, dan estimasi menetas 21 hari.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showPairDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("empty_add_pair_btn")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buat Pasangan Baru")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(data.pairs, key = { it.id }) { pair ->
                            val pairClutches = data.clutches.filter { it.pairId == pair.id }
                                .sortedByDescending { it.clutchNumber }
                            BreedingPairCard(
                                pair = pair,
                                clutches = pairClutches,
                                onAddClutch = { activePairForNewClutch = pair },
                                onEditClutch = { clutch -> editingClutchPairPair = clutch to pair },
                                onDeleteClutch = onDeleteClutch,
                                onToggleStatus = { nextStatus -> onUpdatePairStatus(pair.id, nextStatus) }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showPairDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(18.dp)
                        .testTag("fab_add_pair")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Tambah pasangan")
                }
            }
        } else {
            Incubation21DayCalculatorPane(
                pairs = data.pairs,
                clutches = data.clutches,
                onApplyToPair = { pair, startMs ->
                    val nextNum = (data.clutches.filter { it.pairId == pair.id }.maxOfOrNull { it.clutchNumber } ?: 0L).toInt() + 1
                    onSaveClutch(
                        null, pair, nextNum, 4, 0, 0, 0, startMs, null,
                        ClutchStatus.INCUBATING, "Dijadwalkan dari Kalkulator 21 Hari"
                    ) {
                        selectedSubTab = 0
                    }
                }
            )
        }
    }

    if (showPairDialog) {
        CreatePairDialog(
            canaries = data.canaries,
            isBusy = isBusy,
            onDismiss = { showPairDialog = false },
            onSubmit = { cage, sire, dam, dateMs, notes ->
                onCreatePair(cage, sire, dam, dateMs, notes) {
                    showPairDialog = false
                }
            }
        )
    }

    activePairForNewClutch?.let { pair ->
        val nextNum = (data.clutches.filter { it.pairId == pair.id }.maxOfOrNull { it.clutchNumber } ?: 0L).toInt() + 1
        ClutchFormDialog(
            pair = pair,
            initial = null,
            defaultClutchNumber = nextNum,
            isBusy = isBusy,
            onDismiss = { activePairForNewClutch = null },
            onSubmit = { num, eggs, fertile, hatched, fledged, incStart, actualHatch, status, notes ->
                onSaveClutch(
                    null, pair, num, eggs, fertile, hatched, fledged,
                    incStart, actualHatch, status, notes
                ) {
                    activePairForNewClutch = null
                }
            }
        )
    }

    editingClutchPairPair?.let { (clutch, pair) ->
        ClutchFormDialog(
            pair = pair,
            initial = clutch,
            defaultClutchNumber = clutch.clutchNumber.toInt(),
            isBusy = isBusy,
            onDismiss = { editingClutchPairPair = null },
            onSubmit = { num, eggs, fertile, hatched, fledged, incStart, actualHatch, status, notes ->
                onSaveClutch(
                    clutch.id, pair, num, eggs, fertile, hatched, fledged,
                    incStart, actualHatch, status, notes
                ) {
                    editingClutchPairPair = null
                }
            }
        )
    }
}

@Composable
private fun BreedingPairCard(
    pair: BreedingPair,
    clutches: List<BreedingClutch>,
    onAddClutch: () -> Unit,
    onEditClutch: (BreedingClutch) -> Unit,
    onDeleteClutch: (String) -> Unit,
    onToggleStatus: (PairStatus) -> Unit
) {
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
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Kandang ${pair.cageCode}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                FilterChip(
                    selected = pair.statusEnum == PairStatus.ACTIVE,
                    onClick = {
                        val next = if (pair.statusEnum == PairStatus.ACTIVE) PairStatus.RESTING else PairStatus.ACTIVE
                        onToggleStatus(next)
                    },
                    label = { Text(pair.statusEnum.labelId) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${pair.sireName}  ×  ${pair.damName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ring: ${pair.sireRing} × ${pair.damRing} • Dijodohkan ${IncubationCalculator.formatShortDate(pair.pairedDateEpochMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Periode / Clutch (${clutches.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedButton(
                    onClick = onAddClutch,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_clutch_btn_${pair.cageCode}")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat Telur")
                }
            }

            if (clutches.isEmpty()) {
                Text(
                    text = "Belum ada periode bertelur untuk pasangan ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                clutches.forEach { clutch ->
                    ClutchItemRow(
                        clutch = clutch,
                        onEdit = { onEditClutch(clutch) },
                        onDelete = { onDeleteClutch(clutch.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClutchItemRow(
    clutch: BreedingClutch,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLocked = clutch.statusEnum == ClutchStatus.COMPLETED || clutch.statusEnum == ClutchStatus.FAILED
    val (bg, fg) = when (clutch.statusEnum) {
        ClutchStatus.INCUBATING -> MonitorAmberBg to MonitorAmber
        ClutchStatus.HATCHED, ClutchStatus.COMPLETED -> HealthGreenBg to HealthGreen
        ClutchStatus.FAILED -> CriticalCrimsonBg to CriticalCrimson
        ClutchStatus.LAYING -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Periode #${clutch.clutchNumber}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = clutch.statusEnum.labelId,
                            style = MaterialTheme.typography.labelSmall,
                            color = fg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row {
                    if (!isLocked) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Perbarui periode", modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus periode", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = "Telur: ${clutch.eggCount}  •  Fertil: ${clutch.fertileCount}  •  Menetas: ${clutch.hatchedCount}  •  Sapih: ${clutch.fledgedCount}",
                style = MaterialTheme.typography.bodySmall
            )

            clutch.incubationStartEpochMs?.let { startMs ->
                val timeline = IncubationCalculator.calculate(startMs)
                Text(
                    text = "Mulai Mengeram: ${IncubationCalculator.formatDate(startMs)}  →  Est. Menetas (+21 Hari): ${IncubationCalculator.formatDate(timeline.expectedHatchEpochMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (clutch.statusEnum == ClutchStatus.INCUBATING) {
                    LinearProgressIndicator(
                        progress = { timeline.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "${timeline.currentPhaseLabel} • Sisa ${timeline.remainingDays} hari",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonitorAmber
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Incubation21DayCalculatorPane(
    pairs: List<BreedingPair>,
    clutches: List<BreedingClutch>,
    onApplyToPair: (BreedingPair, Long) -> Unit
) {
    var startEpochMs by remember { mutableLongStateOf(IncubationCalculator.normalizeToStartOfDay(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedPairMenu by remember { mutableStateOf(false) }
    var selectedPair by remember { mutableStateOf(pairs.firstOrNull { it.statusEnum == PairStatus.ACTIVE } ?: pairs.firstOrNull()) }

    val timeline = remember(startEpochMs) { IncubationCalculator.calculate(startEpochMs) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Kalkulator Estimasi Penetasan (21 Hari)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Pilih tanggal indukan betina mulai mengeram untuk menghitung jadwal teropong telur hingga hari estimasi menetas (+21 hari).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )

                Button(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("calculator_pick_date_btn")
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mulai Mengeram: ${IncubationCalculator.formatDate(startEpochMs)}")
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ESTIMASI TANGGAL MENETAS (H+21)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = IncubationCalculator.formatDate(timeline.expectedHatchEpochMs),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${timeline.currentPhaseLabel} • Sisa ${timeline.remainingDays} hari dari hari ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Milestone Timeline Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Jadwal Tahapan Pengeraman 21 Hari", style = MaterialTheme.typography.titleMedium)

                MilestoneRow("Hari ke-1", "Indukan Mulai Mengeram", IncubationCalculator.formatDate(timeline.startEpochMs))
                MilestoneRow("Hari ke-7", "Teropong Telur (Cek Fertilitas)", IncubationCalculator.formatDate(timeline.candlingDay7EpochMs))
                MilestoneRow("Hari ke-14", "Cek Perkembangan Embrio & Kelembapan", IncubationCalculator.formatDate(timeline.embryoCheckDay14EpochMs))
                MilestoneRow("Hari ke-20", "H-1 Persiapan Menetas & Siapkan Eggfood", IncubationCalculator.formatDate(timeline.hatchPrepDay20EpochMs))
                MilestoneRow("Hari ke-21", "Estimasi Telur Menetas!", IncubationCalculator.formatDate(timeline.expectedHatchEpochMs), highlight = true)
            }
        }

        // Quick apply to pair
        if (pairs.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Simpan Langsung ke Pasangan", style = MaterialTheme.typography.titleSmall)
                    Box {
                        OutlinedButton(
                            onClick = { selectedPairMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                selectedPair?.let { "Kandang ${it.cageCode}: ${it.sireRing} × ${it.damRing}" }
                                    ?: "Pilih Pasangan"
                            )
                        }
                        DropdownMenu(expanded = selectedPairMenu, onDismissRequest = { selectedPairMenu = false }) {
                            pairs.forEach { pair ->
                                DropdownMenuItem(
                                    text = { Text("Kandang ${pair.cageCode}: ${pair.sireName} × ${pair.damName}") },
                                    onClick = {
                                        selectedPair = pair
                                        selectedPairMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { selectedPair?.let { onApplyToPair(it, startEpochMs) } },
                        enabled = selectedPair != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mulai Catatan Pengeraman + Aktifkan Notifikasi")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startEpochMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        startEpochMs = IncubationCalculator.normalizeToStartOfDay(it)
                    }
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
private fun MilestoneRow(dayLabel: String, title: String, dateText: String, highlight: Boolean = false) {
    Surface(
        color = if (highlight) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlight) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
                )
            }
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePairDialog(
    canaries: List<Canary>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (cageCode: String, sire: Canary, dam: Canary, pairedDateMs: Long, notes: String) -> Unit
) {
    val eligible = remember(canaries) { canaries.filter { it.ownershipStatusEnum == OwnershipStatus.OWNED && it.inventoryStatusEnum == InventoryStatus.READY && it.healthStatusEnum != HealthStatus.CRITICAL } }
    val males = remember(eligible) { eligible.filter { it.sexEnum == CanarySex.MALE } }
    val females = remember(eligible) { eligible.filter { it.sexEnum == CanarySex.FEMALE } }

    var cageCode by remember { mutableStateOf("KDG-01") }
    var selectedSire by remember { mutableStateOf(males.firstOrNull()) }
    var selectedDam by remember { mutableStateOf(females.firstOrNull()) }
    var pairedDateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var sireMenu by remember { mutableStateOf(false) }
    var damMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val sharedAncestors = remember(selectedSire, selectedDam, canaries) {
        val s = selectedSire
        val d = selectedDam
        if (s != null && d != null) GenealogyAnalyzer.findSharedAncestors(s, d, canaries) else emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Pasangan Breeding") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (males.isEmpty() || females.isEmpty()) {
                    Text(
                        text = "Syarat pilihan: status READY, kepemilikan OWNED, dan kesehatan bukan CRITICAL. Atur status di tab Inventaris. Minimal 1 jantan dan 1 betina yang memenuhi syarat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = cageCode,
                    onValueChange = { cageCode = it },
                    label = { Text("Kode Kandang *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    OutlinedButton(onClick = { sireMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedSire?.let { "Jantan: ${it.ringCode} (${it.name})" } ?: "Pilih Pejantan")
                    }
                    DropdownMenu(expanded = sireMenu, onDismissRequest = { sireMenu = false }) {
                        males.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.ringCode} • ${m.name} (${m.breed})") },
                                onClick = {
                                    selectedSire = m
                                    sireMenu = false
                                }
                            )
                        }
                    }
                }

                Box {
                    OutlinedButton(onClick = { damMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedDam?.let { "Betina: ${it.ringCode} (${it.name})" } ?: "Pilih Induk Betina")
                    }
                    DropdownMenu(expanded = damMenu, onDismissRequest = { damMenu = false }) {
                        females.forEach { f ->
                            DropdownMenuItem(
                                text = { Text("${f.ringCode} • ${f.name} (${f.breed})") },
                                onClick = {
                                    selectedDam = f
                                    damMenu = false
                                }
                            )
                        }
                    }
                }

                if (selectedSire != null && selectedDam != null && selectedSire?.id == selectedDam?.id) {
                    Text("Tidak valid: burung yang sama tidak dapat dipasangkan.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (sharedAncestors.isNotEmpty()) {
                    Surface(
                        color = CriticalCrimsonBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = CriticalCrimson)
                            Text(
                                text = "Peringatan Inbreeding: Keduanya memiliki leluhur sama (${sharedAncestors.joinToString { it.ringCode }}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = CriticalCrimson
                            )
                        }
                    }
                }

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tanggal Jodoh: ${IncubationCalculator.formatDate(pairedDateMs)}")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Penjodohan") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = selectedSire ?: return@Button
                    val d = selectedDam ?: return@Button
                    onSubmit(cageCode, s, d, pairedDateMs, notes)
                },
                enabled = !isBusy && cageCode.isNotBlank() && selectedSire != null && selectedDam != null && selectedSire?.id != selectedDam?.id
            ) {
                Text(if (isBusy) "Menyimpan..." else "Simpan Pasangan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = pairedDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { pairedDateMs = it }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClutchFormDialog(
    pair: BreedingPair,
    initial: BreedingClutch?,
    defaultClutchNumber: Int,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        clutchNumber: Int,
        eggCount: Int,
        fertileCount: Int,
        hatchedCount: Int,
        fledgedCount: Int,
        incubationStartEpochMs: Long?,
        actualHatchEpochMs: Long?,
        status: ClutchStatus,
        notes: String
    ) -> Unit
) {
    var clutchNum by remember { mutableIntStateOf(initial?.clutchNumber?.toInt() ?: defaultClutchNumber) }
    var eggCount by remember { mutableIntStateOf(initial?.eggCount?.toInt() ?: 4) }
    var fertileCount by remember { mutableIntStateOf(initial?.fertileCount?.toInt() ?: 0) }
    var hatchedCount by remember { mutableIntStateOf(initial?.hatchedCount?.toInt() ?: 0) }
    var fledgedCount by remember { mutableIntStateOf(initial?.fledgedCount?.toInt() ?: 0) }
    var incubationStartMs by remember {
        mutableStateOf(initial?.incubationStartEpochMs ?: IncubationCalculator.normalizeToStartOfDay(System.currentTimeMillis()))
    }
    var hasIncubationDate by remember { mutableStateOf(initial?.incubationStartEpochMs != null || initial == null) }
    var status by remember { mutableStateOf(initial?.statusEnum ?: ClutchStatus.INCUBATING) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var actualHatchMs by remember { mutableStateOf(initial?.actualHatchEpochMs) }
    var showActualHatchPicker by remember { mutableStateOf(false) }

    val expectedHatch = if (hasIncubationDate) {
        IncubationCalculator.calculate(incubationStartMs).expectedHatchEpochMs
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Catat Periode Telur #${clutchNum}" else "Update Periode #${clutchNum}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Kandang ${pair.cageCode}: ${pair.sireRing} × ${pair.damRing}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("Status Periode", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ClutchStatus.LAYING, ClutchStatus.INCUBATING, ClutchStatus.HATCHED).forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.labelId) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ClutchStatus.COMPLETED, ClutchStatus.FAILED).forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.labelId) }
                            )
                        }
                    }
                }

                // Incubation Start Date (with automatic 21-day hatch estimate)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (hasIncubationDate) "Tanggal Bertelur / Mulai Mengeram: ${IncubationCalculator.formatDate(incubationStartMs)}"
                        else "Pilih Tanggal Bertelur / Mulai Mengeram"
                    )
                }

                OutlinedButton(onClick = { showActualHatchPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actualHatchMs?.let { "Tanggal Menetas Aktual: ${IncubationCalculator.formatDate(it)}" } ?: "Catat Tanggal Menetas Aktual (Opsional)")
                }

                expectedHatch?.let { expMs ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Estimasi Menetas (21 Hari): ${IncubationCalculator.formatDate(expMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                CountStepper("Jumlah Telur", eggCount, 0..12) { eggCount = it }
                CountStepper("Telur Fertil", fertileCount, 0..eggCount) { fertileCount = it }
                CountStepper("Menetas", hatchedCount, 0..eggCount) { hatchedCount = it }
                CountStepper("Anakan Sapih", fledgedCount, 0..hatchedCount.coerceAtLeast(eggCount)) { fledgedCount = it }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val actualHatch = if (status == ClutchStatus.HATCHED || status == ClutchStatus.COMPLETED) {
                        actualHatchMs ?: System.currentTimeMillis()
                    } else null
                    onSubmit(
                        clutchNum,
                        eggCount,
                        fertileCount.coerceAtMost(eggCount),
                        hatchedCount.coerceAtMost(eggCount),
                        fledgedCount.coerceAtMost(eggCount),
                        if (hasIncubationDate) incubationStartMs else null,
                        actualHatch,
                        status,
                        notes
                    )
                },
                enabled = !isBusy,
                modifier = Modifier.testTag("save_clutch_btn")
            ) {
                Text(if (isBusy) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showActualHatchPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = actualHatchMs ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showActualHatchPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { actualHatchMs = IncubationCalculator.normalizeToStartOfDay(it) }
                    showActualHatchPicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showActualHatchPicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = incubationStartMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        incubationStartMs = IncubationCalculator.normalizeToStartOfDay(it)
                        hasIncubationDate = true
                    }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun CountStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { if (value > range.first) onChange(value - 1) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("-") }
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { if (value < range.last) onChange(value + 1) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("+") }
        }
    }
}
