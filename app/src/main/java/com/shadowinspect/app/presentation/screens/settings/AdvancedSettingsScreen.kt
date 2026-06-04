package com.shadowinspect.app.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMLSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    viewModel: AdvancedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently remove all scan history and MITRE reports. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonRed)
                ) { Text("CLEAR EVERYTHING") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("This will restore all app configurations to their default values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllSettings()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonRed)
                ) { Text("RESET") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("CANCEL") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "ADVANCED SETTINGS", 
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        style = MaterialTheme.typography.titleMedium
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ML Settings
                item {
                    SettingsOptionCard(
                        icon = Icons.Default.PrecisionManufacturing,
                        title = "ML Detection Engine",
                        subtitle = "Configure on-device ML models for malware detection",
                        onClick = onNavigateToMLSettings
                    )
                }
                
                // Diagnostics
                item {
                    SettingsOptionCard(
                        icon = Icons.Default.MonitorHeart,
                        title = "System Diagnostics",
                        subtitle = "View device info, app version, and ML model status",
                        onClick = onNavigateToDiagnostics
                    )
                }
                
                // Export Data
                item {
                    SettingsOptionCard(
                        icon = Icons.Default.LibraryBooks,
                        title = "Export Data",
                        subtitle = "Export scan history and reports to PDF",
                        onClick = { viewModel.exportAllData() }
                    )
                }
                
                // Clear History
                item {
                    SettingsOptionCard(
                        icon = Icons.Default.History,
                        title = "Clear History",
                        subtitle = "Remove all scan records permanently",
                        onClick = { showClearDialog = true },
                        warning = true
                    )
                }
                
                // Reset App
                item {
                    SettingsOptionCard(
                        icon = Icons.Default.Refresh,
                        title = "Reset All Settings",
                        subtitle = "Restore default configuration",
                        onClick = { showResetDialog = true },
                        warning = true
                    )
                }
            }

            // Feedback overlays
            state.message?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.resetMessage()
                }
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonGreen)
                ) {
                    Text(msg, modifier = Modifier.padding(16.dp), color = Color.Black)
                }
            }

            state.error?.let { err ->
                LaunchedEffect(err) {
                    kotlinx.coroutines.delay(5000)
                    viewModel.resetMessage()
                }
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonRed)
                ) {
                    Text(err, modifier = Modifier.padding(16.dp), color = Color.White)
                }
            }
            
            if (state.isActionLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun SettingsOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    warning: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (warning) 
                NeonRed.copy(alpha = 0.1f) 
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (warning) NeonRed else NeonGreen,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (warning) NeonRed else Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = NeonGreen
            )
        }
    }
}
