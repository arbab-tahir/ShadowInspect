package com.shadowinspect.app.presentation.screens.export

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.domain.export.ExportConfig
import com.shadowinspect.app.domain.export.ExportFormat
import com.shadowinspect.app.domain.export.ExportMetadata
import com.shadowinspect.app.domain.export.ExportScope
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.shadowinspect.app.presentation.theme.NeonRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) } // Prioritize PDF
    var selectedScope by remember { mutableStateOf(ExportScope.LAST_MONTH) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "EXPORT ANALYTICS",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header information
            item {
                Text(
                    text = "Professional Analytics Reports",
                    style = MaterialTheme.typography.headlineSmall,
                    color = NeonGreen
                )
                Text(
                    text = "Generate detailed summaries of your security posture, threats detected, and predictive forecasts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Export button
            item {
                OutlinedButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonGreen,
                        containerColor = Color.Black
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Generate New Report",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }

            // Quick Formats Section
            item {
                Text(
                    text = "Quick Formats",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickExportButton(
                        text = "PDF",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.quickExport(ExportFormat.PDF, context) }
                    )
                    QuickExportButton(
                        text = "CSV",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.quickExport(ExportFormat.CSV, context) }
                    )
                    QuickExportButton(
                        text = "JSON",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.quickExport(ExportFormat.JSON, context) }
                    )
                }
            }
            
            // Previous exports
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Exports",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonGreen
                    )
                    if (state.previousExports.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAllExports() }) {
                            Text("Clear All", color = NeonRed)
                        }
                    }
                }
            }
            
            if (state.previousExports.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reports generated yet.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else {
                items(state.previousExports) { export ->
                    ExportItemCard(
                        export = export,
                        dateFormat = dateFormat,
                        onShare = { viewModel.shareExport(export.fileName, context) },
                        onDelete = { viewModel.deleteExport(export.fileName) }
                    )
                }
            }
        }
    }
    
    // Export dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Report Configuration") },
            text = {
                Column {
                    // Format selection
                    Text("Selected Format:", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFormat == ExportFormat.PDF,
                            onClick = { selectedFormat = ExportFormat.PDF },
                            label = { Text("PDF (Report)") }
                        )
                        FilterChip(
                            selected = selectedFormat == ExportFormat.CSV,
                            onClick = { selectedFormat = ExportFormat.CSV },
                            label = { Text("CSV (Raw)") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Time range
                    Text("Analysis Period:", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedScope == ExportScope.LAST_WEEK,
                            onClick = { selectedScope = ExportScope.LAST_WEEK },
                            label = { Text("Week") }
                        )
                        FilterChip(
                            selected = selectedScope == ExportScope.LAST_MONTH,
                            onClick = { selectedScope = ExportScope.LAST_MONTH },
                            label = { Text("Month") }
                        )
                        FilterChip(
                            selected = selectedScope == ExportScope.ALL_TIME,
                            onClick = { selectedScope = ExportScope.ALL_TIME },
                            label = { Text("All Time") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        val config = ExportConfig(
                            format = selectedFormat,
                            scope = selectedScope,
                            includeCharts = true,
                            includeRawData = true
                        )
                        viewModel.exportWithConfig(config, context)
                    }
                ) {
                    Text("Generate", color = NeonGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.isExporting) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonGreen)
        }
    }
}

@Composable
fun QuickExportButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
    ) {
        Text(text, fontSize = 10.sp)
    }
}

@Composable
fun ExportItemCard(
    export: ExportMetadata,
    dateFormat: SimpleDateFormat,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on format
            Icon(
                imageVector = when (export.format) {
                    ExportFormat.CSV -> Icons.Default.TableChart
                    ExportFormat.JSON -> Icons.Default.Code
                    ExportFormat.PDF -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = when (export.format) {
                    ExportFormat.PDF -> NeonRed
                    else -> NeonGreen
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = export.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(Date(export.generatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "${export.recordCount} items • ${export.size / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // Actions
            Row {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NeonGreen
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = NeonRed
                    )
                }
            }
        }
    }
}
