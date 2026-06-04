package com.shadowinspect.app.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdvancedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "ADVANCED",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Diagnostic Info Section
                item {
                    Text(
                        text = "System Diagnostics",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                state.diagnosticInfo?.let { info ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                DiagnosticRow("App Version", info.appVersion)
                                DiagnosticRow("Android", info.androidVersion)
                                DiagnosticRow("Device", info.deviceModel)
                                DiagnosticRow("Resolution", info.screenSize)
                                DiagnosticRow("Total Scans", info.totalScans.toString())
                                DiagnosticRow("Database", "${info.databaseSize / 1024} KB")
                                DiagnosticRow("Battery Opt", if (info.batteryOptimization) "Disabled (Safe)" else "Enabled")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("ML Models Loaded", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                info.mlModelsLoaded.forEach { model ->
                                    Text("• $model", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Maintenance Actions
                item {
                    Text(
                        text = "Maintenance",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.exportAllData() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !state.isActionLoading,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeonCyan,
                            containerColor = Color.Black
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("▶ EXPORT ALL SCAN DATA (PDF)", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !state.isActionLoading,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeonRed,
                            containerColor = Color.Black
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = NeonRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("▶ CLEAR LOCAL SCAN DATA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Text(
                        "Note: Clearing data will permanently remove all scan history and MITRE reports from this device. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (state.isActionLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Clear All Data?") },
            text = { Text("Are you sure you want to permanently delete all scan history? This action is irreversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonRed)
                ) {
                    Text("DELETE EVERYTHING")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            // In a real app we'd use a SnackbarHostState
            viewModel.resetMessage()
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
