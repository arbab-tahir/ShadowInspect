package com.shadowinspect.app.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.shadowinspect.app.presentation.components.HackerLoadingAnimation
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UrlScanScreen(
    onBack: () -> Unit,
    viewModel: UrlScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showFullScreenshot by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "URL INSPECTION MODULE",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                UrlInputSection(
                    url = uiState.urlInput,
                    onUrlChange = viewModel::onUrlInputChanged,
                    onScanClick = viewModel::scanUrl,
                    isScanning = uiState.isScanning
                )
            }

            if (uiState.isScanning) {
                item {
                    HackerLoadingAnimation(statusText = "EXECUTION URL SCAN")
                }
            } else if (uiState.scanResult != null) {
                val res = uiState.scanResult!!
                
                // 1. Risk Score Header
                item {
                    RiskScoreHeader(res)
                }

                // 2. Screenshot Section (If available)
                res.screenshotUrl?.let { url ->
                    item {
                        ScreenshotCard(url, res.pageTitle) { showFullScreenshot = true }
                    }
                }

                // 3. AI Summary
                res.aiSummary?.let { aiText ->
                    item {
                        AiVerdictCard(aiText)
                    }
                }

                // 4. Basic VT stats
                item {
                    AnalysisResultsGrid(res.stats) { category ->
                        selectedCategory = category
                    }
                }

                // 5. Advanced Hosting Details
                if (res.ipAddress != null) {
                    item {
                        HostingDetailsSection(res)
                    }
                }

                // 6. Technology Stack
                if (res.detectedTech.isNotEmpty()) {
                    item {
                        TechStackSection(res.detectedTech)
                    }
                }

                // 7. Security Engine Breakdown
                if (res.engineResults.isNotEmpty()) {
                    item {
                        EngineBreakdownSection(res.engineResults)
                    }
                }
            }

            uiState.error?.let { error ->
                item {
                    ErrorMessage(error)
                }
            }
            
            if (uiState.scanResult != null || uiState.error != null) {
                item {
                    Button(
                        onClick = viewModel::clearResult,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("CLEAR RESULTS", fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }

    // Engine Details Modal
    if (selectedCategory != null && uiState.scanResult != null) {
        val category = selectedCategory!!
        val res = uiState.scanResult!!
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
    if (showFullScreenshot && uiState.scanResult?.screenshotUrl != null) {
        ZoomableImageDialog(
            url = uiState.scanResult!!.screenshotUrl!!,
            onDismiss = { showFullScreenshot = false }
        )
    }
}
