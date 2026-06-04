package com.shadowinspect.app.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.presentation.theme.ChartColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RiskGaugeChart(
    score: Int,
    chartSizeDp: Float = 200f,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val riskLevel = when {
        score >= 80 -> "CRITICAL"
        score >= 60 -> "HIGH"
        score >= 40 -> "MEDIUM"
        score >= 20 -> "LOW"
        else -> "SAFE"
    }
    
    val riskColor = when {
        score >= 80 -> ChartColors.Critical
        score >= 60 -> ChartColors.High
        score >= 40 -> ChartColors.Medium
        score >= 20 -> ChartColors.Low
        else -> ChartColors.Safe
    }
    
    Box(
        modifier = modifier.size(chartSizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f * 0.8f
            val startAngle = 150f
            val sweepAngle = 240f
            
            // Draw background arc
            drawArc(
                color = Color.DarkGray.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 20f)
            )
            
            // Draw score arc
            val scoreAngle = (score / 100f) * sweepAngle
            drawArc(
                color = riskColor,
                startAngle = startAngle,
                sweepAngle = scoreAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 20f, cap = StrokeCap.Round)
            )
            
            // Draw ticks
            for (i in 0..10) {
                val angle = startAngle + (i * sweepAngle / 10f)
                val rad = Math.toRadians(angle.toDouble())
                val innerRadius = radius - 15f
                val outerRadius = radius + 5f
                
                val startX = center.x + (innerRadius * cos(rad)).toFloat()
                val startY = center.y + (innerRadius * sin(rad)).toFloat()
                val endX = center.x + (outerRadius * cos(rad)).toFloat()
                val endY = center.y + (outerRadius * sin(rad)).toFloat()
                
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2f
                )
            }
        }
        
        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineLarge,
                color = riskColor,
                fontSize = 48.sp
            )
            
            if (showLabel) {
                Text(
                    text = riskLevel,
                    style = MaterialTheme.typography.titleSmall,
                    color = riskColor
                )
            }
        }
    }
}
