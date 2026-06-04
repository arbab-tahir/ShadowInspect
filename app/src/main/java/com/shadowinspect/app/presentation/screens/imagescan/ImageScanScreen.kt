package com.shadowinspect.app.presentation.screens.imagescan

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.presentation.components.ImageFilePicker
import com.shadowinspect.app.presentation.components.ImageScanResultCard
import com.shadowinspect.app.presentation.components.SelectedImageCard
import com.shadowinspect.app.presentation.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImageScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "IMAGE SECURITY ANALYZER",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Scan images for hidden data, metadata leaks, and steganography",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            if (state.selectedFile == null) {
                ImageFilePicker(
                    onImageSelected = { uri, file ->
                        viewModel.selectImage(uri, file)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "What we check:",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeonGreen
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        listOf(
                            "🔍 Hidden data (steganography)",
                            "📍 GPS coordinates in metadata",
                            "📷 Camera make/model information",
                            "🔗 Embedded URLs",
                            "🖼️ Image manipulation detection",
                            "📊 Color distribution analysis",
                            "📁 File size anomaly detection"
                        ).forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("•", color = NeonGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(feature, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                SelectedImageCard(
                    fileName = state.fileName,
                    fileSize = state.fileSize,
                    imageType = state.imageType,
                    onClear = { viewModel.clearSelection() }
                )

                OutlinedButton(
                    onClick = { viewModel.scanImage() },
                    enabled = !state.isScanning,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonGreen,
                        containerColor = Color.Black,
                        disabledContentColor = Color.Gray
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    if (state.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NeonGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ANALYZING IMAGE...", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    } else {
                        Text("▶ EXECUTE IMAGE SCAN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            state.scanResult?.let { result ->
                ImageScanResultCard(
                    result = result,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
