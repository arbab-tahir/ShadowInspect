package com.shadowinspect.app.presentation.reports

import com.shadowinspect.app.presentation.scan.ZoomableImageDialog

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowinspect.app.domain.model.ApkAnalysisResult
import com.shadowinspect.app.domain.model.UrlScanResult
import com.shadowinspect.app.presentation.reports.ReportDetailUiState
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed
import com.shadowinspect.app.presentation.theme.RiskHigh
import com.shadowinspect.app.presentation.theme.RiskLow
import com.shadowinspect.app.presentation.theme.RiskMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.shadowinspect.app.presentation.scan.*
import com.shadowinspect.app.presentation.components.ImageScanResultCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    onBack: () -> Unit,
    viewModel: ReportDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showFullScreenshot by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("REPORT DETAILS", letterSpacing = 2.sp, fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = NeonCyan
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.error ?: "Unknown Error",
                        color = NeonRed,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                ReportContent(state, { selectedCategory = it }, { showFullScreenshot = true })
            }
        }
    }

    // Reuse Modals from UrlScanScreen logic
    if (selectedCategory != null && state.urlResult != null) {
        val category = selectedCategory!!
        val res = state.urlResult!!
        val filteredEngines = res.fullEngineResults.filter { it.value.category == category.lowercase() }
        
        ModalBottomSheet(
            onDismissRequest = { selectedCategory = null },
            containerColor = CyberSurface,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                val catColor = when(category.lowercase()) {
                    "malicious" -> NeonRed
                    "suspicious" -> Color(0xFFFF9800)
                    "harmless", "undetected" -> NeonGreen
                    else -> Color.Gray
                }
                
                Text(
                    text = "${category.uppercase()} ENGINES (${filteredEngines.size})",
                    color = catColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEngines.toList()) { (vendor, detail) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(vendor, color = Color.White, fontSize = 14.sp)
                            Text(
                                text = detail.result ?: "No detail",
                                color = catColor,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // Zoomable Screenshot Dialog
    if (showFullScreenshot && state.urlResult?.screenshotUrl != null) {
        ZoomableImageDialog(
            url = state.urlResult!!.screenshotUrl!!,
            onDismiss = { showFullScreenshot = false }
        )
    }
}

@Composable
fun ReportContent(
    state: ReportDetailUiState,
    onCategoryClick: (String) -> Unit,
    onScreenshotClick: () -> Unit
) {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    val dateString = formatter.format(Date(state.timestamp))

    Text(
        text = "TARGET: ${state.target}",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = NeonCyan,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "DATE: $dateString",
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    )

    if (state.type == "APK" && state.apkDetails != null) {
        ApkReportLayout(state.apkDetails)
    } else if (state.type == "URL" && state.urlResult != null) {
        UrlReportLayout(state.urlResult!!, onCategoryClick, onScreenshotClick)
    } else if (state.type == "PHONE" && state.phoneResult != null) {
        PhoneReportLayout(state.phoneResult)
    } else if (state.type == "IMAGE" && state.imageResult != null) {
        ImageReportLayout(state.imageResult)
    } else if (state.type == "DOCUMENT" && state.docResult != null) {
        DocumentReportLayout(state.docResult!!)
    }
}

@Composable
fun DocumentReportLayout(result: com.shadowinspect.app.domain.model.DocumentAnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // We reuse the look and feel from DocumentScanScreen
        com.shadowinspect.app.presentation.screens.documentscan.DocRiskHeader(result)
        
        com.shadowinspect.app.presentation.screens.documentscan.DocCardSection("FILE IDENTITY", Icons.Default.Info) {
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("File Name", result.fileName)
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Size", "${result.fileSize} bytes")
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Format", result.format ?: result.documentType.name)
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("SHA-256", result.hashSha256)
        }

        com.shadowinspect.app.presentation.screens.documentscan.DocCardSection("AUTHORSHIP & METADATA", Icons.Default.Person) {
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Author", result.author)
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Company", result.company)
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Language", result.language)
        }

        com.shadowinspect.app.presentation.screens.documentscan.DocCardSection("CONTENT STATISTICS", Icons.Default.Analytics) {
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Pages", result.pageCount?.toString())
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Words", result.wordCount?.let { "%,d".format(it) })
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Characters", result.characterCount?.let { "%,d".format(it) })
            com.shadowinspect.app.presentation.screens.documentscan.MetaRow("Embedded Images", result.embeddedImageCount?.toString())
        }

        if (result.totalUrls > 0) {
            com.shadowinspect.app.presentation.screens.documentscan.DocCardSection("URLS (${result.totalUrls})", Icons.Default.Link) {
                if (result.embeddedUrls.isNotEmpty()) {
                    result.embeddedUrls.forEach { url ->
                        Text("• $url", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (result.containsSensitiveData) {
            com.shadowinspect.app.presentation.screens.documentscan.DocCardSection("SENSITIVE DATA", Icons.Default.Warning, borderColor = Color(0xFFFF9800)) {
                result.sensitiveDataMatches.forEach { (type, matches) ->
                    Text("$type:", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    matches.forEach { Text(" • $it", color = Color.White.copy(0.8f), fontSize = 10.sp) }
                }
            }
        }

        com.shadowinspect.app.presentation.screens.documentscan.DocCardSection("RECOMMENDATIONS", Icons.Default.Checklist) {
            result.recommendations.forEach { Text(it, color = Color.White.copy(0.85f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
    }
}

@Composable
fun ImageReportLayout(result: com.shadowinspect.app.domain.model.ImageAnalysisResult) {
    ImageScanResultCard(result = result)
}

@Composable
fun PhoneReportLayout(result: com.shadowinspect.app.domain.model.PhoneAnalysisResult) {
    com.shadowinspect.app.presentation.scan.PhoneResultCard(result = result)
}

@Composable
fun ApkReportLayout(details: ApkAnalysisResult) {
    EnhancedScanResultCard(result = details)
}

@Composable
fun UrlReportLayout(
    res: com.shadowinspect.app.domain.model.UrlScanResult,
    onCategoryClick: (String) -> Unit,
    onScreenshotClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        RiskScoreHeader(res)
        
        res.screenshotUrl?.let { 
            ScreenshotCard(it, res.pageTitle, onScreenshotClick) 
        }
        
        res.aiSummary?.let { 
            AiVerdictCard(it) 
        }
        
        AnalysisResultsGrid(res.stats, onCategoryClick)
        
        if (res.ipAddress != null) {
            HostingDetailsSection(res)
        }
        
        if (res.detectedTech.isNotEmpty()) {
            TechStackSection(res.detectedTech)
        }
        
        if (res.engineResults.isNotEmpty()) {
            EngineBreakdownSection(res.engineResults)
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        HorizontalDivider(modifier = Modifier.padding(top=4.dp), color = Color.DarkGray)
    }
}
