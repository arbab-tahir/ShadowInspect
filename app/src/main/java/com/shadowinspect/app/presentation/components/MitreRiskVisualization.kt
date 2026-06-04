package com.shadowinspect.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.domain.mitre.ComprehensiveRiskScore
import com.shadowinspect.app.domain.mitre.PredictedThreat
import com.shadowinspect.app.domain.mitre.RiskFactor
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

/**
 * Premium Risk Gauge with Glowing Gradients
 */
@Composable
fun RiskGauge(
    score: Int,
    size: Float = 220f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val color = when {
        score >= 70 -> NeonRed
        score >= 40 -> Color(0xFFFFA500)
        else -> NeonGreen
    }
    
    Box(
        modifier = modifier
            .size(size.dp)
            .padding(16.dp)
            .drawBehind {
                drawGaugeDetailed(score, color, pulse)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-2).sp
                ),
                color = color
            )
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "THREAT INDEX",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = color
                )
            }
        }
    }
}

private fun DrawScope.drawGaugeDetailed(score: Int, color: Color, pulse: Float) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 - 20f
    val sweepAngle = (score / 100f) * 240f
    val startAngle = 150f
    
    // Background Track with Glow
    drawArc(
        color = Color.DarkGray.copy(alpha = 0.2f),
        startAngle = startAngle,
        sweepAngle = 240f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 16f, cap = StrokeCap.Round)
    )
    
    // Outer Glow
    drawArc(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.2f * pulse), Color.Transparent),
            center = center,
            radius = radius + 40f
        ),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius - 20f, center.y - radius - 20f),
        size = Size((radius + 20f) * 2, (radius + 20f) * 2),
        style = Stroke(width = 40f, cap = StrokeCap.Round)
    )

    // Main Foreground Arc
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 16f, cap = StrokeCap.Round)
    )
    
    // Tic Marks
    for (i in 0..10) {
        val angle = (startAngle + i * 24) * (Math.PI / 180).toFloat()
        val innerPos = Offset(
            center.x + (radius - 30f) * Math.cos(angle.toDouble()).toFloat(),
            center.y + (radius - 30f) * Math.sin(angle.toDouble()).toFloat()
        )
        val outerPos = Offset(
            center.x + (radius - 10f) * Math.cos(angle.toDouble()).toFloat(),
            center.y + (radius - 10f) * Math.sin(angle.toDouble()).toFloat()
        )
        drawLine(
            color = if (i * 10 <= score) color.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.3f),
            start = innerPos,
            end = outerPos,
            strokeWidth = 4f
        )
    }
}

/**
 * Tactical Bar Chart for MITRE Tactics
 */
@Composable
fun TacticScoreChart(
    tacticScores: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val allTactics = listOf(
        "Initial Access", "Execution", "Persistence", "Privilege Escalation",
        "Defense Evasion", "Credential Access", "Discovery", "Collection",
        "Command and Control", "Exfiltration", "Impact"
    )
    
    val displayScores = allTactics.associateWith { tactic ->
        tacticScores.entries.find { it.key.contains(tactic, ignoreCase = true) }?.value ?: 0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .border(1.dp, NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TACTICAL BREAKDOWN",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = NeonCyan
                )
            }
            
            Text(
                text = "FULL HEATMAP",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = NeonCyan.copy(alpha = 0.5f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        displayScores.forEach { (tactic, score) ->
            TacticProgressBar(tactic, score)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun TacticProgressBar(label: String, score: Int) {
    val color = when {
        score >= 70 -> NeonRed
        score >= 40 -> Color(0xFFFFA500)
        else -> NeonGreen
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "$score%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 100f)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.5f), color)
                        )
                    )
                    .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Risk Factors with Alert Styling
 */
@Composable
fun RiskFactorsList(
    riskFactors: List<RiskFactor>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "CRITICAL RISK FACTORS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = NeonRed,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        riskFactors.forEach { factor ->
            val color = when (factor.severity) {
                "CRITICAL" -> NeonRed
                "HIGH" -> Color(0xFFFFA500)
                else -> NeonGreen
            }
            
            Card(
                modifier = Modifier.padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (factor.severity == "CRITICAL") Icons.Default.GppBad else Icons.Default.Warning,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = factor.factor.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = color
                        )
                        Text(
                            text = factor.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Predicted Threat Progression
 */
@Composable
fun PredictedThreatsList(
    predictedThreats: List<PredictedThreat>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PREDICTED THREAT VECTORS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Color(0xFFFFA500),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        predictedThreats.forEach { threat ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = CyberSurface,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFFFFA500).copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = threat.threatType,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Badge(
                            containerColor = Color(0xFFFFA500).copy(alpha = 0.2f),
                            contentColor = Color(0xFFFFA500)
                        ) {
                            Text("${(threat.probability * 100).toInt()}% PROB")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = threat.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COUNTERMEASURE: ${threat.mitigation}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen
                        )
                    }
                }
            }
        }
    }
}

/**
 * Confidence Indicator
 */
@Composable
fun ConfidenceIndicator(
    confidence: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ANALYSIS CONFIDENCE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = "${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (confidence >= 0.8) NeonGreen else if (confidence >= 0.5) Color.Yellow else NeonRed
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { confidence.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = if (confidence >= 0.8) NeonGreen else if (confidence >= 0.5) Color.Yellow else NeonRed,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}
