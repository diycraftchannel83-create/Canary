package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.AdMobManager
import com.example.ads.KenariAdaptiveBanner
import com.example.ads.findHostActivity
import com.example.data.model.Canary
import com.example.data.repository.BreedingRepository
import com.example.notifications.BreedingNotificationHelper
import com.example.ui.viewmodel.AviaryDashboardData
import kotlinx.coroutines.launch
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutFromApp
import com.example.ui.screens.BreedingScreen
import com.example.ui.screens.CareScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GenealogyScreen
import com.example.ui.screens.TeamAndReportsScreen
import com.example.ui.theme.KenariPedigreeTheme
import com.example.ui.viewmodel.AviaryViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Dasbor", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    GENEALOGY("genealogy", "Silsilah", Icons.Rounded.AccountTree, Icons.Outlined.AccountTree),
    BREEDING("breeding", "Produksi", Icons.Rounded.Egg, Icons.Outlined.Egg),
    CARE("care", "Perawatan", Icons.Rounded.HealthAndSafety, Icons.Outlined.HealthAndSafety),
    TEAM_REPORTS("team_reports", "Setting", Icons.Rounded.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            BreedingNotificationHelper.ensureChannels(this)
        } catch (e: Exception) {
            android.util.Log.w("KenariStartup", "Notification channels could not be initialized", e)
        }
        setContent {
            KenariPedigreeTheme {
                KenariPedigreeApp()
            }
        }
    }
}

@Composable
fun KenariPedigreeApp() {
    // Prevent a missing Firebase configuration from crashing the app immediately.
    val firebaseConfigured = remember {
        try {
            FirebaseApp.getInstance()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
    if (!firebaseConfigured) {
        FirebaseConfigurationRequiredScreen()
        return
    }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedAviaryShell(currentUserId = user.uid)
        }
    }
}

