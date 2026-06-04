package com.shadowinspect.app.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.domain.dashboard.TechniqueCount
import com.shadowinspect.app.presentation.theme.ChartColors

@Composable
fun TechniquePieChart(
    techniques: List<TechniqueCount>,
    modifier: Modifier = Modifier
) {
    if (techniques.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No technique data",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }
    
    val total = techniques.sumOf { it.count }
    val colors = listOf(
        ChartColors.Collection,
        ChartColors.CommandAndControl,
        ChartColors.CredentialAccess,
        ChartColors.DefenseEvasion,
        ChartColors.Discovery,
        ChartColors.Execution,
        ChartColors.Exfiltration,
        ChartColors.Impact,
        ChartColors.InitialAccess,
        ChartColors.Persistence,
        ChartColors.PrivilegeEscalation
    )
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie chart - centered and fixed size to leave room for legend
        Box(
            modifier = Modifier
                .size(100.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = 0f
                val size = Size(size.width, size.height)
                
                techniques.take(8).forEachIndexed { index, technique ->
                    if (technique.count > 0 && total > 0) {
                        val angle = (technique.count.toFloat() / total) * 360f
                        if (angle > 0f && !angle.isNaN()) {
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = startAngle,
                                sweepAngle = angle,
                                useCenter = true,
                                size = size,
                                style = Fill
                            )
                            startAngle += angle
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Legend - Vertical list with better space for text
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            techniques.take(4).forEachIndexed { index, technique ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = technique.techniqueName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${technique.count}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors[index % colors.size]
                    )
                }
            }
        }
    }
}
