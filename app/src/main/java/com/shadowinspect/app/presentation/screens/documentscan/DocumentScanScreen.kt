package com.shadowinspect.app.presentation.screens.documentscan

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowinspect.app.domain.model.*
import com.shadowinspect.app.presentation.components.FilePickerButton
import com.shadowinspect.app.presentation.components.HackerLoadingAnimation
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════
// Main Screen
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: DocumentScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "DOCUMENT FORENSIC SCANNER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp
                        ),
                        color = NeonGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Phase 1: File Selection
            AnimatedVisibility(
                visible = state.selectedFileUri == null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DocHeaderSection("TARGET ACQUISITION", "Select a PDF, DOCX, XLSX, PPTX, or TXT for deep forensic inspection.")
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "This engine extracts hidden metadata, detects embedded malicious files, scans for CVE exploits, identifies time-stomping anomalies, and flags leaked sensitive data.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    FilePickerButton(
                        onFileSelected = { uri: Uri -> viewModel.selectFile(uri) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Phase 2: File Selected
            state.selectedFileUri?.let {
                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DocFileCard(state.fileName, state.fileSize, state.documentType) { viewModel.clearSelection() }
                        if (state.scanResult == null) {
                            DocScanButton(state.isScanning) { viewModel.scanSelectedFile() }
                        }
                    }
                }
            }

            // Phase 3: Loading
            if (state.isScanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HackerLoadingAnimation(statusText = "FORENSIC ANALYSIS IN PROGRESS...")
                    if (state.showLoadingWarning) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "The analysis is taking longer due to file size. Please wait, analysis is under process... Don't exit or go back.",
                            color = Color(0xFFFF9800),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Phase 4: Results
            state.scanResult?.let { result ->
                AnimatedVisibility(visible = true, enter = fadeIn(tween(800)) + expandVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Risk Score Header
                        DocRiskHeader(result)
                        // 1. File Identity
                        DocCardSection("FILE IDENTITY", Icons.Default.Info) {
                            MetaRow("File Name", result.fileName)
                            MetaRow("Size", "${result.fileSize} bytes")
                            MetaRow("Format", result.format ?: result.documentType.name)
                            MetaRow("Version", result.version)
                            MetaRow("MIME", result.mimeType)
                            MetaRow("SHA-256", result.hashSha256)
                            MetaRow("MD5", result.hashMd5)
                        }
                        // 2. Metadata
                        DocCardSection("AUTHORSHIP & METADATA", Icons.Default.Person) {
                            MetaRow("Author", result.author)
                            MetaRow("Creator Tool", result.creator)
                            MetaRow("Producer", result.producer)
                            MetaRow("Company", result.company)
                            MetaRow("Manager", result.manager)
                            MetaRow("Last Modified By", result.lastModifiedBy)
                            MetaRow("Title", result.title)
                            MetaRow("Subject", result.subject)
                            MetaRow("Language", result.language)
                            if (result.keywords.isNotEmpty()) MetaRow("Keywords", result.keywords.joinToString(", "))
                        }
                        // 3. Content Stats
                        DocCardSection("CONTENT STATISTICS", Icons.Default.Analytics) {
                            MetaRow("Pages", result.pageCount?.toString())
                            MetaRow("Slides", result.slideCount?.toString())
                            MetaRow("Sheets", result.sheetCount?.toString())
                            MetaRow("Words", result.wordCount?.let { "%,d".format(it) })
                            MetaRow("Characters", result.characterCount?.let { "%,d".format(it) })
                            MetaRow("Paragraphs", result.paragraphCount?.toString())
                            MetaRow("Tables", result.tableCount?.toString())
                            MetaRow("Embedded Images", result.embeddedImageCount?.toString())
                        }
                        // 4. Forensic Timeline
                        DocCardSection("FORENSIC TIMELINE", Icons.Default.Schedule) {
                            MetaRow("Created", result.creationDate?.let { fmtDate(it) })
                            MetaRow("Modified", result.modificationDate?.let { fmtDate(it) })
                            MetaRow("Revisions", result.revisionNumber?.toString())
                            MetaRow("Editing Time", result.totalEditingTime?.let { "$it min" })
                            if (result.hasForensicAnomaly) {
                                Spacer(Modifier.height(4.dp))
                                Text("⚠️ ANOMALIES DETECTED", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                result.forensicAnomalyDetails.forEach { Text("• $it", color = NeonRed.copy(0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            }
                        }
                        // 5. Security & Permissions
                        DocCardSection("SECURITY & PERMISSIONS", Icons.Default.Lock) {
                            MetaRow("Encrypted", if (result.isEncrypted) "YES ⚠️" else "No")
                            MetaRow("Digitally Signed", if (result.isDigitallySigned) "YES ✅" else "No")
                            MetaRow("Protected", if (result.isProtected) "YES 🔒" else "No")
                            result.pdfPermissions?.let { p ->
                                Spacer(Modifier.height(4.dp))
                                Text("PDF Permission Flags:", color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                PermRow("Print", p.canPrint); PermRow("Copy", p.canCopy)
                                PermRow("Modify", p.canModify); PermRow("Annotate", p.canAnnotate)
                                PermRow("Fill Forms", p.canFillForms); PermRow("Extract", p.canExtractContent)
                                PermRow("Assemble", p.canAssemble); PermRow("Hi-Q Print", p.canPrintHighQuality)
                            }
                        }
                        // 6. Macro & Script Analysis
                        if (result.hasMacros || result.pdfJsActions.isNotEmpty()) {
                            DocCardSection("MACRO & SCRIPT ANALYSIS", Icons.Default.Code, borderColor = NeonRed) {
                                MetaRow("Macros Found", if (result.hasMacros) "YES ⚠️" else "No")
                                MetaRow("Macro Count", result.macroCount.toString())
                                if (result.maliciousMacros.isNotEmpty()) {
                                    Text("Malicious patterns:", color = NeonRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    result.maliciousMacros.forEach { Text("• $it", color = NeonRed.copy(0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                                }
                                if (result.pdfJsActions.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("PDF JavaScript (${result.pdfJsActions.size}):", color = NeonRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    result.pdfJsActions.take(5).forEach { Text("• $it", color = Color(0xFFFF9800), fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                                }
                            }
                        }
                        // 7. Embedded Content
                        if (result.embeddedFileCount > 0 || result.hasExternalReferences || result.templateUrl != null || result.hasHiddenSheets || result.hasTrackedChanges) {
                            DocCardSection("EMBEDDED CONTENT", Icons.Default.Folder, borderColor = Color(0xFFFF9800)) {
                                MetaRow("Embedded Files", result.embeddedFileCount.toString())
                                if (result.embeddedFileNames.isNotEmpty()) {
                                    result.embeddedFileNames.forEach { name ->
                                        val isDanger = name.endsWith(".exe") || name.endsWith(".dll") || name.endsWith(".bat") || name.endsWith(".ps1")
                                        Text("  📎 $name${if (isDanger) " ⚠️ DANGEROUS" else ""}", color = if (isDanger) NeonRed else NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                MetaRow("External Template", result.templateUrl)
                                MetaRow("External References", if (result.hasExternalReferences) "${result.externalReferences.size} found" else null)
                                MetaRow("Hidden Sheets", if (result.hasHiddenSheets) "YES ⚠️" else null)
                                MetaRow("Tracked Changes", if (result.hasTrackedChanges) "YES" else null)
                            }
                        }
                        // 8. URLs
                        if (result.totalUrls > 0) {
                            DocCardSection("URLS & REFERENCES (${result.totalUrls})", Icons.Default.Link) {
                                MetaRow("Total Links", result.totalUrls.toString())
                                MetaRow("Suspicious", result.suspiciousUrls.size.toString())
                                
                                Spacer(Modifier.height(8.dp))
                                Text("All Extracted URLs:", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                
                                result.embeddedUrls.take(20).forEach { url ->
                                    val isSusp = result.suspiciousUrls.contains(url)
                                    Row(Modifier.padding(vertical = 2.dp)) {
                                        Text(if (isSusp) "⚠️" else "🔗", fontSize = 10.sp)
                                        Spacer(Modifier.width(6.dp))
                                        androidx.compose.foundation.text.selection.SelectionContainer(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = url,
                                                color = if (isSusp) NeonRed else Color.White.copy(0.7f),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (result.embeddedUrls.size > 20) {
                                    Text("... and ${result.embeddedUrls.size - 20} more", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }
                        // 9. Sensitive Data
                        if (result.containsSensitiveData) {
                            DocCardSection("SENSITIVE DATA DETECTED", Icons.Default.Warning, borderColor = Color(0xFFFF9800)) {
                                result.sensitiveDataMatches.forEach { (type, matches) ->
                                    val matchLabel = "$type (${matches.size})"
                                    MetaRow(matchLabel, matches.joinToString("\n"))
                                }
                            }
                        }
                        // 10. CVE Exploits
                        if (result.detectedCves.isNotEmpty()) {
                            DocCardSection("CVE EXPLOIT SIGNATURES", Icons.Default.BugReport, borderColor = NeonRed) {
                                result.detectedCves.forEach { cve ->
                                    val sevColor = when (cve.severity) { "CRITICAL" -> NeonRed; "HIGH" -> Color(0xFFFF9800); else -> Color(0xFFFFEB3B) }
                                    Text("[${cve.severity}] ${cve.cveId}", color = sevColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(cve.name, color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text(cve.description, color = Color.White.copy(0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                        // 11. Hidden Text
                        if (result.hasHiddenText) {
                            DocCardSection("HIDDEN TEXT DETECTED", Icons.Default.VisibilityOff, borderColor = NeonRed) {
                                Text("White-on-white or invisible text was detected in this document. This technique is used to evade automated scanners and hide malicious payloads.",
                                    color = NeonRed.copy(0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        // 12. MITRE
                        if (result.mitreTechniques.isNotEmpty()) {
                            DocCardSection("MITRE ATT&CK", Icons.Default.Shield) {
                                result.mitreTechniques.forEach { Text("• $it", color = Color(0xFF80DEEA), fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                            }
                        }
                        // 13. Recommendations
                        DocCardSection("RECOMMENDATIONS", Icons.Default.Checklist) {
                            result.recommendations.forEach { Text(it, color = Color.White.copy(0.85f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                        }
                    }
                }
            }

            // Error
            state.error?.let { err ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(NeonRed.copy(0.1f)).border(1.dp, NeonRed, RoundedCornerShape(8.dp)).padding(16.dp)) {
                    Text("CRITICAL ERROR: $err", color = NeonRed, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Reusable Components
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun DocHeaderSection(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = NeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color.White.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

@Composable
private fun DocFileCard(name: String, size: String, type: DocumentType, onClear: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(CyberSurface, Color(0xFF1A2332))))
            .border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(12.dp)).padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, "File", tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$size • ${type.name}", color = Color.White.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                IconButton(onClick = onClear) { Icon(Icons.Default.Close, "Clear", tint = NeonRed) }
            }
        }
    }
}

@Composable
private fun DocScanButton(isScanning: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = !isScanning,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(0.15f))
    ) {
        Text(if (isScanning) "SCANNING…" else "⚡ INITIATE FORENSIC SCAN", color = NeonCyan,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
fun DocRiskHeader(result: DocumentAnalysisResult) {
    val riskColor = when { result.riskScore >= 70 -> NeonRed; result.riskScore >= 40 -> Color(0xFFFF9800); else -> NeonGreen }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(riskColor.copy(0.15f), Color.Black.copy(0.3f))))
            .border(1.dp, riskColor.copy(0.5f), RoundedCornerShape(16.dp)).padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("RISK SCORE", color = riskColor.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 4.sp)
            Text("${result.riskScore}/100", color = riskColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 48.sp)
            Text(result.riskLevel, color = riskColor.copy(0.8f), fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            if (result.threatCategories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("${result.threatCategories.size} threat categories detected", color = Color.White.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun DocCardSection(
    title: String, icon: ImageVector, borderColor: Color = NeonCyan.copy(0.2f),
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(CyberSurface.copy(0.6f)).border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, title, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.weight(1f), letterSpacing = 1.sp)
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Toggle", tint = NeonCyan.copy(0.5f))
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp)) { content() }
            }
        }
    }
}

@Composable
fun MetaRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label:", color = Color.White.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 11.sp,
            modifier = Modifier.width(130.dp))
        androidx.compose.foundation.text.selection.SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(value, color = Color.White.copy(0.9f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PermRow(label: String, allowed: Boolean) {
    Row(Modifier.padding(start = 8.dp, top = 1.dp)) {
        Text(if (allowed) "✅" else "🚫", fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (allowed) NeonGreen.copy(0.7f) else NeonRed.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}

private fun fmtDate(ms: Long): String = try {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(ms))
} catch (e: Exception) { ms.toString() }
