package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Canary
import com.example.data.model.InventoryStatus
import com.example.data.model.OwnershipStatus
import com.example.ui.viewmodel.AviaryDashboardData

@Composable
fun InventoryScreen(data: AviaryDashboardData) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<InventoryStatus?>(null) }
    var ownershipFilter by remember { mutableStateOf<OwnershipStatus?>(null) }
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
            Text("Inventaris Burung", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Kelola burung milik aviary, asal pembelian, status, dan data silsilah.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InventoryMetric("Total", data.canaries.size.toString(), Modifier.weight(1f))
                InventoryMetric("Siap", data.canaries.count { it.inventoryStatusEnum == InventoryStatus.READY }.toString(), Modifier.weight(1f))
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
                items(birds, key = { it.id }) { bird -> InventoryBirdCard(bird) }
            }
        }
    }
}

@Composable
private fun InventoryMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InventoryBirdCard(bird: Canary) {
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
        }
    }
}
