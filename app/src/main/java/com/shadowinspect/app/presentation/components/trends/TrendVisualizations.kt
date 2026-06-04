package com.shadowinspect.app.presentation.components.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.domain.trends.*
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@Composable
fun ThreatPatternsCard(
    patterns: List<ThreatPattern>,
    isDemoData: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Detected Patterns",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                if (isDemoData) {
                    Surface(
                        color = NeonGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "SIMULATED",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (patterns.isEmpty()) {
                Text(
                    text = if (isDemoData) "No demo data available." 
                           else "Analyzing your scans... No significant threat patterns have been detected yet.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val displayed = if (expanded) patterns else patterns.take(3)
                displayed.forEach { pattern ->
                    PatternItem(pattern = pattern)
                }

                if (patterns.size > 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 40.dp)
                            .clickable { expanded = !expanded },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Show less" else "Show more",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (expanded) "Show less" else "Show ${patterns.size - 3} more patterns",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatternItem(pattern: ThreatPattern) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (pattern.patternType) {
                PatternType.REPEATING -> Icons.Default.Repeat
                PatternType.ESCALATING -> Icons.Default.TrendingUp
                PatternType.EMERGING -> Icons.Default.NewReleases
                PatternType.CORRELATED -> Icons.Default.Link
                PatternType.SEASONAL -> Icons.Default.CalendarToday
                PatternType.CYCLICAL -> Icons.Default.Autorenew
            },
            contentDescription = null,
            tint = when (pattern.severity) {
                "CRITICAL" -> NeonRed
                "HIGH" -> Color(0xFFFFA500)
                else -> NeonGreen
            },
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = pattern.patternType.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = pattern.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Surface(
            shape = MaterialTheme.shapes.small,
            color = when (pattern.severity) {
                "CRITICAL" -> NeonRed.copy(alpha = 0.2f)
                "HIGH" -> Color(0xFFFFA500).copy(alpha = 0.2f)
                else -> NeonGreen.copy(alpha = 0.2f)
            }
        ) {
            Text(
                text = "${(pattern.confidence * 100).toInt()}%",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun PredictionsCard(
    predictions: List<ThreatPrediction>,
    isDemoData: Boolean = false,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Threat Predictions",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen
                )
                
                Icon(
                    imageVector = Icons.Default.OnlinePrediction,
                    contentDescription = null,
                    tint = NeonGreen
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (predictions.isEmpty()) {
                Text(
                    text = if (isDemoData) "No demo predictions available." 
                           else "Analyzing trends... Predictions will appear once more scan data is collected.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                predictions.take(3).forEach { prediction ->
                    PredictionItem(prediction = prediction)
                }
            }
        }
    }
}

@Composable
fun PredictionItem(prediction: ThreatPrediction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = prediction.threatType,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (prediction.expectedSeverity) {
                        "CRITICAL" -> NeonRed.copy(alpha = 0.2f)
                        "HIGH" -> Color(0xFFFFA500).copy(alpha = 0.2f)
                        "MEDIUM" -> Color(0xFFFFFF00).copy(alpha = 0.2f)
                        else -> NeonGreen.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = "${(prediction.probability * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Text(
                text = "Expected: ${prediction.timeframe.name}",
                style = MaterialTheme.typography.bodySmall
            )
            
            if (prediction.recommendedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Actions: ${prediction.recommendedActions.first()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonGreen
                )
            }
        }
    }
}

@Composable
fun WeeklyForecastCard(
    forecast: WeeklyForecast,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen
                )
                
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (forecast.overallRiskLevel) {
                        "CRITICAL" -> NeonRed
                        "HIGH" -> Color(0xFFFFA500)
                        "MEDIUM" -> Color(0xFFFFFF00)
                        else -> NeonGreen
                    }.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = forecast.overallRiskLevel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(forecast.predictedThreats) { dayPrediction ->
                    DayForecastItem(prediction = dayPrediction)
                }
            }
            
            forecast.accuracy?.let { accuracy ->
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accuracy: ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = accuracy,
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = if (accuracy > 0.7) NeonGreen else Color(0xFFFFA500)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(accuracy * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun DayForecastItem(prediction: PredictedThreatForDay) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = prediction.day.take(3),
                style = MaterialTheme.typography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${prediction.predictedRisk}",
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    prediction.predictedRisk > 70 -> NeonRed
                    prediction.predictedRisk > 50 -> Color(0xFFFFA500)
                    prediction.predictedRisk > 30 -> Color(0xFFFFFF00)
                    else -> NeonGreen
                }
            )
            
            Text(
                text = "risk",
                style = MaterialTheme.typography.labelSmall
            )
            
            prediction.actualRisk?.let { actual ->
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Text(
                    text = "Actual: $actual",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreen
                )
            }
        }
    }
}
