package com.shadowinspect.app.presentation.components.score

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.shadowinspect.app.domain.score.HistoricalScore
import com.shadowinspect.app.presentation.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScoreHistoryChart(
    history: List<HistoricalScore>,
    modifier: Modifier = Modifier
) {
    if (history.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (history.isEmpty()) "No history data available" else "Not enough data for a trend (minimum 2 scans required)",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }
    
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }
    
    // Prepare data for chart
    val entries = history.mapIndexed { index, score ->
        com.patrykandpatrick.vico.core.entry.FloatEntry(index.toFloat(), score.overallScore.toFloat())
    }
    
    val model = entryModelOf(entries)
    
    Chart(
        chart = lineChart(
            lines = listOf(
                lineSpec(NeonGreen, lineThickness = 4.dp)
            )
        ),
        model = model,
        startAxis = rememberStartAxis(
            title = "Score",
            valueFormatter = { value, _ -> "${value.toInt()}" }
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { index, _ ->
                history.getOrNull(index.toInt())?.let {
                    dateFormat.format(Date(it.timestamp))
                } ?: ""
            },
            title = "Date"
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    )
}
