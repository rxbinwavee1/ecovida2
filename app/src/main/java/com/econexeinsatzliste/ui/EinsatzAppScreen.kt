package com.econexeinsatzliste.ui

import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.econexeinsatzliste.data.Einsatz
import com.econexeinsatzliste.data.EinsatzStatus
import com.econexeinsatzliste.ui.components.CsvImportDialog
import com.econexeinsatzliste.ui.components.EinsatzCard
import com.econexeinsatzliste.ui.components.EinsatzDialog
import com.econexeinsatzliste.ui.components.EinsatzHeaderStats
import com.econexeinsatzliste.ui.components.RescheduleDialog
import com.econexeinsatzliste.ui.components.ScreenshotPreviewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinsatzAppScreen(
    viewModel: EinsatzViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showManualDialog by remember { mutableStateOf(false) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }
    var editingEinsatz by remember { mutableStateOf<Einsatz?>(null) }
    var rescheduleEinsatzTarget by remember { mutableStateOf<Einsatz?>(null) }

    // Permission Launcher for Notifications (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Image Picker for Screenshots
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeScreenshot(context, uri)
        }
    }

    // File Picker for CSV files
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeCsvUri(context, uri)
        }
    }

    // Show Snackbars for user messages
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Econex Planer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkTheme() }) {
                        AnimatedContent(
                            targetState = uiState.isDarkTheme,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.7f)) togetherWith
                                        (fadeOut(animationSpec = tween(350)) + scaleOut(targetScale = 0.7f))
                            },
                            label = "ThemeSwitchAnimation"
                        ) { isDark ->
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Theme umschalten"
                            )
                        }
                    }
                    IconButton(onClick = { showPurgeConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Alle Einsätze löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = { viewModel.loadEinsatze() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aktualisieren"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Top 3 Action Buttons: Screenshot | CSV | Manuell (Equal Size & Compact Padding)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Screenshot",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = { showCsvDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "CSV",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = {
                        editingEinsatz = null
                        showManualDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Manuell",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header Stats Summary
            EinsatzHeaderStats(
                totalCount = uiState.einsatze.count { it.status != EinsatzStatus.ABGESCHLOSSEN },
                totalEarnings = uiState.totalEarnings
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Tabs: Aktive Einsätze vs Archiv
            val activeCount = uiState.einsatze.count { it.status != EinsatzStatus.ABGESCHLOSSEN }
            val archiveCount = uiState.einsatze.count { it.status == EinsatzStatus.ABGESCHLOSSEN }

            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.activeTab == EinsatzTab.AKTIV,
                    onClick = { viewModel.setActiveTab(EinsatzTab.AKTIV) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aktive Einsätze ($activeCount)", fontWeight = FontWeight.Bold)
                        }
                    }
                )

                Tab(
                    selected = uiState.activeTab == EinsatzTab.ARCHIV,
                    onClick = { viewModel.setActiveTab(EinsatzTab.ARCHIV) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Archiv ($archiveCount)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Filter Chips (For Active Tab)
            if (uiState.activeTab == EinsatzTab.AKTIV) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedStatusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("Alle ($activeCount)") }
                    )

                    listOf(EinsatzStatus.OFFEN, EinsatzStatus.VERSCHOBEN, EinsatzStatus.ABGEBROCHEN).forEach { status ->
                        val count = uiState.einsatze.count { it.status == status }
                        FilterChip(
                            selected = uiState.selectedStatusFilter == status,
                            onClick = { viewModel.setStatusFilter(status) },
                            label = { Text("${status.label} ($count)") },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Search Bar (Balanced 52dp height)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "Einsatz, Bahnhof, Linie",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Suchen",
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Löschen",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Einsätze List or Empty State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredEinsatze.isEmpty()) {
                EmptyStateView(
                    isArchive = uiState.activeTab == EinsatzTab.ARCHIV,
                    isSearch = uiState.searchQuery.isNotEmpty() || uiState.selectedStatusFilter != null,
                    onImportScreenshotClick = { imagePickerLauncher.launch("image/*") },
                    onImportCsvClick = { showCsvDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredEinsatze,
                        key = { it.id }
                    ) { einsatz ->
                        EinsatzCard(
                            einsatz = einsatz,
                            onEditClick = {
                                editingEinsatz = it
                                showManualDialog = true
                            },
                            onDeleteClick = { viewModel.deleteEinsatz(context, it.id) },
                            onStatusChange = { item, newStatus ->
                                viewModel.toggleEinsatzStatus(context, item, newStatus)
                            },
                            onRescheduleClick = { rescheduleEinsatzTarget = it }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Purge All Confirmation Dialog
    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = {
                Text(
                    text = "Alle Einsätze löschen?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Möchtest du wirklich ALLE Einsätze (aktive & archivierte) unwiderruflich aus der App löschen?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purgeAll(context)
                        showPurgeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Ja, alle löschen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // AI / CSV Loading Dialog
    if (uiState.isParsingScreenshot || uiState.isParsingCsv) {
        Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (uiState.isParsingScreenshot) "KI analysiert deinen Screenshot..." else "CSV Daten werden ausgewertet...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Einsätze werden erkannt und vorbereitet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Recognized Results Preview Dialog
    uiState.parsedPreviewList?.let { previewList ->
        ScreenshotPreviewDialog(
            parsedList = previewList,
            onDismiss = { viewModel.dismissParsedPreview() },
            onConfirmSave = { confirmed ->
                viewModel.confirmParsedEinsatze(context, confirmed)
            }
        )
    }

    // Reschedule / Postpone Dialog
    rescheduleEinsatzTarget?.let { einsatzToReschedule ->
        RescheduleDialog(
            einsatz = einsatzToReschedule,
            onDismiss = { rescheduleEinsatzTarget = null },
            onConfirmReschedule = { newDateStr ->
                viewModel.rescheduleEinsatz(context, einsatzToReschedule, newDateStr)
            }
        )
    }

    // CSV Import Dialog
    if (showCsvDialog) {
        CsvImportDialog(
            onDismiss = { showCsvDialog = false },
            onPickFileClick = { csvPickerLauncher.launch("*/*") },
            onParseTextClick = { text -> viewModel.analyzeCsvText(text) }
        )
    }

    // Manual Add / Edit Dialog
    if (showManualDialog) {
        EinsatzDialog(
            initialEinsatz = editingEinsatz,
            onDismiss = { showManualDialog = false },
            onSave = { saved ->
                viewModel.saveEinsatz(context, saved)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun EmptyStateView(
    isArchive: Boolean,
    isSearch: Boolean,
    onImportScreenshotClick: () -> Unit,
    onImportCsvClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else if (isArchive) Icons.Default.Archive else Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isSearch -> "Keine Einsätze für diesen Filter"
                    isArchive -> "Noch keine abgeschlossenen Einsätze im Archiv"
                    else -> "Noch keine aktiven Einsätze vorhanden"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isSearch -> "Versuche einen anderen Filter oder Suchbegriff."
                    isArchive -> "Sobald du einen Einsatz als 'Abgeschlossen' markierst, erscheint er hier."
                    else -> "Füge einen Screenshot deiner Einsatzliste ein oder lade eine CSV-Datei hoch!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isSearch && !isArchive) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onImportScreenshotClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Screenshot (KI)")
                    }

                    Button(
                        onClick = onImportCsvClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV Import")
                    }
                }
            }
        }
    }
}
