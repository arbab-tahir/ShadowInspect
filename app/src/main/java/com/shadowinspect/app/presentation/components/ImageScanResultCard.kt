package com.shadowinspect.app.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.domain.model.ImageAnalysisResult
import com.shadowinspect.app.domain.model.ImageThreatType
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ImageScanResultCard(
    result: ImageAnalysisResult,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with risk score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Image Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonGreen
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = getRiskColor(result.riskLevel).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${result.riskScore}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = getRiskColor(result.riskLevel)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image info — copyable fields
            ImageInfoRow("File", result.fileName)
            ImageInfoRow("Type", result.imageType.name)
            ImageInfoRow("Size", formatFileSizeResult(result.fileSize))
            ImageInfoRow("Dimensions", "${result.imageWidth}×${result.imageHeight}")
            ImageInfoRow("Bit Depth", "${result.bitDepth}-bit")
            CopyableInfoRow("SHA-256", result.hashSha256)
            CopyableInfoRow("MD5", result.hashMd5)
            CopyableInfoRow("MIME", result.mimeType)

            Spacer(modifier = Modifier.height(8.dp))

            // Risk level banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = getRiskColor(result.riskLevel).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Risk Level: ${result.riskLevel}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = getRiskColor(result.riskLevel)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Steganography section
            if (result.hasHiddenData) {
                ImageExpandableSection(
                    title = "⚠️ Hidden Data Detected",
                    icon = Icons.Default.Security,
                    isExpanded = expandedSection == "stego",
                    onToggle = { expandedSection = if (expandedSection == "stego") null else "stego" }
                ) {
                    Text(
                        text = "Confidence: ${result.steganographyResult?.confidenceScore}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonRed
                    )
                    Text(
                        text = "Method: ${result.steganographyResult?.method ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    result.steganographyResult?.extractionNotes?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // EXIF Metadata section
            if (result.hasExifData) {
                ImageExpandableSection(
                    title = "📷 EXIF Metadata",
                    icon = Icons.Default.Info,
                    isExpanded = expandedSection == "exif",
                    onToggle = { expandedSection = if (expandedSection == "exif") null else "exif" }
                ) {
                    result.exifMetadata?.make?.let { CopyableInfoRow("Camera Make", it) }
                    result.exifMetadata?.model?.let { CopyableInfoRow("Camera Model", it) }
                    result.exifMetadata?.dateTime?.let { CopyableInfoRow("Date Taken", it) }
                    result.exifMetadata?.software?.let { CopyableInfoRow("Software", it) }
                    result.exifMetadata?.exposureTime?.let { ImageInfoRow("Exposure", "${it}s") }
                    result.exifMetadata?.fNumber?.let { ImageInfoRow("F-Stop", "f/$it") }
                    result.exifMetadata?.iso?.let { ImageInfoRow("ISO", it.toString()) }
                    result.exifMetadata?.focalLength?.let { ImageInfoRow("Focal Length", "${it}mm") }
                    result.exifMetadata?.artist?.let { CopyableInfoRow("Artist", it) }
                    result.exifMetadata?.copyright?.let { CopyableInfoRow("Copyright", it) }
                    result.exifMetadata?.userComment?.let { CopyableInfoRow("Comment", it) }
                }
            }

            // GPS Location section
            if (result.containsGpsLocation) {
                ImageExpandableSection(
                    title = "📍 GPS Location Found",
                    icon = Icons.Default.LocationOn,
                    isExpanded = expandedSection == "gps",
                    onToggle = { expandedSection = if (expandedSection == "gps") null else "gps" }
                ) {
                    result.gpsCoordinates?.let { gps ->
                        CopyableInfoRow("Coordinates", gps.toLatLngString())
                        gps.altitude?.let {
                            ImageInfoRow("Altitude", "${it}m")
                        }
                    }
                }
            }

            // Threats section
            if (result.threatCategories.isNotEmpty()) {
                ImageExpandableSection(
                    title = "Threats Detected (${result.threatCategories.size})",
                    icon = Icons.Default.Warning,
                    isExpanded = expandedSection == "threats",
                    onToggle = { expandedSection = if (expandedSection == "threats") null else "threats" }
                ) {
                    result.threatCategories.forEach { threat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            color = when (threat) {
                                ImageThreatType.STEGANOGRAPHY -> NeonRed.copy(alpha = 0.2f)
                                ImageThreatType.GPS_LOCATION -> Color(0xFFFFA500).copy(alpha = 0.2f)
                                ImageThreatType.METADATA_LEAK -> Color(0xFFFFFF00).copy(alpha = 0.15f)
                                else -> NeonGreen.copy(alpha = 0.1f)
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "• ${formatThreatName(threat)}",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Explanation
            Text(
                text = result.explanation,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Recommendations
            if (result.recommendations.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NeonGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Recommendations",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeonGreen
                        )

                        result.recommendations.forEach { rec ->
                            Text(
                                text = "• $rec",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Timestamp
            Text(
                text = "Scanned: ${dateFormat.format(Date(result.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * A read-only info row (label + value)
 */
@Composable
private fun ImageInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * A copyable info row — tap to copy value, shows a copy icon hint.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CopyableInfoRow(label: String, value: String) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    copyToClipboard(context, label, value)
                },
                onLongClick = {
                    copyToClipboard(context, label, value)
                }
            )
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.length > 24) value.take(22) + "…" else value,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(12.dp),
                tint = NeonGreen.copy(alpha = 0.6f)
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

@Composable
private fun ImageExpandableSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = NeonGreen
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                content()
            }
        }
    }
}

private fun getRiskColor(riskLevel: String): Color {
    return when (riskLevel) {
        "CRITICAL" -> NeonRed
        "HIGH" -> Color(0xFFFFA500)
        "MEDIUM" -> Color(0xFFFFFF00)
        "LOW" -> Color(0xFF90EE90)
        else -> NeonGreen
    }
}

private fun formatThreatName(threat: ImageThreatType): String {
    return when (threat) {
        ImageThreatType.STEGANOGRAPHY -> "Steganography — Hidden Data"
        ImageThreatType.METADATA_LEAK -> "Metadata Leak — Device Info Exposed"
        ImageThreatType.GPS_LOCATION -> "GPS Location — Location Data Embedded"
        ImageThreatType.MANIPULATED_IMAGE -> "Image Manipulation Detected"
        ImageThreatType.ILLEGAL_CONTENT -> "Suspicious Content"
        ImageThreatType.EMBEDDED_URL -> "Embedded URLs Found"
        ImageThreatType.MALICIOUS_SCRIPT -> "Malicious Script Embedded"
    }
}

private fun formatFileSizeResult(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}
