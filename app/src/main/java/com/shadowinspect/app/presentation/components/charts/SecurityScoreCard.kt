package com.shadowinspect.app.presentation.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.presentation.theme.ChartColors
import com.shadowinspect.app.presentation.theme.NeonGreen

@Composable
fun SecurityScoreCard(
    overallScore: Int,
    appScore: Int = 0,
    urlScore: Int = 0,
    phoneScore: Int = 0,
    systemScore: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main score
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .drawBehind {
                            val size = Size(size.width, size.height)
                            
                            // Background circle
                            drawArc(
                                color = Color.DarkGray.copy(alpha = 0.3f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                size = size,
                                style = Stroke(width = 8f)
                            )
                            
                            // Score arc
                            val scoreAngle = (overallScore / 100f) * 360f
                            val color = when {
                                overallScore >= 80 -> NeonGreen
                                overallScore >= 60 -> ChartColors.Low
                                overallScore >= 40 -> ChartColors.Medium
                                overallScore >= 20 -> ChartColors.High
                                else -> ChartColors.Critical
                            }
                            
                            drawArc(
                                color = color,
                                startAngle = -90f,
                                sweepAngle = scoreAngle,
                                useCenter = false,
                                size = size,
                                style = Stroke(width = 8f, cap = StrokeCap.Round)
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$overallScore",
                            style = MaterialTheme.typography.headlineMedium,
                            color = when {
                                overallScore >= 80 -> NeonGreen
                                overallScore >= 60 -> ChartColors.Low
                                overallScore >= 40 -> ChartColors.Medium
                                overallScore >= 20 -> ChartColors.High
                                else -> ChartColors.Critical
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Score breakdown
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    ScoreBreakdownItem("App Security", appScore, ChartColors.Collection)
                    ScoreBreakdownItem("URL Safety", urlScore, ChartColors.Exfiltration)
                    ScoreBreakdownItem("Phone Calls", phoneScore, ChartColors.Impact)
                    ScoreBreakdownItem("System Health", systemScore, ChartColors.Safe)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Risk level indicator (Security Score is inverted of Risk)
            // If overallScore is 90, Risk is 10 (SAFE)
            val riskLevel = when {
                overallScore >= 80 -> "SAFE"
                overallScore >= 60 -> "LOW"
                overallScore >= 40 -> "MEDIUM"
                overallScore >= 20 -> "HIGH"
                else -> "CRITICAL"
            }
            
            val riskColor = when {
                overallScore >= 80 -> NeonGreen
                overallScore >= 60 -> ChartColors.Low
                overallScore >= 40 -> ChartColors.Medium
                overallScore >= 20 -> ChartColors.High
                else -> ChartColors.Critical
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = riskColor.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Overall Risk Level: $riskLevel",
                    modifier = Modifier.padding(8.dp),
                    color = riskColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ScoreBreakdownItem(
    label: String,
    score: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(6.dp)
                .background(Color.DarkGray.copy(alpha = 0.2f), shape = MaterialTheme.shapes.extraSmall)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(score / 100f)
                    .background(color, shape = MaterialTheme.shapes.extraSmall)
            )
        }
        
        Text(
            text = "$score",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}
