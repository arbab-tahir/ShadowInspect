package com.shadowinspect.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.domain.mitre.DetectedTechnique
import com.shadowinspect.app.domain.mitre.MitreAnalysisResult
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@Composable
fun MitreTechniqueCard(
    technique: DetectedTechnique,
    expanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (technique.confidence >= 70) NeonRed.copy(alpha = 0.5f) else NeonGreen.copy(alpha = 0.3f)
    
    Card(
        onClick = onExpandToggle,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: ID and Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (technique.confidence >= 70) NeonRed else NeonGreen, RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = technique.technique.id,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (technique.confidence >= 70) NeonRed else NeonGreen
                    )
                }
                
                Surface(
                    color = (if (technique.confidence >= 70) NeonRed else NeonGreen).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, (if (technique.confidence >= 70) NeonRed else NeonGreen).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "CONFIDENCE: ${technique.confidence}%",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        color = if (technique.confidence >= 70) NeonRed else NeonGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Name
            Text(
                text = technique.technique.name.uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Tactics
            if (technique.technique.tactics.isNotEmpty()) {
                Text(
                    text = "TACTIC: ${technique.technique.tactics.joinToString().uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = NeonCyan.copy(alpha = 0.7f)
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "DETECTION EVIDENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    technique.evidence.forEach { evidence ->
                        Text(
                            text = "> $evidence",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    Text(
                        text = technique.technique.description,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            IconButton(
                onClick = onExpandToggle,
                modifier = Modifier.align(Alignment.End).size(24.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = NeonGreen.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun MitreSummaryCard(
    result: MitreAnalysisResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "RECONNAISSANCE SUMMARY",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TOTAL TECHNIQUES IDENTIFIED: ${result.totalTechniques}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = NeonGreen
                )
            }
        }
    }
}
