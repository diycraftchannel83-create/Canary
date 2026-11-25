package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BreedingClutch
import com.example.data.model.BreedingPair
import com.example.data.model.Canary
import com.example.data.model.CanarySex
import com.example.ui.theme.FemaleRose
import com.example.ui.theme.FemaleRoseBg
import com.example.ui.theme.MaleBlue
import com.example.ui.theme.MaleBlueBg
import com.example.utils.CanaryPhotoUtils
import com.example.utils.GenealogyAnalyzer
import com.example.utils.IncubationCalculator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PedigreeTreeBottomSheet(
    subject: Canary,
    allCanaries: List<Canary>,
    pairs: List<BreedingPair> = emptyList(),
    clutches: List<BreedingClutch> = emptyList(),
    onSelectCanary: (Canary) -> Unit,
    onEditCanary: ((Canary) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tree = GenealogyAnalyzer.buildPedigreeTree(subject, allCanaries)
    val lineColor = MaterialTheme.colorScheme.outline
    val relatedPairs = remember(pairs, subject.id) {
        pairs.filter { it.sireId == subject.id || it.damId == subject.id }
    }
    val relatedClutches = remember(clutches, subject.id) {
        clutches.filter { it.sireId == subject.id || it.damId == subject.id }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Detail Indukan, Silsilah & Produksi",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Ketuk leluhur atau anakan untuk menelusuri silsilah",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    onEditCanary?.let { editFn ->
                        IconButton(onClick = { editFn(subject) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Kelola atau edit indukan")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup silsilah")
                    }
                }
            }

            // Generation 2: Grandparents (Kakek & Nenek)
            Text(
                text = "Generasi 2 (Kakek & Nenek)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PedigreeMiniNode(
                    label = "Kakek (J)",
                    canary = tree.paternalGrandsire,
                    ringFallback = tree.sire?.sireRing,
                    onClick = { tree.paternalGrandsire?.let(onSelectCanary) },
                    modifier = Modifier.weight(1f)
                )
                PedigreeMiniNode(
                    label = "Nenek (J)",
                    canary = tree.paternalGranddam,
                    ringFallback = tree.sire?.damRing,
                    onClick = { tree.paternalGranddam?.let(onSelectCanary) },
                    modifier = Modifier.weight(1f)
                )
                PedigreeMiniNode(
                    label = "Kakek (B)",
                    canary = tree.maternalGrandsire,
                    ringFallback = tree.dam?.sireRing,
                    onClick = { tree.maternalGrandsire?.let(onSelectCanary) },
                    modifier = Modifier.weight(1f)
                )
                PedigreeMiniNode(
                    label = "Nenek (B)",
                    canary = tree.maternalGranddam,
                    ringFallback = tree.dam?.damRing,
                    onClick = { tree.maternalGranddam?.let(onSelectCanary) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Connector lines Gen2 -> Gen1
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                val w = size.width
                val h = size.height
                drawLine(lineColor, Offset(w * 0.125f, 0f), Offset(w * 0.25f, h), strokeWidth = 3f)
                drawLine(lineColor, Offset(w * 0.375f, 0f), Offset(w * 0.25f, h), strokeWidth = 3f)
                drawLine(lineColor, Offset(w * 0.625f, 0f), Offset(w * 0.75f, h), strokeWidth = 3f)
                drawLine(lineColor, Offset(w * 0.875f, 0f), Offset(w * 0.75f, h), strokeWidth = 3f)
            }

            // Generation 1: Sire & Dam
            Text(
                text = "Generasi 1 (Indukan Langsung)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PedigreeParentCard(
                    roleLabel = "Induk Jantan (Sire)",
                    canary = tree.sire,
                    ringFallback = subject.sireRing,
                    isMale = true,
                    onClick = { tree.sire?.let(onSelectCanary) },
                    modifier = Modifier.weight(1f)
                )
                PedigreeParentCard(
                    roleLabel = "Induk Betina (Dam)",
                    canary = tree.dam,
                    ringFallback = subject.damRing,
                    isMale = false,
                    onClick = { tree.dam?.let(onSelectCanary) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Connector lines Gen1 -> Subject
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                val w = size.width
                val h = size.height
                drawLine(lineColor, Offset(w * 0.25f, 0f), Offset(w * 0.5f, h), strokeWidth = 4f)
                drawLine(lineColor, Offset(w * 0.75f, 0f), Offset(w * 0.5f, h), strokeWidth = 4f)
            }

            // Subject Canary Card
            val subjectPhoto = remember(subject.photoBase64) {
                CanaryPhotoUtils.decodeBase64ToImageBitmap(subject.photoBase64)
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (subjectPhoto != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = subjectPhoto,
                                contentDescription = "Foto asli indukan ${subject.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Foto Indukan Asli Terkunci",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = subject.ringCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "${subject.sexEnum.labelId} • ${subject.breed}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (subjectPhoto == null) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Warna: ${subject.featherColor.ifBlank { "-" }}  |  Menetas: ${IncubationCalculator.formatDate(subject.hatchDateEpochMs)}  |  Skor Kesehatan: ${subject.healthScore}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    if (subject.lineageNotes.isNotBlank()) {
                        Text(
                            text = "Trah / Silsilah: ${subject.lineageNotes}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (subject.notes.isNotBlank()) {
                        Text(
                            text = "Catatan: ${subject.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    onEditCanary?.let { editFn ->
                        OutlinedButton(
                            onClick = { editFn(subject) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kelola / Edit Data Indukan")
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Riwayat Produksi Indukan (Recorded fields + Live linked clutches)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Egg,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Riwayat Produksi Indukan",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            val totalClutches = subject.recordedClutchCount.toInt() + relatedClutches.size
            val totalEggs = subject.recordedEggCount.toInt() + relatedClutches.sumOf { it.eggCount.toInt() }
            val totalHatched = subject.recordedHatchedCount.toInt() + relatedClutches.sumOf { it.hatchedCount.toInt() }
            val totalFledged = subject.recordedFledgedCount.toInt() + relatedClutches.sumOf { it.fledgedCount.toInt() }
            val hatchPct = if (totalEggs > 0) (totalHatched * 100 / totalEggs) else 0

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Akumulasi Produksi: $totalClutches Periode  •  $totalEggs Telur  •  $totalHatched Menetas ($hatchPct%)  •  $totalFledged Sapih",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (subject.recordedClutchCount > 0 || subject.recordedEggCount > 0) {
                        Text(
                            text = "Riwayat Manual Tercatat: ${subject.recordedClutchCount} Periode, ${subject.recordedEggCount} Telur, ${subject.recordedHatchedCount} Menetas, ${subject.recordedFledgedCount} Sapih",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (subject.productionHistoryNotes.isNotBlank()) {
                        Text(
                            text = "Catatan Produksi: ${subject.productionHistoryNotes}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (relatedPairs.isNotEmpty()) {
                        Text(
                            text = "Pasangan Terdaftar: ${relatedPairs.joinToString(", ") { "${it.sireRing} × ${it.damRing} (${it.cageCode})" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (relatedClutches.isNotEmpty()) {
                        relatedClutches.take(5).forEach { cl ->
                            Text(
                                text = "• Kandang ${cl.cageCode} Periode #${cl.clutchNumber} [${cl.statusEnum.labelId}]: ${cl.eggCount} telur, ${cl.hatchedCount} menetas, ${cl.fledgedCount} sapih",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else if (subject.recordedClutchCount == 0L && subject.recordedEggCount == 0L && subject.productionHistoryNotes.isBlank()) {
                        Text(
                            text = "Belum ada riwayat produksi tercatat untuk indukan ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Offspring (Anakan)
            Text(
                text = "Keturunan / Anakan Terdaftar (${tree.offspring.size})",
                style = MaterialTheme.typography.titleSmall
            )
            if (tree.offspring.isEmpty()) {
                Text(
                    text = "Belum ada anakan yang tertaut ke indukan ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tree.offspring.forEach { child ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.clickable { onSelectCanary(child) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (child.sexEnum == CanarySex.MALE) Icons.Rounded.Male else Icons.Rounded.Female,
                                    contentDescription = null,
                                    tint = if (child.sexEnum == CanarySex.MALE) MaleBlue else FemaleRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = child.name,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${child.ringCode} • ${child.breed}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PedigreeMiniNode(
    label: String,
    canary: Canary?,
    ringFallback: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .then(if (canary != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (canary != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = if (canary != null) 2.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = canary?.name ?: ringFallback?.takeIf { it.isNotBlank() } ?: "Tidak tercatat",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (canary != null) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (canary != null) {
                Text(
                    text = canary.ringCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PedigreeParentCard(
    roleLabel: String,
    canary: Canary?,
    ringFallback: String?,
    isMale: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg: Color = if (isMale) MaleBlueBg.copy(alpha = 0.65f) else FemaleRoseBg.copy(alpha = 0.65f)
    val accent: Color = if (isMale) MaleBlue else FemaleRose

    Surface(
        modifier = modifier
            .then(if (canary != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (canary != null || !ringFallback.isNullOrBlank()) bg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isMale) Icons.Rounded.Male else Icons.Rounded.Female,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = roleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
            Text(
                text = canary?.name ?: if (!ringFallback.isNullOrBlank()) "Ring Eksternal" else "Belum ditautkan",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = canary?.let { "${it.ringCode} • ${it.breed}" }
                    ?: ringFallback?.takeIf { it.isNotBlank() }
                    ?: "-",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
