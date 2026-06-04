package com.shadowinspect.app.presentation.components.score

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.domain.score.ScoreRecommendation
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@Composable
fun ScoreRecommendationsCard(
    recommendations: List<ScoreRecommendation>,
    onRecommendationClick: (ScoreRecommendation) -> Unit,
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
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonGreen
                )
                
                Badge(
                    containerColor = if (recommendations.any { it.priority >= 4 }) 
                        NeonRed else NeonGreen
                ) {
                    Text("${recommendations.size}")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (recommendations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recommendations - you're doing great! 🎉",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                recommendations.take(5).forEach { rec ->
                    RecommendationItem(
                        recommendation = rec,
                        onClick = { onRecommendationClick(rec) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
                
                if (recommendations.size > 5) {
                    TextButton(
                        onClick = { /* Show all */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("View all ${recommendations.size}")
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationItem(
    recommendation: ScoreRecommendation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when (recommendation.priority) {
                            5 -> NeonRed
                            4 -> Color(0xFFFFA500) // Orange
                            3 -> Color(0xFFFFFF00) // Yellow
                            else -> NeonGreen
                        },
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (recommendation.priority >= 4) 
                        FontWeight.Bold else null
                )
                
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Impact chip
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = recommendation.impact,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    // Effort chip
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = recommendation.effort,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View",
            tint = NeonGreen
        )
    }
}
