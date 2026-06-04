package com.shadowinspect.app.presentation.screens.dashboard

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.domain.dashboard.ActivityItem
import com.shadowinspect.app.domain.dashboard.DashboardState
import com.shadowinspect.app.domain.score.*
import com.shadowinspect.app.domain.trends.*
import com.shadowinspect.app.presentation.components.charts.*
import com.shadowinspect.app.presentation.components.score.*
import com.shadowinspect.app.presentation.components.trends.*
import com.shadowinspect.app.presentation.components.widgets.*
import com.shadowinspect.app.presentation.theme.ChartColors
import com.shadowinspect.app.presentation.theme.CyberBackground
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed
import com.shadowinspect.app.domain.widgets.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToScan: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWidgetSettings: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val widgetState by viewModel.widgetState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    // Handle Export Side Effect from old exportDashboard (sharing PDF directly from dashboard)
    LaunchedEffect(uiState.exportFile) {
        uiState.exportFile?.let { file ->
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Dashboard Summary"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.resetExportState()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberBackground
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "SECURITY DASHBOARD",
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            letterSpacing = 2.sp
                        )
                    },
                    actions = {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = NeonGreen
                            )
                        } else {
                            // Navigate to full Export screen
                            IconButton(onClick = onNavigateToExport) {
                                Icon(
                                    Icons.Default.IosShare,
                                    contentDescription = "Export Analytics",
                                    tint = NeonGreen
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = onNavigateToWidgetSettings) {
                            Icon(Icons.Default.DashboardCustomize, contentDescription = "Widget Settings")
                        }
                        IconButton(onClick = { viewModel.debugDashboardData() }) {
                            Icon(Icons.Default.BugReport, contentDescription = "Debug")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(8.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Score header spans all columns
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val cats = uiState.categoryScores
                        SecurityScoreCard(
                            overallScore = uiState.securityScore,
                            appScore   = cats?.appSecurity?.score  ?: (maxOf(0, 100 - uiState.highRiskCount * 10 - uiState.criticalCount * 20)).coerceIn(0, 100),
                            urlScore   = cats?.urlSafety?.score    ?: 85,
                            phoneScore = cats?.phoneSecurity?.score ?: 90,
                            systemScore = cats?.systemHealth?.score ?: 85
                        )
                    }

                    // Dynamic widgets from database
                    items(
                        items = widgetState.widgets.filter { it.isVisible },
                        key = { it.id },
                        span = { widget ->
                            val colSpan = when (widget.size) {
                                WidgetSize.SMALL -> 1
                                WidgetSize.MEDIUM -> 2
                                WidgetSize.LARGE -> 3
                                WidgetSize.WIDE -> 3
                                WidgetSize.TALL -> 1
                            }
                            GridItemSpan(colSpan)
                        }
                    ) { widget ->
                        WidgetContainer(
                            widget = widget,
                            onDragStart = { viewModel.startDragging(widget.id) },
                            onDragEnd = { offsetX, offsetY ->
                                viewModel.updateWidgetPosition(widget.id, offsetX, offsetY)
                            },
                            onSettingsClick = onNavigateToWidgetSettings,
                            onRemoveClick = { viewModel.removeWidget(widget.id) },
                            content = {
                                WidgetContent(
                                    widget = widget,
                                    dashboardState = uiState,
                                    onAction = { action -> 
                                        onNavigateToScan(action)
                                    }
                                )
                            }
                        )
                    }

                    // Fallback technique chart if no widget for it
                    if (widgetState.widgets.none { it.widgetType == WidgetType.TECHNIQUE_PIE } && uiState.topTechniques.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Top MITRE Techniques", color = NeonGreen, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    TechniquePieChart(techniques = uiState.topTechniques)
                                }
                            }
                        }
                    }

                    // Threat patterns section
                    if (uiState.threatPatterns.isNotEmpty() || !uiState.isDemoData) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ThreatPatternsCard(
                                patterns = uiState.threatPatterns,
                                isDemoData = uiState.isDemoData
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color = NeonGreen
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun RecentActivityRow(
    activity: ActivityItem,
    dateFormat: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = when (activity.riskLevel) {
                "CRITICAL" -> NeonRed.copy(alpha = 0.2f)
                "HIGH" -> ChartColors.High.copy(alpha = 0.2f)
                "MEDIUM" -> ChartColors.Medium.copy(alpha = 0.2f)
                else -> NeonGreen.copy(alpha = 0.2f)
            }
        ) {
            Text(
                text = activity.scanType,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.target.take(25),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = dateFormat.format(Date(activity.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Text(
            text = "${activity.riskScore}",
            color = when (activity.riskLevel) {
                "CRITICAL" -> NeonRed
                "HIGH" -> ChartColors.High
                "MEDIUM" -> ChartColors.Medium
                else -> NeonGreen
            },
            style = MaterialTheme.typography.titleSmall
        )
    }
}
