package com.shadowinspect.app.presentation.scan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.domain.model.ApkAnalysisResult
import com.shadowinspect.app.domain.ml.MLDetectionResult
import com.shadowinspect.app.presentation.theme.*
import com.shadowinspect.app.presentation.components.*
import com.shadowinspect.app.domain.mitre.ReportTemplate

@Composable
fun EnhancedScanResultCard(
    result: ApkAnalysisResult,
    onDownloadReport: ((ReportTemplate) -> Unit)? = null,
    onShareReport: ((ReportTemplate) -> Unit)? = null,
    onFeedbackSubmit: ((Boolean, String?) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var showFormatDialog by remember { mutableStateOf(false) }
    var isSharingMode by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf(ReportTemplate.EDUCATIONAL) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStroke(2.dp, if (result.riskScore >= 70) NeonRed else NeonGreen.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (showFormatDialog) {
                AlertDialog(
                    onDismissRequest = { showFormatDialog = false },
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Security Report", color = NeonCyan) 
                        }
                    },
                    text = {
                        Column {
                            Text("Choose a report template:", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 12.dp))
                            
                            val options = listOf(
                                ReportTemplate.EDUCATIONAL to "📚 EDUCATIONAL - Simple & Friendly",
                                ReportTemplate.TECHNICAL to "🔧 TECHNICAL - Security Deep-Dive",
                                ReportTemplate.EXECUTIVE to "📊 EXECUTIVE - Summary for Management",
                                ReportTemplate.FORENSIC to "⚖️ FORENSIC - Formal Evidence Report",
                                ReportTemplate.JSON_DATA to "📦 JSON - Machine-Readable Data"
                            )
                            
                            options.forEach { (template, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selectedTemplate == template,
                                            onClick = { selectedTemplate = template }
                                        )
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedTemplate == template,
                                        onClick = { selectedTemplate = template },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonGreen)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selectedTemplate == template) NeonGreen else Color.White
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                if (isSharingMode) {
                                    onShareReport?.invoke(selectedTemplate)
                                } else {
                                    onDownloadReport?.invoke(selectedTemplate)
                                }
                                showFormatDialog = false 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.2f), contentColor = NeonGreen),
                            border = BorderStroke(1.dp, NeonGreen)
                        ) {
                            Text(if (isSharingMode) "SHARE" else "GENERATE")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFormatDialog = false }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                    },
                    containerColor = CyberSurface
                )
            }
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ANALYSIS COMPLETE",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "INTELLIGENCE REPORT v1.5",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { result.riskScore / 100f },
                            modifier = Modifier.size(64.dp),
                            color = if (result.riskScore >= 70) NeonRed else if (result.riskScore >= 30) Color.Yellow else NeonGreen,
                            strokeWidth = 6.dp,
                            trackColor = Color.DarkGray
                        )
                        Text(
                            text = "${result.riskScore}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = if (result.riskScore >= 70) NeonRed else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Grid
                ResultMetadataRow(
                    Icons.Default.Badge, 
                    "PACKAGE", 
                    result.packageName ?: "UNDEFINED",
                    onCopy = { clipboardManager.setText(AnnotatedString(result.packageName ?: "")) }
                )
                
                ResultMetadataRow(
                    Icons.Default.Fingerprint, 
                    "SHA-256", 
                    result.sha256, 
                    isTruncated = true,
                    onCopy = { clipboardManager.setText(AnnotatedString(result.sha256)) }
                )
                
                ResultMetadataRow(
                    Icons.Default.VerifiedUser, 
                    "RISK LEVEL", 
                    "[ ${result.riskLevel} ]", 
                    valueColor = if (result.riskScore >= 70) NeonRed else NeonGreen
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Chips
                var selectedDisc by remember { mutableStateOf<String?>(null) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip(
                        "PERMS", 
                        result.permissions.size.toString(), 
                        NeonCyan,
                        isSelected = selectedDisc == "PERMS",
                        onClick = { selectedDisc = if (selectedDisc == "PERMS") null else "PERMS" }
                    )
                    StatChip(
                        "DANGER", 
                        result.dangerousPermissions.size.toString(), 
                        NeonRed,
                        isSelected = selectedDisc == "DANGER",
                        onClick = { selectedDisc = if (selectedDisc == "DANGER") null else "DANGER" }
                    )
                    StatChip(
                        "TECH", 
                        result.mitreAnalysis?.totalTechniques?.toString() ?: "0", 
                        NeonGreen,
                        isSelected = selectedDisc == "TECH",
                        onClick = { selectedDisc = if (selectedDisc == "TECH") null else "TECH" }
                    )
                }

                AnimatedVisibility(
                    visible = selectedDisc != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        DiscoveryDetailSection(
                            type = selectedDisc ?: "",
                            result = result
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Summary / Explanation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXECUTIVE SUMMARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(result.explanation)) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = NeonCyan.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = result.explanation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace, 
                                lineHeight = 18.sp,
                                fontSize = 11.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }

        // --- ML ANALYSIS RESULTS ---
        result.mlDetection?.let { ml ->
            Spacer(modifier = Modifier.height(24.dp))
            MLAnalysisResultCard(ml)
        }

        // --- MITRE TACTICAL DATA ---
        result.mitreAnalysis?.let { mitreResult ->
            Spacer(modifier = Modifier.height(24.dp))
            
            HeaderSection("MITRE ATT&CK® FRAMEWORK", "Detection patterns mapped to adversarial tactics and techniques.")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RiskGauge(score = mitreResult.riskScore)
                    
                    mitreResult.comprehensiveRisk?.let { risk ->
                        ConfidenceIndicator(confidence = risk.confidenceLevel)
                        Spacer(modifier = Modifier.height(16.dp))
                        TacticScoreChart(tacticScores = risk.tacticScores)
                        Spacer(modifier = Modifier.height(16.dp))
                        RiskFactorsList(riskFactors = risk.riskFactors)
                    }
                }
            }

            if (mitreResult.predictedThreats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                PredictedThreatsList(predictedThreats = mitreResult.predictedThreats)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "IDENTIFIED TECHNIQUES",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            mitreResult.detectedTechniques.forEach { technique ->
                var expanded by remember { mutableStateOf(false) }
                MitreTechniqueCard(
                    technique = technique,
                    expanded = expanded,
                    onExpandToggle = { expanded = !expanded }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Report generation buttons
            if ((onDownloadReport != null || onShareReport != null) && mitreResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download Button
                    Button(
                        onClick = { 
                            isSharingMode = false
                            showFormatDialog = true 
                        },
                        modifier = Modifier.weight(1.1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberSurface,
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "DOWNLOAD",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Share Button
                    Button(
                        onClick = { 
                            isSharingMode = true
                            showFormatDialog = true 
                        },
                        modifier = Modifier.weight(0.9f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.1f),
                            contentColor = NeonGreen
                        ),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "SHARE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            // --- RESEARCH FEEDBACK SECTION ---
            Spacer(modifier = Modifier.height(16.dp))
            var showFeedbackDialog by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Was this analysis accurate?", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }
                        
                        Row {
                            IconButton(onClick = { onFeedbackSubmit?.invoke(true, null) }) {
                                Icon(Icons.Default.ThumbUp, contentDescription = "Yes", tint = NeonGreen)
                            }
                            IconButton(onClick = { showFeedbackDialog = true }) {
                                Icon(Icons.Default.ThumbDown, contentDescription = "No", tint = NeonRed)
                            }
                        }
                    }
                }
            }

            if (showFeedbackDialog) {
                FeedbackDialog(
                    onDismiss = { showFeedbackDialog = false },
                    onSubmit = { comments ->
                        onFeedbackSubmit?.invoke(false, comments)
                        showFeedbackDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (String?) -> Unit
) {
    var comments by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Improve Detection", color = NeonCyan) },
        text = {
            Column {
                Text("What was inaccurate about the analysis?", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Comments (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = NeonGreen
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(comments.ifBlank { null }) }) {
                Text("SUBMIT", color = NeonGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        },
        containerColor = CyberSurface
    )
}

@Composable
fun HeaderSection(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = NeonCyan,
            letterSpacing = 3.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ResultMetadataRow(
    icon: ImageVector, 
    label: String, 
    value: String, 
    valueColor: Color = Color.White,
    isTruncated: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.width(80.dp)
        )
        
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isTruncated && value.length > 20) value.take(20) + "..." else value,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                color = valueColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        onCopy?.let {
            IconButton(
                onClick = it,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = NeonGreen.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun StatChip(
    label: String, 
    value: String, 
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) color.copy(alpha = 0.25f) else color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, if (isSelected) color else color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) color else color.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun DiscoveryDetailSection(type: String, result: ApkAnalysisResult) {
    val title = when(type) {
        "PERMS" -> "REQUESTED PERMISSIONS"
        "DANGER" -> "DANGER INDICATORS"
        "TECH" -> "MITRE TECH SUMMARY"
        else -> ""
    }
    
    val color = when(type) {
        "PERMS" -> NeonCyan
        "DANGER" -> NeonRed
        "TECH" -> NeonGreen
        else -> NeonCyan
    }

    Surface(
        color = Color.Black.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            when(type) {
                "PERMS" -> {
                    result.permissions.forEach { perm ->
                        DiscoveryItem(perm.name.replace("android.permission.", ""), null, color)
                    }
                }
                "DANGER" -> {
                    if (result.dangerousPermissions.isEmpty()) {
                        Text("No specific high-danger permissions found.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    result.dangerousPermissions.forEach { perm ->
                        DiscoveryItem(perm.name.replace("android.permission.", ""), perm.description, color)
                    }
                }
                "TECH" -> {
                    val techs = result.mitreAnalysis?.detectedTechniques ?: emptyList()
                    if (techs.isEmpty()) {
                        Text("No adversarial techniques identified.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    techs.forEach { tech ->
                        DiscoveryItem(tech.technique.name, "Confidence: ${tech.confidence}%", color)
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalFileCard(fileName: String, fileSize: String, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Dataset, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "SIZE: $fileSize",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = NeonGreen.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = NeonRed)
            }
        }
    }
}

@Composable
fun DeepScanButton(isScanning: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Button(
        onClick = onClick,
        enabled = !isScanning,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                if (isScanning) 2.dp else 1.dp,
                if (isScanning) NeonGreen.copy(alpha = alpha) else NeonGreen,
                RoundedCornerShape(4.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isScanning) Color.Transparent else NeonGreen.copy(alpha = 0.1f),
            contentColor = NeonGreen
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        if (isScanning) {
            Text(
                "SYSTEM ANALYZING...",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = NeonGreen,
                trackColor = Color.Transparent
            )
        } else {
            Icon(Icons.Default.Security, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "INITIATE DEEP INSPECTION",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun DiscoveryItem(name: String, description: String?, accentColor: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(accentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
        }
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}

@Composable
fun MLAnalysisResultCard(ml: MLDetectionResult) {
    HeaderSection("\uD83E\uDDE0 ML ANALYSIS", "On-device machine learning classification results.")

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(
            1.dp,
            if (ml.isMalicious) NeonRed.copy(alpha = 0.5f) else NeonGreen.copy(alpha = 0.3f)
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
                    text = "Classification",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (ml.isMalicious) NeonRed.copy(alpha = 0.2f)
                            else NeonGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = ml.modelType.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ml.isMalicious) NeonRed else NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Threat classification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Threat Class:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Text(
                    text = ml.threatClass,
                    color = if (ml.isMalicious) NeonRed else NeonGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Confidence:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Text(
                    text = "${(ml.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Inference time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Inference Time:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Text(
                    text = "${ml.inferenceTimeMs}ms",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = NeonCyan
                )
            }

            // Threat family (if available)
            ml.threatFamily?.let { family ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Threat Family:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = family,
                        color = NeonRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }

            // Top predictions
            if (ml.topClasses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "TOP PREDICTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                ml.topClasses.take(5).forEach { pred ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        if (pred.probability > 0.5f) NeonRed else NeonGreen,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pred.className,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${(pred.probability * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (pred.probability > 0.5f) NeonRed else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
