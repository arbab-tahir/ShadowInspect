package com.shadowinspect.app.presentation.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.shadowinspect.app.domain.dashboard.ThreatTrend
import com.shadowinspect.app.presentation.theme.ChartColors
import com.shadowinspect.app.presentation.theme.NeonGreen

@Composable
fun ThreatTrendChart(
    trends: List<ThreatTrend>,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No trend data available",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }
    
    // Prepare data for chart
    val criticalEntries = trends.mapIndexed { index, trend ->
        com.patrykandpatrick.vico.core.entry.FloatEntry(index.toFloat(), trend.critical.toFloat())
    }
    
    val highEntries = trends.mapIndexed { index, trend ->
        com.patrykandpatrick.vico.core.entry.FloatEntry(index.toFloat(), trend.high.toFloat())
    }
    
    val mediumEntries = trends.mapIndexed { index, trend ->
        com.patrykandpatrick.vico.core.entry.FloatEntry(index.toFloat(), trend.medium.toFloat())
    }
    
    val model = entryModelOf(criticalEntries, highEntries, mediumEntries)
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem("Critical", ChartColors.Critical)
            LegendItem("High", ChartColors.High)
            LegendItem("Medium", ChartColors.Medium)
        }
        
        // Chart
        Chart(
            chart = lineChart(
                lines = listOf(
                    lineSpec(lineColor = ChartColors.Critical, lineThickness = 4.dp),
                    lineSpec(lineColor = ChartColors.High, lineThickness = 4.dp),
                    lineSpec(lineColor = ChartColors.Medium, lineThickness = 4.dp)
                )
            ),
            model = model,
            startAxis = rememberStartAxis(
                title = "Threats",
                valueFormatter = { value, _ -> value.toInt().toString() }
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ ->
                    val index = value.toInt()
                    if (index >= 0 && index < trends.size) {
                        // Show just the day number (e.g. "5", "6", "12")
                        val date = trends[index].date  // format: "yyyy-MM-dd"
                        val day = date.substringAfterLast("-").trimStart('0')
                        day.ifEmpty { "0" }
                    } else {
                        ""
                    }
                },
                title = "Date"
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

@Composable
private fun LegendItem(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
