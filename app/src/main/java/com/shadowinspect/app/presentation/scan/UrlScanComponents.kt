package com.shadowinspect.app.presentation.scan

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import com.shadowinspect.app.domain.model.UrlAnalysisStats
import com.shadowinspect.app.domain.model.UrlScanResult
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@Composable
fun RiskScoreHeader(res: UrlScanResult) {
    val riskColor = getRiskColor(res.riskLevel)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(riskColor.copy(alpha = 0.1f))
            .border(1.dp, riskColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RISK SCORE: ${res.riskScore}/100",
            color = riskColor,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = res.riskLevel,
            color = riskColor,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = res.explanation,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ScreenshotCard(url: String, title: String?, onClick: () -> Unit) {
    Column {
        Text(
            text = "LIVE RENDER PREVIEW (Click to enlarge)",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            Box {
                AsyncImage(
                    model = url,
                    contentDescription = "Website Screenshot",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High
                )
                title?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImageDialog(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale *= zoomChange
            offset += offsetChange
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        val highResUrl = if (url.contains("urlscan.io")) "$url?width=1280" else url

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    })
                }
        ) {
            AsyncImage(
                model = highResUrl,
                contentDescription = "Zoomable Screenshot",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = maxOf(1f, scale),
                        scaleY = maxOf(1f, scale),
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state),
                contentScale = ContentScale.Fit
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        try {
                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(highResUrl))
                                .setTitle("Screenshot Download")
                                .setDescription("Downloading website render...")
                                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "screenshot_${System.currentTimeMillis()}.png")
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(true)

                            val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            downloadManager.enqueue(request)
                            
                            android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            
            Text(
                text = "Pinch to zoom • Double tap to reset",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
fun AiVerdictCard(aiText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, NeonCyan.copy(alpha=0.5f), RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = "AI", tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "GEMINI AI ANALYSIS",
                color = NeonCyan,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = aiText,
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun HostingDetailsSection(res: UrlScanResult) {
    Column {
        Text(
            text = "HOSTING & NETWORK",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(12.dp)) {
                    DetailRow("IP Address", res.ipAddress ?: "N/A")
                    DetailRow("Country", res.country ?: "N/A")
                    DetailRow("Server", res.server ?: "N/A")
                    DetailRow("ASN Name", res.asnName ?: "N/A")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp),
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(value)) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = NeonCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechStackSection(techs: List<String>) {
    Column {
        Text(
            text = "DETECTED TECHNOLOGIES",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            techs.forEach { tech ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonCyan.copy(alpha = 0.1f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tech,
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun EngineBreakdownSection(engines: Map<String, String>) {
    Column {
        Text(
            text = "SECURITY ENGINE BREAKDOWN",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                engines.forEach { (vendor, result) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = vendor,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(NeonRed.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = result.uppercase(),
                                color = NeonRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisResultsGrid(stats: UrlAnalysisStats, onCategoryClick: (String) -> Unit) {
    Column {
        Text(
            text = "ANALYSIS RESULTS (Click for details)",
            style = MaterialTheme.typography.titleSmall,
            color = NeonCyan,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick("Malicious") },
                label = "MALICIOUS",
                value = stats.malicious,
                color = if (stats.malicious > 0) NeonRed else NeonGreen
            )
            StatCard(
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick("Suspicious") },
                label = "SUSPICIOUS",
                value = stats.suspicious,
                color = if (stats.suspicious > 0) Color(0xFFFF9800) else NeonGreen
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick("Undetected") },
                label = "UNDETECTED",
                value = stats.undetected,
                color = Color.LightGray
            )
            StatCard(
                modifier = Modifier.weight(1f),
                onClick = { onCategoryClick("Harmless") },
                label = "HARMLESS",
                value = stats.harmless,
                color = NeonCyan
            )
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, onClick: () -> Unit, label: String, value: Int, color: Color) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun UrlInputSection(
    url: String,
    onUrlChange: (String) -> Unit,
    onScanClick: () -> Unit,
    isScanning: Boolean
) {
    Column {
        Text(
            text = "TARGET VECTOR",
            style = MaterialTheme.typography.titleSmall,
            color = NeonCyan,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { 
                Text(
                    "Enter URL to scan", 
                    color = NeonCyan.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                ) 
            },
            placeholder = { 
                Text(
                    "https://example.com", 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
            leadingIcon = {
                Icon(Icons.Default.Language, contentDescription = "URL", tint = NeonCyan)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                cursorColor = NeonGreen
            ),
            singleLine = true,
            enabled = !isScanning,
            shape = RoundedCornerShape(4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen.copy(alpha = 0.2f),
                contentColor = NeonGreen,
                disabledContainerColor = Color.DarkGray,
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NeonGreen),
            enabled = !isScanning && url.isNotBlank()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Execute", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "EXECUTE SCAN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun ErrorMessage(error: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(NeonRed.copy(alpha = 0.1f))
            .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "ERROR: $error",
            color = NeonRed,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun getRiskColor(level: String): Color {
    return when {
        level.contains("CRITICAL") -> NeonRed
        level.contains("HIGH") -> Color(0xFFFF5722)
        level.contains("MEDIUM") -> Color(0xFFFF9800)
        level.contains("LOW") -> NeonGreen
        level.contains("SAFE") -> NeonCyan
        else -> Color.Gray
    }
}
