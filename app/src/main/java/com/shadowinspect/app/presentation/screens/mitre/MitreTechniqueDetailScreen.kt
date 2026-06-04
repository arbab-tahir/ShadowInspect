package com.shadowinspect.app.presentation.screens.mitre

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.domain.mitre.MitreTechnique
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.CyberSurface
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MitreTechniqueDetailScreen(
    techniqueId: String,
    onNavigateBack: () -> Unit,
    viewModel: MitreTechniqueDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    LaunchedEffect(techniqueId) {
        viewModel.loadTechnique(techniqueId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = state.technique?.id ?: "TECHNIQUE DETAILS",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        // Share technique text
                        val technique = state.technique
                        if (technique != null) {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "MITRE ATT&CK Technique: ${technique.id} - ${technique.name}\n\n${technique.description}\n\nRead more: ${technique.url}")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Technique Details"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else if (state.technique == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Technique not found", color = NeonRed)
            }
        } else {
            val technique = state.technique!!
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Technique Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CyberSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = technique.name.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = NeonGreen
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ID: ",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = technique.id,
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonGreen
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Tactics
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            technique.tactics.forEach { tactic ->
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = NeonGreen.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = tactic.uppercase(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonGreen
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CyberSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "DESCRIPTION",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = technique.description,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Platforms
                if (technique.platforms.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CyberSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "AFFECTED PLATFORMS",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            technique.platforms.forEach { platform ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Devices,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = NeonGreen.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = platform,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Permissions Required
                if (technique.permissionsRequired.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CyberSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "REQUIRED PERMISSIONS",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            technique.permissionsRequired.forEach { permission ->
                                Surface(
                                    modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth(),
                                    color = NeonRed.copy(alpha = 0.05f),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = permission,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonRed.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Detection
                if (!technique.detection.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CyberSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "DETECTION STRATEGY",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = technique.detection ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Mitigation
                if (!technique.mitigation.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CyberSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "MITIGATION STEPS",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonGreen
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = technique.mitigation ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Reference Link
                Button(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(technique.url)
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen.copy(alpha = 0.1f),
                        contentColor = NeonGreen
                    ),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIEW ON MITRE ATT&CK", style = MaterialTheme.typography.labelLarge)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
