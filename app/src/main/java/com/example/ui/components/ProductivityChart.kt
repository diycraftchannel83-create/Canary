package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Canary
import com.example.export.MonthlyProductivityStat
import com.example.ui.theme.CanaryGold
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.SageAccent

@Composable
fun MonthlyProductivityChartCard(
    stats: List<MonthlyProductivityStat>,
    canaries: List<Canary>,
    selectedCanaryId: String?,
    onSelectCanaryFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var selectedMonthIndex by remember { mutableIntStateOf((stats.size - 1).coerceAtLeast(0)) }

    val selectedCanary = canaries.firstOrNull { it.id == selectedCanaryId }
    val filterLabel = selectedCanary?.let { "${it.ringCode} (${it.name})" } ?: "Semua Indukan"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grafik Produktivitas Bulanan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Performa telur, fertil, menetas & sapih (6 bulan)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    OutlinedButton(
                        onClick = { filterMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Filter indukan",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedCanary != null) selectedCanary.ringCode else "Semua",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Indukan") },
                            onClick = {
                                onSelectCanaryFilter(null)
                                filterMenuExpanded = false
                            }
                        )
                        canaries.forEach { canary ->
                            DropdownMenuItem(
                                text = { Text("${canary.ringCode} • ${canary.name} (${canary.sexEnum.labelId})") },
                                onClick = {
                                    onSelectCanaryFilter(canary.id)
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedCanary != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Menampilkan performa indukan: $filterLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            val maxVal = (stats.maxOfOrNull { maxOf(it.eggs, it.fertile, it.hatched, it.fledged) } ?: 0)
                .coerceAtLeast(4)
            val hasAnyData = stats.any { it.eggs > 0 || it.hatched > 0 || it.fledged > 0 }

            if (!hasAnyData) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Belum ada data produksi pada filter ini",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Catat periode pengeraman di tab Produksi untuk melihat tren grafik bulanan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val barColorEggs = CanaryGold
                val barColorFertile = SageAccent
                val barColorHatched = ForestEmerald
                val barColorFledged = HealthGreen
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .padding(top = 8.dp)
                ) {
                    val chartHeight = size.height - 16f
                    val groupWidth = size.width / stats.size.coerceAtLeast(1)

                    // Horizontal dashed gridlines
                    val steps = 4
                    for (i in 0..steps) {
                        val y = chartHeight - (chartHeight * i / steps)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }

                    stats.forEachIndexed { idx, stat ->
                        val groupStart = idx * groupWidth
                        val innerPad = groupWidth * 0.16f
                        val usableWidth = groupWidth - innerPad * 2
                        val barWidth = (usableWidth / 4.6f).coerceAtLeast(6f)
                        val gap = barWidth * 0.2f

                        val values = listOf(
                            stat.eggs to barColorEggs,
                            stat.fertile to barColorFertile,
                            stat.hatched to barColorHatched,
                            stat.fledged to barColorFledged
                        )

                        values.forEachIndexed { bIdx, (v, color) ->
                            val ratio = (v.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                            val barH = (chartHeight * ratio).coerceAtLeast(if (v > 0) 6f else 2f)
                            val x = groupStart + innerPad + bIdx * (barWidth + gap)
                            val y = chartHeight - barH
                            drawRoundRect(
                                color = if (idx == selectedMonthIndex) color else color.copy(alpha = 0.78f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }

                // Month interactive pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    stats.forEachIndexed { idx, stat ->
                        val isSelected = idx == selectedMonthIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable { selectedMonthIndex = idx }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stat.monthLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Selected Month detail strip
                val activeStat = stats.getOrNull(selectedMonthIndex)
                if (activeStat != null) {
                    val hatchPct = if (activeStat.eggs > 0) {
                        (activeStat.hatched * 100 / activeStat.eggs)
                    } else 0
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeStat.monthLabel}: ${activeStat.eggs} Telur • ${activeStat.fertile} Fertil • ${activeStat.hatched} Menetas • ${activeStat.fledged} Sapih",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$hatchPct%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ChartLegendItem("Telur", CanaryGold)
                ChartLegendItem("Fertil", SageAccent)
                ChartLegendItem("Menetas", ForestEmerald)
                ChartLegendItem("Sapih", HealthGreen)
            }
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
