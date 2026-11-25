package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AviaryDashboardData

@Composable
fun TeamAndReportsScreen(
    data: AviaryDashboardData,
    isBusy: Boolean,
    onRenameAviary: (aviaryId: String, newName: String) -> Unit,
    onUpdateFarmSettings: (aviaryId: String?, farmName: String, farmAddress: String, farmPhone: String) -> Unit,
    onSwitchAviary: (aviaryId: String) -> Unit,
    onJoinAviaryByCode: (code: String, displayName: String, onDone: () -> Unit) -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    val context = LocalContext.current
    val activeAviary = data.activeAviary
    var showJoinDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    var farmName by remember(activeAviary?.id, activeAviary?.farmName, activeAviary?.name) {
        mutableStateOf(activeAviary?.farmName?.ifBlank { activeAviary.name } ?: "Kenari Tasikmalaya Farm")
    }
    var farmAddress by remember(activeAviary?.id, activeAviary?.farmAddress) {
        mutableStateOf(activeAviary?.farmAddress ?: "")
    }
    var farmPhone by remember(activeAviary?.id, activeAviary?.farmPhone) {
        mutableStateOf(activeAviary?.farmPhone ?: "")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Farm Settings Card (Nama Farm, Alamat, Nomor Telepon) - always visible
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("farm_settings_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Setting Profil Farm", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        text = "Informasi Farm akan tampil di Dasbor Kenari Tasikmalaya dan otomatis tercetak pada laporan PDF/Excel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = { Text("Nama Farm *") },
                        placeholder = { Text("Contoh: Kenari Tasikmalaya Farm") },
                        leadingIcon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farm_name_input")
                    )
                    OutlinedTextField(
                        value = farmAddress,
                        onValueChange = { farmAddress = it },
                        label = { Text("Alamat Farm") },
                        placeholder = { Text("Jl. Merdeka No. 12, Tasikmalaya...") },
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farm_address_input")
                    )
                    OutlinedTextField(
                        value = farmPhone,
                        onValueChange = { farmPhone = it },
                        label = { Text("Nomor Telepon Farm") },
                        placeholder = { Text("0812-xxxx-xxxx") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farm_phone_input")
                    )
                    Button(
                        onClick = {
                            onUpdateFarmSettings(activeAviary?.id, farmName, farmAddress, farmPhone)
                        },
                        enabled = !isBusy && farmName.trim().length >= 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_farm_settings_btn")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBusy) "Menyimpan..." else "Simpan Setting Farm")
                    }
                }
            }
        }

        // Automated PDF & Excel Reports Card
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
                    Text("Sistem Pelaporan Otomatis", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Kompilasi otomatis data silsilah (${data.canaries.size} kenari), riwayat pengeraman 21 hari (${data.clutches.size} periode), analitik bulanan, dan log kesehatan ke dokumen siap cetak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportPdf,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("export_pdf_button")
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("export_excel_button")
                        ) {
                            Icon(Icons.Rounded.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor Excel")
                        }
                    }
                }
            }
        }

        // Cloud Sync & Multi-Device Team Card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Tim Pengelola & Multi-Perangkat", style = MaterialTheme.typography.titleMedium)
                        }
                        if (activeAviary != null) {
                            IconButton(onClick = { showRenameDialog = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Ubah nama aviary")
                            }
                        }
                    }

                    Text(
                        text = "Semua data ternak tersinkronisasi otomatis ke Cloud Firestore. Bagikan kode undangan di bawah agar anggota tim di perangkat lain dapat mengakses dan mencatat data bersama secara real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (activeAviary != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "KODE UNDANGAN TIM (${activeAviary.name})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = activeAviary.inviteCode,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Kode Tim Aviary", activeAviary.inviteCode))
                                        Toast.makeText(context, "Kode undangan disalin", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Salin kode undangan",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Anggota Tim Aktif (${activeAviary.memberLabels.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        activeAviary.memberLabels.forEachIndexed { index, label ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (index == 0) "$label (Pemilik)" else label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showJoinDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_team_button")
                    ) {
                        Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gabung Aviary Tim Lain (Kode Undangan)")
                    }
                }
            }
        }

        // Switch Aviary workspace if member of > 1
        if (data.aviaries.size > 1) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Pindah Workspace Aviary", style = MaterialTheme.typography.titleSmall)
                        data.aviaries.forEach { aviary ->
                            val isCurrent = aviary.id == activeAviary?.id
                            OutlinedButton(
                                onClick = { if (!isCurrent) onSwitchAviary(aviary.id) },
                                enabled = !isCurrent,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCurrent) "${aviary.name} (Aktif)" else "Buka ${aviary.name}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        var codeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Gabung Tim Aviary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Masukkan kode undangan 8 karakter dari pemilik aviary untuk menyinkronkan data ternak secara real-time.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase() },
                        label = { Text("Kode Undangan") },
                        placeholder = { Text("Contoh: KNR7X9M2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onJoinAviaryByCode(codeInput, data.profile?.displayName ?: "Anggota Tim") {
                            showJoinDialog = false
                        }
                    },
                    enabled = !isBusy && codeInput.trim().length >= 4
                ) {
                    Text(if (isBusy) "Menghubungkan..." else "Gabung")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showRenameDialog && activeAviary != null) {
        var nameInput by remember { mutableStateOf(activeAviary.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Ubah Nama Aviary") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama Aviary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameAviary(activeAviary.id, nameInput)
                        showRenameDialog = false
                    },
                    enabled = nameInput.trim().length >= 2
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Batal") }
            }
        )
    }
}
