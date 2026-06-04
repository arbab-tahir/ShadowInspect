package com.shadowinspect.app.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun StorageSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdvancedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "STORAGE MANAGEMENT", 
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Storage Usage Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Storage Usage",
                                style = MaterialTheme.typography.headlineSmall,
                                color = NeonGreen
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("In Use (App): ${state.dbSizeFormatted}", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Free: ${state.availableSpace}", style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LinearProgressIndicator(
                                progress = { 0.05f }, // Estimated
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = NeonGreen,
                                trackColor = NeonGreen.copy(alpha = 0.2f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            StorageBreakdownItem("Database", state.dbSizeFormatted, 0.6f)
                            StorageBreakdownItem("ML Models", "24.5 MB", 0.3f)
                            StorageBreakdownItem("Cache", "2.1 MB", 0.1f)
                        }
                    }
                }
                
                // Database Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Statistics",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonGreen
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            InfoRow("Total Scan Records", "${state.diagnosticInfo?.totalScans ?: 0}")
                            InfoRow("Database Size", state.dbSizeFormatted)
                            InfoRow("Available Disk", state.availableSpace)
                        }
                    }
                }
                
                // Maintenance Actions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Actions",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonGreen
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { viewModel.exportAllData() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen.copy(alpha = 0.2f),
                                    contentColor = NeonGreen
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("EXPORT ALL DATA (PDF)")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonRed.copy(alpha = 0.2f),
                                    contentColor = NeonRed
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CLEAR LOCAL HISTORY")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Note: Exporting data will generate a PDF summary in your Downloads folder. Clearing data is permanent.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
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

            if (state.isActionLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun StorageBreakdownItem(label: String, size: String, percentage: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = size,
            style = MaterialTheme.typography.bodyMedium,
            color = NeonGreen
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(NeonGreen.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percentage)
                .height(4.dp)
                .background(NeonGreen)
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = NeonGreen)
    }
}
