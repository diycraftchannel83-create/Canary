package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Canary
import com.example.data.model.CanarySex
import com.example.data.model.InventoryStatus
import com.example.data.model.OwnershipStatus
import com.example.data.model.OwnershipSource
import com.example.ui.viewmodel.AviaryDashboardData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryScreen(data: AviaryDashboardData, isBusy: Boolean, onUpdateMetadata: (Canary, InventoryStatus, OwnershipStatus, OwnershipSource, Long, String, Long, String, String) -> Unit, onCreateBird: (String, String, CanarySex, String, String, Long, String, InventoryStatus, OwnershipStatus, OwnershipSource, Long, String, Long, String, String, String, String, String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<InventoryStatus?>(null) }
    var ownershipFilter by remember { mutableStateOf<OwnershipStatus?>(null) }
    var editBird by remember { mutableStateOf<Canary?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val birds = remember(data.canaries, query, statusFilter, ownershipFilter) {
        data.canaries.filter { bird ->
            val q = query.trim().lowercase()
            val textMatch = q.isEmpty() || listOf(bird.name, bird.ringCode, bird.breed, bird.cageCode,
                bird.previousOwnerName, bird.sireRing.orEmpty(), bird.damRing.orEmpty(),
                bird.lineageNotes, bird.pedigreeDocumentNotes).any { it.lowercase().contains(q) }
            textMatch && (statusFilter == null || bird.inventoryStatusEnum == statusFilter) &&
                (ownershipFilter == null || bird.ownershipStatusEnum == ownershipFilter)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Inventaris Burung", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Button(onClick = { showAddDialog = true }) { Text("Tambah") }
            }
            Text("Kelola burung aviary, status kesiapan, asal, kepemilikan, dan pedigree.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InventoryMetric("Total", data.canaries.size.toString(), Modifier.weight(1f))
                InventoryMetric("Ready", data.canaries.count { it.inventoryStatusEnum == InventoryStatus.READY }.toString(), Modifier.weight(1f))
                InventoryMetric("Grower", data.canaries.count { it.inventoryStatusEnum == InventoryStatus.GROWER }.toString(), Modifier.weight(1f))
                InventoryMetric("Karantina", data.canaries.count { it.inventoryStatusEnum == InventoryStatus.QUARANTINE }.toString(), Modifier.weight(1f))
            }
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, placeholder = { Text("Cari ring, nama, strain, pemilik lama...") })
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("Semua Status") })
                InventoryStatus.entries.forEach { s ->
                    FilterChip(selected = statusFilter == s, onClick = { statusFilter = if (statusFilter == s) null else s }, label = { Text(s.labelId) })
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = ownershipFilter == null, onClick = { ownershipFilter = null }, label = { Text("Semua Kepemilikan") })
                OwnershipStatus.entries.forEach { s ->
                    FilterChip(selected = ownershipFilter == s, onClick = { ownershipFilter = if (ownershipFilter == s) null else s }, label = { Text(s.labelId) })
                }
            }
        }
        if (birds.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Text(if (data.canaries.isEmpty()) "Belum ada burung terdaftar. Tambahkan data melalui menu Silsilah." else "Tidak ada data yang cocok dengan filter.")
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(birds, key = { it.id }) { bird -> InventoryBirdCard(bird, onEdit = { editBird = bird }) }
            }
        }
    }
    if (showAddDialog) {
        AddInventoryBirdDialog(busy = isBusy, onDismiss = { showAddDialog = false }, onCreate = { ring, name, sex, breed, color, hatchMs, cage, status, ownership, source, acquiredMs, owner, price, confidence, sireRing, damRing, lineage, notes ->
            onCreateBird(ring, name, sex, breed, color, hatchMs, cage, status, ownership, source, acquiredMs, owner, price, confidence, sireRing, damRing, lineage, notes)
            showAddDialog = false
        })
    }
    editBird?.let { bird ->
        InventoryEditDialog(bird = bird, busy = isBusy, onDismiss = { editBird = null }, onSave = { status, ownership, source, dateMs, owner, price, confidence, notes ->
            onUpdateMetadata(bird, status, ownership, source, dateMs, owner, price, confidence, notes)
            editBird = null
        })
    }
}

