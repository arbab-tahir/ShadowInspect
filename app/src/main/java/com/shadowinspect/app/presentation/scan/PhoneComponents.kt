package com.shadowinspect.app.presentation.scan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.domain.model.PhoneAnalysisResult
import com.shadowinspect.app.domain.model.SpamSource
import com.shadowinspect.app.presentation.theme.*

@Composable
fun PhoneResultCard(
    result: PhoneAnalysisResult,
    onClear: (() -> Unit)? = null
) {
    val riskColor = riskColor(result.riskLevel)
    val displayLevel = result.riskLevel ?: "UNKNOWN"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Risk Header ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RiskScoreGauge(score = result.spamScore, color = riskColor)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayLevel.uppercase(),
                        color = riskColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "Risk Score: ${result.spamScore}/100",
                        color = DimWhite,
                        fontSize = 13.sp
                    )
                }

                if (onClear != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = DimWhite)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = riskColor.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // ── Number Info ──────────────────────────────────────
            InfoRow("📞", "Number", result.formattedNumber)
            if (result.isValid) {
                InfoRow("✅", "Status", "Valid Number")
            } else {
                InfoRow("❌", "Status", "Invalid / Unverified")
            }

            result.countryName?.let { InfoRow("🌍", "Country", "$it (${result.countryCode ?: ""})") }
            result.carrier?.let { InfoRow("📡", "Carrier", it) }
            InfoRow(result.lineType.let {
                when (it.name) {
                    "MOBILE" -> "📱"; "LANDLINE" -> "☎️"; "VOIP" -> "🌐"
                    "PREMIUM" -> "💰"; "TOLL_FREE" -> "📞"; else -> "❓"
                }
            }, "Line Type", result.lineType.label)

            // VoIP / Prepaid tags
            if (result.isVoIP || result.isPrepaid == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (result.isVoIP) {
                        TagChip("VoIP", NeonRed)
                    }
                    if (result.isPrepaid == true) {
                        TagChip("Prepaid", NeonYellow)
                    }
                }
            }

            // ── Source Scores ────────────────────────────────────
            if (result.spamSources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "DETECTION SOURCES",
                    color = NeonCyan,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                result.spamSources.forEach { source ->
                    SourceItem(source, riskColor)
                }
            }

            // ── AI Explanation ───────────────────────────────────
            result.explanation?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "ANALYSIS SUMMARY",
                    color = NeonCyan,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun RiskScoreGauge(score: Int, color: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
        Canvas(modifier = Modifier.size(60.dp)) {
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (score.toFloat() / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$score",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun InfoRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = DimWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TagChip(name: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = name.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color
        )
    }
}

@Composable
fun SourceItem(source: SpamSource, riskColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(source.sourceName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(source.reason ?: "No detailed reason provided", color = DimWhite, fontSize = 11.sp)
        }
        Text(
            "${source.score}%",
            color = if (source.score > 50) NeonRed else NeonGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

fun riskColor(level: String?): Color {
    val lvl = level?.uppercase() ?: return Color.Gray
    return when (lvl) {
        "CRITICAL", "MALICIOUS" -> NeonRed
        "HIGH" -> Color(0xFFFF5722)
        "MODERATE", "SUSPICIOUS", "MEDIUM" -> NeonYellow
        "LOW" -> Color(0xFF4CAF50)
        "SAFE", "CLEAN" -> NeonGreen
        else -> Color.Gray
    }
}

@Composable
fun RecentPhoneScansSection(
    scans: List<PhoneAnalysisResult>,
    onRescan: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "RECENT SCANS",
            color = NeonCyan,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(scans.take(10)) { scan ->
                RecentScanChip(scan = scan, onRescan = { onRescan(scan.phoneNumber) })
            }
        }
    }
}

@Composable
fun RecentScanChip(scan: PhoneAnalysisResult, onRescan: () -> Unit) {
    val color = riskColor(scan.riskLevel)

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onRescan() },
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    scan.riskLevel ?: "UNKNOWN",
                    color = color,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                (scan.formattedNumber ?: "").ifBlank { scan.phoneNumber ?: "Unknown" },
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Score: ${scan.spamScore}/100",
                color = DimWhite,
                fontSize = 11.sp
            )
        }
    }
}
