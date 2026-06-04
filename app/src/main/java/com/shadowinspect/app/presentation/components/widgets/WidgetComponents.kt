package com.shadowinspect.app.presentation.components.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.domain.widgets.WidgetConfig
import com.shadowinspect.app.domain.widgets.WidgetSize
import com.shadowinspect.app.domain.widgets.WidgetType
import com.shadowinspect.app.presentation.components.charts.*
import com.shadowinspect.app.presentation.components.trends.*
import com.shadowinspect.app.presentation.screens.dashboard.DashboardViewModel
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@Composable
fun WidgetContainer(
    widget: WidgetConfig,
    onDragStart: () -> Unit,
    onDragEnd: (Int, Int) -> Unit,
    onSettingsClick: () -> Unit,
    onRemoveClick: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val sizeModifier = when (widget.size) {
        WidgetSize.SMALL  -> Modifier.size(160.dp, 160.dp)
        WidgetSize.MEDIUM -> Modifier.size(200.dp, 160.dp)
        WidgetSize.LARGE  -> Modifier.size(320.dp, 200.dp)
        WidgetSize.WIDE   -> Modifier.size(320.dp, 120.dp)
        WidgetSize.TALL   -> Modifier.size(160.dp, 250.dp)
    }

    Card(
        modifier = modifier
            .then(sizeModifier)
            .offset(x = offsetX.dp, y = offsetY.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd(offsetX.toInt(), offsetY.toInt())
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings",
                        tint = Color.White, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRemoveClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove",
                        tint = NeonRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun WidgetContent(
    widget: WidgetConfig,
    dashboardState: DashboardViewModel.DashboardUiState,
    onAction: (String) -> Unit
) {
    when (widget.widgetType) {

        // ── Risk Gauge ───────────────────────────────────────────────────────
        WidgetType.RISK_GAUGE -> {
            // Risk Gauge shows the average RAW risk score from scans (0–100 = low–high danger)
            // This is distinct from the SecurityScore which is 100 - riskAvg
            val riskValue = dashboardState.averageRiskScore
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RiskGaugeChart(
                    score = riskValue,
                    chartSizeDp = 100f,
                    showLabel = false
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = widget.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                val riskLabel = when {
                    riskValue >= 80 -> "CRITICAL"
                    riskValue >= 60 -> "HIGH"
                    riskValue >= 40 -> "MEDIUM"
                    riskValue >= 20 -> "LOW"
                    else            -> "SAFE"
                }
                val riskColor = when {
                    riskValue >= 80 -> NeonRed
                    riskValue >= 60 -> Color(0xFFFFA500)
                    riskValue >= 40 -> Color(0xFFFFFF00)
                    riskValue >= 20 -> Color(0xFF90EE90)
                    else            -> NeonGreen
                }
                Text(text = riskLabel, fontSize = 10.sp, color = riskColor,
                    fontWeight = FontWeight.Bold)
            }
        }

        // ── Security Score ───────────────────────────────────────────────────
        WidgetType.SECURITY_SCORE -> {
            // Security Score: higher = safer (inverted of risk)
            val score = dashboardState.securityScore
            val (scoreColor, statusLabel) = when {
                score >= 80 -> NeonGreen       to "Very Secure"
                score >= 60 -> Color(0xFF90EE90) to "Secure"
                score >= 40 -> Color(0xFFFFFF00) to "Moderate Risk"
                score >= 20 -> Color(0xFFFFA500) to "High Risk"
                else        -> NeonRed         to "Critical Risk"
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "$score", fontSize = 36.sp, color = scoreColor,
                    fontWeight = FontWeight.Bold)
                Text(text = widget.title, fontSize = 12.sp)
                Text(text = statusLabel, fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        // ── Threat Trends ────────────────────────────────────────────────────
        WidgetType.THREAT_TRENDS -> {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(text = widget.title, fontSize = 14.sp, color = NeonGreen)
                Spacer(modifier = Modifier.height(4.dp))
                ThreatTrendChart(
                    trends = dashboardState.threatTrends,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        }

        // ── Top MITRE Techniques (expandable) ───────────────────────────────
        WidgetType.TECHNIQUE_PIE -> {
            var expanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = widget.title, fontSize = 14.sp, color = NeonGreen)
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                AnimatedVisibility(
                    visible = !expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    TechniquePieChart(
                        techniques = dashboardState.topTechniques,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(dashboardState.topTechniques) { tech ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tech.techniqueId,
                                        fontSize = 10.sp,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tech.techniqueName,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = NeonGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${tech.count}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        // ── Recent Activity ──────────────────────────────────────────────────
        WidgetType.RECENT_ACTIVITY -> {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(text = widget.title, fontSize = 14.sp, color = NeonGreen)
                if (dashboardState.recentActivity.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recent activity", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn {
                        items(dashboardState.recentActivity.take(5)) { activity ->
                            Text(
                                text = "${activity.scanType}: ${activity.target.take(15)}",
                                fontSize = 11.sp, maxLines = 1
                            )
                            Text(
                                text = "Risk: ${activity.riskScore}",
                                fontSize = 10.sp,
                                color = when {
                                    activity.riskScore > 70 -> NeonRed
                                    activity.riskScore > 50 -> Color(0xFFFFA500)
                                    else -> NeonGreen
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Quick Scan ───────────────────────────────────────────────────────
        WidgetType.QUICK_SCAN -> {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(8.dp)
            ) {
                val availableWidth = maxWidth
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = widget.title,
                        fontSize = 13.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val scanOptions = listOf(
                        Triple("URL", Icons.Default.Link, NeonGreen),
                        Triple("APK", Icons.Default.Archive, Color(0xFF64B5F6)),
                        Triple("Phone", Icons.Default.Phone, Color(0xFFFFA500)),
                        Triple("Image", Icons.Default.Image, Color(0xFFE91E63)),
                        Triple("Document", Icons.Default.Description, Color(0xFF4CAF50))
                    )

                    val columns = when {
                        availableWidth < 250.dp -> 3
                        else -> 5
                    }
                    val rows = scanOptions.chunked(columns)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { (label, icon, color) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color.copy(alpha = 0.1f))
                                            .clickable {
                                                try {
                                                    onAction(label.lowercase())
                                                } catch (e: Exception) {
                                                    android.util.Log.e("WIDGET", "$label navigation failed", e)
                                                }
                                            }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "Scan $label",
                                            tint = color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            color = color,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                                // Add spacers if row is incomplete to keep alignment
                                if (rowItems.size < columns) {
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Security Tip ─────────────────────────────────────────────────────
        WidgetType.SECURITY_TIP -> {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(text = widget.title, fontSize = 14.sp, color = NeonGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = getRandomSecurityTip(), fontSize = 12.sp)
            }
        }

        // ── Predictions ──────────────────────────────────────────────────────
        WidgetType.PREDICTIONS -> {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(text = widget.title, fontSize = 14.sp, color = NeonGreen)
                if (dashboardState.predictions.isEmpty()) {
                    Text(text = "No predictions", fontSize = 12.sp)
                } else {
                    PredictionItem(prediction = dashboardState.predictions.first())
                }
            }
        }

        // ── Weekly Forecast ──────────────────────────────────────────────────
        WidgetType.WEEKLY_FORECAST -> {
            dashboardState.weeklyForecast?.let { forecast ->
                WeeklyForecastCard(
                    forecast = forecast,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }

        // ── Recommendations ──────────────────────────────────────────────────
        WidgetType.RECOMMENDATIONS -> {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(text = widget.title, fontSize = 14.sp, color = NeonGreen)
                Spacer(modifier = Modifier.height(8.dp))
                if (dashboardState.recommendations.isEmpty()) {
                    Text(text = "No recommendations", fontSize = 12.sp)
                } else {
                    dashboardState.recommendations.take(2).forEach { rec ->
                        Text(text = "• ${rec.title}", fontSize = 12.sp, maxLines = 2)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = widget.title, fontSize = 14.sp, color = NeonGreen)
            }
        }
    }
}

private fun getRandomSecurityTip(): String {
    val tips = listOf(
        "Use strong, unique passwords for each account",
        "Enable two-factor authentication whenever possible",
        "Keep your apps and OS updated regularly",
        "Be careful with SMS links — they could be phishing",
        "Review app permissions regularly and revoke unnecessary ones",
        "Use a VPN on public Wi-Fi networks",
        "Lock your phone when not in use",
        "Back up your data regularly to a secure location",
        "Disable Bluetooth when not needed",
        "Don't click suspicious email attachments",
        "Scan APKs before installing from unknown sources",
        "Check phone numbers on ShadowInspect before answering"
    )
    return tips.random()
}
