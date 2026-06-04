package com.shadowinspect.app.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.domain.dashboard.TacticCount
import com.shadowinspect.app.presentation.theme.ChartColors

@Composable
fun TacticBarChart(
    tactics: List<TacticCount>,
    modifier: Modifier = Modifier
) {
    if (tactics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No tactic data",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }
    
    val maxCount = tactics.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        tactics.take(7).forEach { tactic ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tactic.tactic.take(15),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(100.dp)
                )
                
                // Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    val fraction = (tactic.count.toFloat() / maxCount).coerceIn(0f, 1f)
                    val barModifier = if (!fraction.isNaN() && fraction > 0f) {
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                    } else {
                        Modifier
                            .fillMaxHeight()
                            .width(0.dp)
                    }

                    Canvas(modifier = barModifier) {
                        drawRect(
                            color = ChartColors.getTacticColor(tactic.tactic),
                            size = Size(size.width, size.height),
                            style = Fill
                        )
                    }
                    
                    Text(
                        text = "${tactic.count}",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
        
        if (tactics.size > 7) {
            Text(
                text = "+${tactics.size - 7} more tactics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