@Composable
private fun FirebaseConfigurationRequiredScreen() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Konfigurasi Firebase belum tersedia", style = MaterialTheme.typography.titleLarge)
            Text(
                "Aplikasi dapat dibuka, tetapi login Google dan sinkronisasi cloud belum dapat digunakan. " +
                    "Tambahkan google-services.json untuk aplikasi com.winsmediatama.kenaritasik, " +
                    "kemudian build ulang.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AuthenticatedAviaryShell(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AviaryViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AviaryViewModel(BreedingRepository(db), currentUserId)
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val selectedCanaryFilterId by viewModel.selectedCanaryFilterId.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    var hasNotifPermission by remember {
        mutableStateOf(BreedingNotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    // Schedule daily supplement alarm whenever profile preferences update
    val profile = (dashboardState as? UiState.Success)?.data?.profile
    LaunchedEffect(profile?.supplementReminderEnabled, profile?.supplementHour, profile?.supplementMinute) {
        if (profile != null) {
            BreedingNotificationHelper.scheduleDailySupplementReminder(
                context = context,
                enabled = profile.supplementReminderEnabled,
                hour = profile.supplementHour.toInt(),
                minute = profile.supplementMinute.toInt()
            )
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppTab.DASHBOARD.route

    var healthTargetCanary by remember { mutableStateOf<Canary?>(null) }
    var pendingExportRequest by remember { mutableStateOf<Pair<AviaryDashboardData, Boolean>?>(null) }

    fun handleExportRequest(exportData: AviaryDashboardData, isExcel: Boolean) {
        if (AdMobManager.isRewardedInterstitialAvailable()) {
            pendingExportRequest = exportData to isExcel
        } else {
            AdMobManager.loadRewardedInterstitialAd(context)
            viewModel.exportAndShareReport(context, exportData, isExcel = isExcel)
        }
    }

    fun navigateToTab(tab: AppTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                val showBanner = currentRoute in setOf(
                    AppTab.DASHBOARD.route,
                    AppTab.BREEDING.route,
                    AppTab.CARE.route
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showBanner) {
                        KenariAdaptiveBanner()
                    }
                    if (!isExpanded) {
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            AppTab.entries.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 920.dp).fillMaxSize()) {
                        when (val state = dashboardState) {
                            is UiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            text = "Menyinkronkan data aviary dari Cloud...",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            is UiState.Error -> {
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
                                        Text(
                                            text = "Gagal Terhubung ke Cloud",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Button(onClick = { signOutFromApp(credentialManager, scope) }) {
                                            Text("Keluar & Masuk Ulang")
                                        }
                                    }
                                }
                            }

                            is UiState.Success -> {
                                val data = state.data
                                val activeAviary = data.activeAviary
                                val monthlyStats = remember(data.clutches, selectedCanaryFilterId) {
                                    viewModel.computeMonthlyProductivity(data.clutches, selectedCanaryFilterId)
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = AppTab.DASHBOARD.route,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable(AppTab.DASHBOARD.route) {
                                        DashboardScreen(
                                            data = data,
                                            monthlyStats = monthlyStats,
                                            selectedCanaryFilterId = selectedCanaryFilterId,
                                            hasNotificationPermission = hasNotifPermission,
                                            isBusy = isBusy,
                                            onRequestNotificationPermission = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            },
                                            onSelectCanaryFilter = viewModel::setCanaryFilter,
                                            onToggleSupplementToday = { task, done ->
                                                activeAviary?.let {
                                                    viewModel.toggleSupplementDoneToday(it.id, task, done)
                                                }
                                            },
                                            onSaveFarmSettings = { aviaryId, fName, fAddr, fPhone, onDone ->
                                                viewModel.updateFarmSettings(aviaryId, fName, fAddr, fPhone, onDone)
                                            },
                                            onNavigateToCare = { navigateToTab(AppTab.CARE) },
                                            onNavigateToBreeding = { navigateToTab(AppTab.BREEDING) },
                                            onExportPdf = { handleExportRequest(data, isExcel = false) },
                                            onExportExcel = { handleExportRequest(data, isExcel = true) },
                                            onSignOut = { signOutFromApp(credentialManager, scope) }
                                        )
                                    }

                                    composable(AppTab.GENEALOGY.route) {
                                        GenealogyScreen(
                                            data = data,
                                            isBusy = isBusy,
                                            onSaveCanary = { existingId, existingHasPhoto, ring, name, sex, breed, color, hatchMs, sId, dId, sRing, dRing, cage, bStatus, hStatus, hScore, notes, lineageNotes, recordedClutchCount, recordedEggCount, recordedHatchedCount, recordedFledgedCount, productionHistoryNotes, newPhotoBase64, onDone ->
                                                val av = activeAviary ?: return@GenealogyScreen
                                                viewModel.saveCanary(
                                                    aviary = av,
                                                    existingId = existingId,
                                                    existingHasPhoto = existingHasPhoto,
                                                    ringCode = ring,
                                                    name = name,
                                                    sex = sex,
                                                    breed = breed,
                                                    featherColor = color,
                                                    hatchDateEpochMs = hatchMs,
                                                    sireId = sId,
                                                    damId = dId,
                                                    sireRing = sRing,
                                                    damRing = dRing,
                                                    cageCode = cage,
                                                    breedingStatus = bStatus,
                                                    healthStatus = hStatus,
                                                    healthScore = hScore,
                                                    notes = notes,
                                                    lineageNotes = lineageNotes,
                                                    recordedClutchCount = recordedClutchCount,
                                                    recordedEggCount = recordedEggCount,
                                                    recordedHatchedCount = recordedHatchedCount,
                                                    recordedFledgedCount = recordedFledgedCount,
                                                    productionHistoryNotes = productionHistoryNotes,
                                                    newPhotoBase64 = newPhotoBase64,
                                                    onDone = onDone
                                                )
                                            },
                                            onDeleteCanary = { canaryId ->
                                                activeAviary?.let { viewModel.deleteCanary(it.id, canaryId) }
                                            },
                                            onOpenHealthCheck = { bird ->
                                                healthTargetCanary = bird
                                                navigateToTab(AppTab.CARE)
                                            }
                                        )
                                    }

                                    composable(AppTab.BREEDING.route) {
                                        BreedingScreen(
                                            data = data,
                                            isBusy = isBusy,
                                            onCreatePair = { cage, sire, dam, dateMs, notes, onDone ->
                                                val av = activeAviary ?: return@BreedingScreen
                                                viewModel.createPair(av, cage, sire, dam, dateMs, notes, onDone)
                                            },
                                            onUpdatePairStatus = { pairId, status ->
                                                activeAviary?.let { viewModel.updatePairStatus(it.id, pairId, status) }
                                            },
                                            onSaveClutch = { existingId, pair, num, eggs, fertile, hatched, fledged, incStart, actualHatch, status, notes, onDone ->
                                                val av = activeAviary ?: return@BreedingScreen
                                                viewModel.saveClutch(
                                                    context = context,
                                                    aviary = av,
                                                    profile = data.profile,
                                                    existingClutchId = existingId,
                                                    pair = pair,
                                                    clutchNumber = num,
                                                    eggCount = eggs,
                                                    fertileCount = fertile,
                                                    hatchedCount = hatched,
                                                    fledgedCount = fledged,
                                                    incubationStartEpochMs = incStart,
                                                    actualHatchEpochMs = actualHatch,
                                                    status = status,
                                                    notes = notes,
                                                    onDone = onDone
                                                )
                                            },
                                            onDeleteClutch = { clutchId ->
                                                activeAviary?.let { viewModel.deleteClutch(it.id, clutchId) }
                                            }
                                        )
                                    }

                                    composable(AppTab.CARE.route) {
                                        CareScreen(
                                            data = data,
                                            isBusy = isBusy,
                                            initialHealthTarget = healthTargetCanary,
                                            onClearInitialHealthTarget = { healthTargetCanary = null },
                                            onRecordHealthCheck = { bird, score, status, symptoms, action, onDone ->
                                                val av = activeAviary ?: return@CareScreen
                                                viewModel.recordHealthCheck(
                                                    context = context,
                                                    aviary = av,
                                                    profile = data.profile,
                                                    canary = bird,
                                                    newScore = score,
                                                    healthStatus = status,
                                                    symptoms = symptoms,
                                                    actionTaken = action,
                                                    onDone = onDone
                                                )
                                            },
                                            onAddSupplement = { title, dosage, group, time, onDone ->
                                                val av = activeAviary ?: return@CareScreen
                                                viewModel.addSupplementTask(av, title, dosage, group, time, onDone)
                                            },
                                            onToggleSupplementToday = { task, done ->
                                                activeAviary?.let { viewModel.toggleSupplementDoneToday(it.id, task, done) }
                                            },
                                            onDeleteSupplement = { taskId ->
                                                activeAviary?.let { viewModel.deleteSupplementTask(it.id, taskId) }
                                            },
                                            onUpdateNotificationPreferences = { suppEn, hour, min, incEn, thresh ->
                                                viewModel.updateNotificationPreferences(
                                                    context = context,
                                                    supplementEnabled = suppEn,
                                                    supplementHour = hour,
                                                    supplementMinute = min,
                                                    incubationEnabled = incEn,
                                                    healthAlertThreshold = thresh
                                                )
                                            }
                                        )
                                    }

                                    composable(AppTab.TEAM_REPORTS.route) {
                                        TeamAndReportsScreen(
                                            data = data,
                                            isBusy = isBusy,
                                            onRenameAviary = viewModel::renameAviary,
                                            onUpdateFarmSettings = { aviaryId, fName, fAddr, fPhone ->
                                                viewModel.updateFarmSettings(aviaryId, fName, fAddr, fPhone)
                                            },
                                            onSwitchAviary = viewModel::switchAviary,
                                            onJoinAviaryByCode = viewModel::joinAviaryByCode,
                                            onExportPdf = { handleExportRequest(data, isExcel = false) },
                                            onExportExcel = { handleExportRequest(data, isExcel = true) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        pendingExportRequest?.let { (exportData, isExcel) ->
            val formatLabel = if (isExcel) "Excel (.xlsx)" else "PDF"
            AlertDialog(
                onDismissRequest = { pendingExportRequest = null },
                title = { Text("Ekspor Laporan $formatLabel") },
                text = {
                    Text(
                        "Tonton iklan singkat untuk mendukung Kenari Tasikmalaya sekaligus mengunduh laporan $formatLabel aviary Anda."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingExportRequest = null
                            val hostActivity = context.findHostActivity()
                            if (hostActivity == null) {
                                viewModel.exportAndShareReport(context, exportData, isExcel = isExcel)
                            } else {
                                AdMobManager.showRewardedInterstitialAd(
                                    activity = hostActivity,
                                    onRewardEarned = {
                                        viewModel.exportAndShareReport(context, exportData, isExcel = isExcel)
                                    },
                                    onDismissedWithoutReward = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Tonton iklan hingga selesai untuk mengekspor laporan $formatLabel."
                                            )
                                        }
                                    },
                                    onAdUnavailableOrFailed = {
                                        viewModel.exportAndShareReport(context, exportData, isExcel = isExcel)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.testTag("confirm_rewarded_ad_export_btn")
                    ) {
                        Text("Tonton Iklan & Ekspor")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingExportRequest = null },
                        modifier = Modifier.testTag("cancel_rewarded_ad_export_btn")
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
