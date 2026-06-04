package com.shadowinspect.app.presentation.scan

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowinspect.app.domain.mitre.MitreAnalysisResult
import com.shadowinspect.app.domain.mitre.ReportTemplate
import com.shadowinspect.app.presentation.components.FilePickerButton
import com.shadowinspect.app.presentation.components.HackerLoadingAnimation
import com.shadowinspect.app.presentation.reports.MitreReportViewModel
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScanScreen(
    onBack: () -> Unit,
    onNavigateToReports: () -> Unit,
    viewModel: FileScanViewModel = hiltViewModel(),
    reportViewModel: MitreReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val reportState by reportViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pendingShare by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf(false) }

    LaunchedEffect(reportState.generatedPdfFile, reportState.generatedJsonFile) {
        val pdf = reportState.generatedPdfFile
        val json = reportState.generatedJsonFile
        
        if (pdf != null) {
            if (pendingShare) {
                val intent = reportViewModel.reportGenerator.shareReport(pdf)
                context.startActivity(Intent.createChooser(intent, "Share Report"))
                pendingShare = false
                reportViewModel.clearGeneratedFiles()
            } else if (pendingDownload) {
                val success = reportViewModel.saveToPublicDownloads(pdf)
                if (success) {
                    Toast.makeText(context, "Report saved to Downloads", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "ERROR: Could not save report", Toast.LENGTH_LONG).show()
                }
                pendingDownload = false
                reportViewModel.clearGeneratedFiles()
                // Removed: onNavigateToReports() - Stay on results screen for direct download perception
            }
        }
        
        if (json != null) {
            if (pendingShare) {
                val intent = reportViewModel.reportGenerator.shareReport(json)
                context.startActivity(Intent.createChooser(intent, "Share Report"))
                pendingShare = false
                reportViewModel.clearGeneratedFiles()
            } else if (pendingDownload) {
                val success = reportViewModel.saveToPublicDownloads(json)
                if (success) {
                    Toast.makeText(context, "JSON Data saved to Downloads", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "ERROR: Could not save JSON", Toast.LENGTH_LONG).show()
                }
                pendingDownload = false
                reportViewModel.clearGeneratedFiles()
                // Removed: onNavigateToReports() - Stay on results screen for direct download perception
            }
        }
    }
    val scrollState = rememberScrollState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "APK SECURITY ANALYZER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = NeonGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToReports) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Phase 1: File Selection
            AnimatedVisibility(
                visible = state.selectedFileUri == null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HeaderSection("TARGET ACQUISITION", "Select an encrypted or plain APK package for deep inspection.")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    FilePickerButton(
                        onFileSelected = { uri: Uri -> viewModel.selectFile(uri) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Phase 2: Analysis Control
            state.selectedFileUri?.let {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TacticalFileCard(
                            fileName = state.fileName,
                            fileSize = state.fileSize,
                            onClear = { viewModel.clearSelection() }
                        )

                        if (state.scanResult == null) {
                            DeepScanButton(
                                isScanning = state.isScanning,
                                onClick = { viewModel.scanSelectedFile() }
                            )
                        }
                    }
                }
            }

            // Phase 3: Results Display
            state.scanResult?.let { result ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(1000)) + expandVertically()
                ) {
                    EnhancedScanResultCard(
                        result = result,
                        onDownloadReport = { template ->
                            pendingDownload = true
                            pendingShare = false
                            reportViewModel.generateReport(
                                analysisResult = result.mitreAnalysis ?: MitreAnalysisResult.empty(),
                                scanTarget = result.packageName ?: result.fileName ?: "Unknown APK",
                                scanType = "APK",
                                scanId = System.currentTimeMillis(),
                                template = template
                            )
                        },
                        onShareReport = { template ->
                            result.mitreAnalysis?.let { mitreResult ->
                                pendingShare = true
                                pendingDownload = false
                                reportViewModel.generateReport(
                                    analysisResult = mitreResult,
                                    scanTarget = result.fileName ?: result.packageName ?: "Unknown APK",
                                    scanType = "APK",
                                    scanId = System.currentTimeMillis(),
                                    template = template
                                )
                                Toast.makeText(context, "Preparing Report for share...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFeedbackSubmit = { wasAccurate, comments ->
                            state.lastScanId?.let { id ->
                                viewModel.submitFeedback(id, wasAccurate, comments)
                                Toast.makeText(context, "Feedback submitted. Thank you for helping our research!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            state.error?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonRed.copy(alpha = 0.1f))
                        .border(1.dp, NeonRed, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "CRITICAL ERROR: $err",
                        color = NeonRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
