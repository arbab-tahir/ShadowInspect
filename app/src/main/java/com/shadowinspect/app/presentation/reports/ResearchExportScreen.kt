package com.shadowinspect.app.presentation.reports

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowinspect.app.presentation.scan.HeaderSection
import com.shadowinspect.app.presentation.theme.*
import com.shadowinspect.app.domain.research.ResearchStatistics
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchExportScreen(
    onBack: () -> Unit,
    viewModel: ResearchExportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    LaunchedEffect(state.exportedFile) {
        state.exportedFile?.let { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Research Data"))
            viewModel.clearExportedFile()
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "ACADEMIC RESEARCH DATA",
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            HeaderSection("RESEARCH INSIGHTS", "Anonymous aggregate data collected for academic paper support.")
            
            state.stats?.let { stats ->
                ResearchSummarySection(stats)
                
                RiskDistributionCard(stats.riskDistribution)
                
                TopTechniquesTable(stats.topTechniques)
                
                ExportActionsSection(
                    onExportCsv = { viewModel.exportCsv() },
                    onGenerateLatex = { viewModel.generateLatex() }
                )
                
                state.latexTables?.let { latex ->
                    LatexOutputResult(latex, onCopy = {
                        clipboardManager.setText(AnnotatedString(latex))
                        Toast.makeText(context, "LaTeX copied to clipboard", Toast.LENGTH_SHORT).show()
                    })
                }
            } ?: run {
                if (!state.isLoading) {
                    Text("No research data collected yet. Start scanning apps to generate data.", color = Color.Gray, modifier = Modifier.padding(32.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ResearchSummarySection(stats: ResearchStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("OVERALL METRICS", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("TOTAL SCANS", stats.totalScans.toString())
                StatItem("DETECTIONS", stats.totalTechniques.toString())
                StatItem("ACCURACY", "${(stats.detectionRate * 100).toInt()}%")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Avg Risk Score: ${"%.1f".format(stats.averageRiskScore)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (stats.averageRiskScore > 50) NeonRed else NeonGreen,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
fun RiskDistributionCard(distribution: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("RISK LEVEL DISTRIBUTION", style = MaterialTheme.typography.labelSmall, color = NeonGreen)
            Spacer(modifier = Modifier.height(16.dp))
            
            val total = distribution.values.sum().toFloat()
            distribution.forEach { (level, count) ->
                val progress = if (total > 0) count / total else 0f
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(level, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("$count", style = MaterialTheme.typography.bodySmall, color = NeonGreen)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = when(level.uppercase()) {
                            "HIGH", "CRITICAL" -> NeonRed
                            "MEDIUM" -> Color.Yellow
                            else -> NeonGreen
                        },
                        trackColor = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun TopTechniquesTable(techniques: List<com.shadowinspect.app.domain.research.TechniqueStat>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("TOP MITRE TECHNIQUES", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            Spacer(modifier = Modifier.height(12.dp))
            
            techniques.take(5).forEach { tech ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        tech.techniqueId, 
                        modifier = Modifier.width(60.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = NeonCyan
                    )
                    Text(
                        tech.techniqueName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        "${tech.detectionCount}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ExportActionsSection(onExportCsv: () -> Unit, onGenerateLatex: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExportCsv,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.1f), contentColor = NeonGreen),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("CSV EXPORT")
        }
        
        Button(
            onClick = onGenerateLatex,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.1f), contentColor = NeonCyan),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("LATEX CODE")
        }
    }
}

@Composable
fun LatexOutputResult(latex: String, onCopy: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LATEX SOURCE", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        }
        
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = latex,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}
