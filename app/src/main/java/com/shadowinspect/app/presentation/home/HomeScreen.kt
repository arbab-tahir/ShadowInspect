package com.shadowinspect.app.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowinspect.app.presentation.session.SessionViewModel
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@Composable
fun HomeScreen(
    onNavigateToUrlScan: () -> Unit,
    onNavigateToFileAnalysis: () -> Unit,
    onNavigateToPhoneScan: () -> Unit,
    onNavigateToDocumentScan: () -> Unit,
    onNavigateToImageScan: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToMitreBrowser: () -> Unit,
    onNavigateToResearch: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToEducation: () -> Unit,
    onLogout: (agentHandle: String) -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val session by sessionViewModel.session.collectAsState()
    val agentHandle = session?.agentHandle ?: "UNKNOWN"
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                agentHandle = agentHandle,
                onLogoutClick = {
                    viewModel.logout { onLogout(agentHandle) }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.recentScans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SystemStatusBanner(uiState.uptime, uiState.systemStatus)
                }

                item {
                    HeaderSection()
                }

                item {
                    QuickScanGrid(
                        onUrlClick = onNavigateToUrlScan,
                        onFileClick = onNavigateToFileAnalysis,
                        onPhoneClick = onNavigateToPhoneScan,
                        onDocumentClick = onNavigateToDocumentScan,
                        onImageClick = onNavigateToImageScan,
                        onMitreClick = onNavigateToMitreBrowser,
                        onResearchClick = onNavigateToResearch,
                        onDashboardClick = onNavigateToDashboard,
                        onEducationClick = onNavigateToEducation
                    )
                }

                item {
                    SecurityOverviewCard(uiState)
                }

                item {
                    WeeklyScanBarChart(weeklyValues = uiState.weeklyBarValues)
                }

                item {
                    SystemLogsSection(uiState.systemLogs)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT OPERATIONS",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeonCyan,
                            letterSpacing = 2.sp
                        )
                        TextButton(onClick = onNavigateToReports) {
                            Text("HISTORY", color = NeonCyan.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                if (uiState.recentScans.isEmpty()) {
                    item {
                        Text(
                            text = "No recent records found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(uiState.recentScans) { scan ->
                        val statusColor = if (scan.isHighRisk) NeonRed else if (scan.status == "SAFE") NeonGreen else NeonCyan
                        RecentScanItem(
                            title = scan.title,
                            type = scan.type,
                            status = scan.status,
                            statusColor = statusColor
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SystemStatusBanner(uptime: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(NeonGreen.copy(alpha = 0.1f))
            .border(1.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NeonGreen)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "STATUS: $status",
                style = MaterialTheme.typography.labelSmall,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "UPTIME: $uptime",
            style = MaterialTheme.typography.labelSmall,
            color = NeonGreen,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun WeeklyScanBarChart(weeklyValues: List<Float>) {
    val dayLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    val maxVal = weeklyValues.maxOrNull()?.takeIf { it > 0f } ?: 1f
    Column {
        Text(
            text = "SCAN ACTIVITY — LAST 7 DAYS",
            style = MaterialTheme.typography.titleSmall,
            color = NeonCyan,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (weeklyValues.all { it == 0f }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No scan data yet.\nRun your first scan to build the chart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    val barCount = weeklyValues.size
                    val totalWidth = size.width
                    val barWidth = totalWidth / barCount * 0.55f
                    val gap = totalWidth / barCount * 0.45f
                    val chartHeight = size.height * 0.85f

                    weeklyValues.forEachIndexed { i, value ->
                        val barHeight = (value / maxVal) * chartHeight
                        val x = i * (barWidth + gap) + gap / 2
                        val y = chartHeight - barHeight

                        // Glow shadow
                        drawRect(
                            color = NeonGreen.copy(alpha = 0.15f),
                            topLeft = Offset(x - 2, y),
                            size = Size(barWidth + 4, barHeight)
                        )
                        // Main bar
                        drawRect(
                            color = NeonGreen.copy(alpha = 0.85f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
            }
        }
        // Day labels row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SystemLogsSection(logs: List<HomeSystemLog>) {

    Column {
        Text(
            text = "KERNEL LOGS",
            style = MaterialTheme.typography.titleSmall,
            color = NeonCyan,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp),
                reverseLayout = false
            ) {
                items(logs) { log ->
                    Text(
                        text = "[${log.timestamp}] ${log.message}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = NeonGreen.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(agentHandle: String, onLogoutClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "AGENT ID: ${agentHandle.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan,
                fontFamily = FontFamily.Monospace
            )
        },
        actions = {
            IconButton(onClick = onLogoutClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = NeonRed
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun HeaderSection() {
    Column {
        Text(
            text = "SHADOW INSPECT",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonGreen
        )
        Text(
            text = "Advanced Sandbox Interface",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun QuickScanGrid(
    onUrlClick: () -> Unit,
    onFileClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onImageClick: () -> Unit,
    onMitreClick: () -> Unit,
    onResearchClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onEducationClick: () -> Unit
) {
    Column {
        Text(
            text = "ENTRY VECTORS & VIEWS",
            style = MaterialTheme.typography.titleSmall,
            color = NeonCyan,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        // Primary Action Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "URL",
                icon = Icons.Default.Language,
                onClick = onUrlClick
            )
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "FILES",
                icon = Icons.Default.FolderOpen,
                onClick = onFileClick
            )
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "NETWORK",
                icon = Icons.Default.Sensors,
                onClick = onPhoneClick
            )
        }
        // Secondary Action Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "DOCS",
                icon = Icons.Default.Description,
                onClick = onDocumentClick
            )
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "MITRE",
                icon = Icons.Default.Security,
                onClick = onMitreClick
            )
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "STATS",
                icon = Icons.Default.Analytics,
                onClick = onResearchClick
            )
        }
        // Tertiary Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "DASHBOARD",
                icon = Icons.Default.Dashboard,
                onClick = onDashboardClick
            )
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "IMAGE",
                icon = Icons.Default.Image,
                onClick = onImageClick
            )
            QuickScanCard(
                modifier = Modifier.weight(1f),
                title = "EDUCATION",
                icon = Icons.Default.School,
                onClick = onEducationClick
            )
        }
    }
}

@Composable
fun QuickScanCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NeonGreen,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SecurityOverviewCard(state: HomeState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "THREAT INTEL",
                style = MaterialTheme.typography.titleSmall,
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 16.dp),
                letterSpacing = 2.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverviewStat(label = "TOTAL", value = state.totalScans.toString(), color = NeonCyan)
                OverviewStat(label = "THREATS", value = state.highRiskCount.toString(), color = NeonRed)
                OverviewStat(label = "CONFIDENCE", value = "${state.averageScore}%", color = NeonGreen)
            }
        }
    }
}

@Composable
fun OverviewStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun RecentScanItem(title: String, type: String, status: String, statusColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(CyberSurface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "TYPE: $type",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = "[ $status ]",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = statusColor,
            fontFamily = FontFamily.Monospace
        )
    }
}