@Composable
private fun InventoryMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(9.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InventoryBirdCard(bird: Canary, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(bird.name.ifBlank { "Tanpa nama" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ring ${bird.ringCode} • ${bird.sexEnum.labelId} • ${bird.breed}", style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text(bird.inventoryStatusEnum.labelId, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    Text(bird.ownershipStatusEnum.labelId, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("Kandang: ${bird.cageCode.ifBlank { "—" }} • Kesehatan: ${bird.healthStatusEnum.labelId} (${bird.healthScore}/100)", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Asal: ${bird.ownershipSourceEnum.labelId}", style = MaterialTheme.typography.labelSmall)
            if (bird.previousOwnerName.isNotBlank()) Text("Pemilik sebelumnya: ${bird.previousOwnerName}", style = MaterialTheme.typography.bodySmall)
            Text("Silsilah: ${bird.pedigreeConfidence} • Ayah: ${bird.sireRing ?: "belum tercatat"} • Ibu: ${bird.damRing ?: "belum tercatat"}", style = MaterialTheme.typography.bodySmall)
            if (bird.lineageNotes.isNotBlank()) Text("Catatan silsilah: ${bird.lineageNotes}", style = MaterialTheme.typography.bodySmall)
            if (bird.pedigreeDocumentNotes.isNotBlank()) Text("Dokumen/riwayat: ${bird.pedigreeDocumentNotes}", style = MaterialTheme.typography.bodySmall)
            Text("Produksi: ${bird.recordedClutchCount} clutch • ${bird.recordedEggCount} telur • ${bird.recordedHatchedCount} menetas • ${bird.recordedFledgedCount} sapih", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit inventaris & pedigree") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryEditDialog(bird: Canary, busy: Boolean, onDismiss: () -> Unit,
    onSave: (InventoryStatus, OwnershipStatus, OwnershipSource, Long, String, Long, String, String) -> Unit) {
    var status by remember { mutableStateOf(bird.inventoryStatusEnum) }
    var ownership by remember { mutableStateOf(bird.ownershipStatusEnum) }
    var source by remember { mutableStateOf(bird.ownershipSourceEnum) }
    var acquiredDate by remember { mutableStateOf(if (bird.acquiredDateEpochMs > 0) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(bird.acquiredDateEpochMs)) else "") }
    var previousOwner by remember { mutableStateOf(bird.previousOwnerName) }
    var price by remember { mutableStateOf(bird.purchasePrice.toString()) }
    var confidence by remember { mutableStateOf(bird.pedigreeConfidence) }
    var notes by remember { mutableStateOf(bird.pedigreeDocumentNotes) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Inventaris — ${bird.ringCode}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 580.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InventoryDropdown("Status kesiapan", status.labelId, InventoryStatus.entries.map { it.labelId }, { index -> status = InventoryStatus.entries[index] })
                InventoryDropdown("Kepemilikan", ownership.labelId, OwnershipStatus.entries.map { it.labelId }, { index -> ownership = OwnershipStatus.entries[index] })
                InventoryDropdown("Asal burung", source.labelId, OwnershipSource.entries.map { it.labelId }, { index -> source = OwnershipSource.entries[index] })
                OutlinedTextField(acquiredDate, { acquiredDate = it }, label = { Text("Tanggal diperoleh (YYYY-MM-DD)") }, singleLine = true)
                OutlinedTextField(previousOwner, { previousOwner = it }, label = { Text("Breeder / pemilik sebelumnya") }, singleLine = true)
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Harga beli (angka)") }, singleLine = true)
                InventoryDropdown("Kelengkapan pedigree", confidence, listOf("PARTIAL", "FULL", "UNKNOWN"), { confidence = listOf("PARTIAL", "FULL", "UNKNOWN")[it] })
                OutlinedTextField(notes, { notes = it }, label = { Text("Catatan dokumen / riwayat pedigree") }, minLines = 2)
            }
        },
        confirmButton = { Button(enabled = !busy, onClick = {
            val dateMs = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(acquiredDate)?.time ?: bird.acquiredDateEpochMs }.getOrDefault(bird.acquiredDateEpochMs)
            onSave(status, ownership, source, dateMs, previousOwner.trim(), price.toLongOrNull() ?: 0L, confidence, notes.trim())
        }) { Text(if (busy) "Menyimpan..." else "Simpan") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Batal") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryDropdown(label: String, selected: String, options: List<String>, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelected(index); expanded = false })
            }
        }
    }
}
