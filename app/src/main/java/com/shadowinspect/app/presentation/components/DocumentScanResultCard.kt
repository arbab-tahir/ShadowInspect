package com.shadowinspect.app.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.domain.model.DocumentAnalysisResult
import com.shadowinspect.app.domain.model.DocumentThreatType
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed
import java.text.SimpleDateFormat
import java.util.*

private val OrangeWarning = Color(0xFFFFA500)
private val YellowCaution = Color(0xFFFFFF00)

@Composable
fun DocumentScanResultCard(
    result: DocumentAnalysisResult,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val context = LocalContext.current

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
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Document Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonGreen
                    )
                }

                // Risk score badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = getRiskColor(result.riskLevel).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${result.riskScore}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = getRiskColor(result.riskLevel),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File info
            DocInfoRow("File", result.fileName)
            DocInfoRow("Type", result.documentType.name)
            DocInfoRow("Size", docFormatFileSize(result.fileSize))

            // Copyable SHA-256
            CopyableInfoRow(
                label = "SHA-256",
                value = result.hashSha256,
                displayValue = result.hashSha256,
                context = context
            )

            // Copyable MD5
            CopyableInfoRow(
                label = "MD5",
                value = result.hashMd5,
                displayValue = result.hashMd5,
                context = context
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Risk level banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = getRiskColor(result.riskLevel).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Risk Level: ${result.riskLevel} (${result.riskScore}/100)",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = getRiskColor(result.riskLevel),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata section
            DocExpandableSection(
                title = "Document Metadata",
                icon = Icons.Default.Info,
                isExpanded = expandedSection == "metadata",
                onToggle = { expandedSection = if (expandedSection == "metadata") null else "metadata" }
            ) {
                result.author?.let { DocInfoRow("Author", it) }
                result.creator?.let { DocInfoRow("Creator", it) }
                result.producer?.let { DocInfoRow("Producer", it) }
                result.pageCount?.let { DocInfoRow("Pages", it.toString()) }
                result.wordCount?.let { DocInfoRow("Words", it.toString()) }
                result.creationDate?.let {
                    DocInfoRow("Created", dateFormat.format(Date(it)))
                }
                result.modificationDate?.let {
                    DocInfoRow("Modified", dateFormat.format(Date(it)))
                }
            }

            // Macro Analysis section
            if (result.hasMacros || result.macroCount > 0) {
                val isMaliciousMacro = result.threatCategories.contains(DocumentThreatType.MALICIOUS_MACRO)
                DocExpandableSection(
                    title = "Macro Analysis ${if (isMaliciousMacro) "⚠️" else "✅"}",
                    icon = Icons.Default.Code,
                    isExpanded = expandedSection == "macros",
                    onToggle = { expandedSection = if (expandedSection == "macros") null else "macros" }
                ) {
                    DocInfoRow("Macros Detected", "${result.macroCount}")

                    if (isMaliciousMacro) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonRed.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "⚠️ MALICIOUS MACROS DETECTED",
                                    color = NeonRed,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Auto-executing macros may run automatically when the document is opened",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Macros can execute system commands, download files, or steal data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Do NOT enable macros unless you fully trust the document source",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonRed
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "• Macros present but no malicious patterns detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // URLs section
            if (result.embeddedUrls.isNotEmpty()) {
                DocExpandableSection(
                    title = "Embedded URLs (${result.embeddedUrls.size})",
                    icon = Icons.Default.Link,
                    isExpanded = expandedSection == "urls",
                    onToggle = { expandedSection = if (expandedSection == "urls") null else "urls" }
                ) {
                    // Show suspicious URLs first
                    result.suspiciousUrls.take(5).forEach { url ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            color = NeonRed.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ $url",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { copyToClipboard(context, url, "URL") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy URL",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Safe URLs
                    result.embeddedUrls
                        .filter { !result.suspiciousUrls.contains(it) }
                        .take(3)
                        .forEach { url ->
                            Text(
                                text = "• $url",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                    if (result.embeddedUrls.size > 8) {
                        Text(
                            text = "+${result.embeddedUrls.size - 8} more URLs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Threats section
            if (result.threatCategories.isNotEmpty()) {
                DocExpandableSection(
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
                                DocumentThreatType.MALICIOUS_MACRO -> NeonRed.copy(alpha = 0.2f)
                                DocumentThreatType.SUSPICIOUS_URL -> OrangeWarning.copy(alpha = 0.2f)
                                else -> NeonGreen.copy(alpha = 0.1f)
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "• ${formatThreatName(threat)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = getThreatExplanation(threat),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // MITRE techniques with explanations
            if (result.mitreTechniques.isNotEmpty()) {
                DocExpandableSection(
                    title = "MITRE ATT&CK Techniques (${result.mitreTechniques.size})",
                    icon = Icons.Default.BugReport,
                    isExpanded = expandedSection == "mitre",
                    onToggle = { expandedSection = if (expandedSection == "mitre") null else "mitre" }
                ) {
                    result.mitreTechniques.forEach { technique ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            color = NeonCyan.copy(alpha = 0.05f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = technique,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Default
                            )
                        }
                    }
                }
            }

            // Explanation
            DocExpandableSection(
                title = "Full Analysis Report",
                icon = Icons.Default.Assessment,
                isExpanded = expandedSection == "explanation",
                onToggle = { expandedSection = if (expandedSection == "explanation") null else "explanation" }
            ) {
                Text(
                    text = result.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Default
                )
            }

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
                        Spacer(modifier = Modifier.height(4.dp))
                        result.recommendations.forEach { rec ->
                            Text(
                                text = rec,
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

// ── Copyable info row ──────────────────────────────────────────────

@Composable
private fun CopyableInfoRow(
    label: String,
    value: String,
    displayValue: String,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { copyToClipboard(context, value, label) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (displayValue.length > 20) displayValue.take(20) + "…" else displayValue,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = NeonCyan,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(14.dp)
            )
        }
    }
}

@Composable
private fun DocInfoRow(label: String, value: String) {
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
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (label == "Risk Score") FontWeight.Bold else null
        )
    }
}

@Composable
private fun DocExpandableSection(
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

        if (isExpanded) {
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

// ── Helper functions ──────────────────────────────────────────────

private fun getRiskColor(riskLevel: String): Color {
    return when (riskLevel) {
        "CRITICAL" -> NeonRed
        "HIGH" -> OrangeWarning
        "MEDIUM" -> YellowCaution
        else -> NeonGreen
    }
}

private fun docFormatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun formatThreatName(threat: DocumentThreatType): String {
    return when (threat) {
        DocumentThreatType.MALICIOUS_MACRO -> "Malicious Macro"
        DocumentThreatType.EMBEDDED_SCRIPT -> "Embedded Script"
        DocumentThreatType.SUSPICIOUS_URL -> "Suspicious URL"
        DocumentThreatType.PHISHING_CONTENT -> "Phishing Content"
        DocumentThreatType.SENSITIVE_DATA -> "Sensitive Data"
        DocumentThreatType.ENCRYPTED_CONTENT -> "Encrypted Content"
        DocumentThreatType.METADATA_LEAK -> "Metadata Leak"
        else -> threat.name
    }
}

private fun getThreatExplanation(threat: DocumentThreatType): String {
    return when (threat) {
        DocumentThreatType.MALICIOUS_MACRO -> "Contains VBA code that may execute commands, download payloads, or steal data when the document is opened."
        DocumentThreatType.EMBEDDED_SCRIPT -> "Contains scripting code that can automate actions on your system without your knowledge."
        DocumentThreatType.SUSPICIOUS_URL -> "Contains links to potentially malicious websites, phishing pages, or malware download sites."
        DocumentThreatType.PHISHING_CONTENT -> "Contains social engineering elements designed to trick you into revealing credentials or personal information."
        DocumentThreatType.SENSITIVE_DATA -> "Contains personally identifiable information (emails, phone numbers, SSNs, or credit card numbers)."
        DocumentThreatType.ENCRYPTED_CONTENT -> "File is password-protected, which can be used to hide malicious content from security scanners."
        DocumentThreatType.METADATA_LEAK -> "Document metadata reveals sensitive information about the author, organization, or system."
        else -> "Additional forensic threat identified during deep scan."
    }
}
