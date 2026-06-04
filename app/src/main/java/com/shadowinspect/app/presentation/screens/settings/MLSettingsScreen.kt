package com.shadowinspect.app.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.domain.ml.MLModelMetadata
import com.shadowinspect.app.domain.ml.ModelType
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MLSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MLSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "ML DETECTION SETTINGS",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Model status
            item {
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
                            text = "ML Engine Status",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Initialized:")
                            Text(
                                text = if (state.isInitialized) "✅ Yes" else "❌ No",
                                color = if (state.isInitialized) NeonGreen else NeonRed
                            )
                        }
                        
                        if (!state.isInitializing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.reinitializeModels() },
                                modifier = Modifier.align(Alignment.End),
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Re-Initialize", color = Color.Black)
                            }
                        }
                        
                        if (state.isInitializing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        state.error?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = it,
                                color = if (it.contains("success", ignoreCase = true)) NeonGreen else NeonRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            // Model selection
            item {
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
                            text = "Active Model",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.activeModel == ModelType.CNN_LSTM,
                                onClick = { viewModel.selectModel(ModelType.CNN_LSTM) }
                            )
                            Text(
                                text = "CNN-LSTM (97.4% acc)",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.activeModel == ModelType.RANDOM_FOREST,
                                onClick = { viewModel.selectModel(ModelType.RANDOM_FOREST) }
                            )
                            Text(
                                text = "Random Forest (95.2% acc)",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.activeModel == ModelType.ENSEMBLE,
                                onClick = { viewModel.selectModel(ModelType.ENSEMBLE) }
                            )
                            Text(
                                text = "Ensemble (98.1% acc)",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
            
            // Available models
            item {
                Text(
                    text = "Available Models",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen
                )
            }
            
            items(state.availableModels) { model ->
                ModelInfoCard(model = model)
            }
            
            // Performance stats
            item {
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
                            text = "Performance Stats",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        state.performance.forEach { (type, perf) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text("${type.name}:", modifier = Modifier.weight(1f))
                                Text("${(perf.accuracy * 100).toInt()}% acc, ${perf.avgInferenceTime}ms")
                            }
                        }
                    }
                }
            }
            
            // Test ML button
            item {
                Button(
                    onClick = { viewModel.testMLWithSample() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    enabled = state.isInitialized && !state.isInitializing
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test ML with Sample Data", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun ModelInfoCard(model: MLModelMetadata) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = model.modelName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NeonGreen
                )
                Text(
                    text = "v${model.version}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = model.description ?: "",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MLStatChip("Accuracy", "${(model.accuracy * 100).toInt()}%")
                MLStatChip("Precision", "${(model.precision * 100).toInt()}%")
                MLStatChip("Recall", "${(model.recall * 100).toInt()}%")
                MLStatChip("F1", "${(model.f1Score * 100).toInt()}%")
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Size: ${model.sizeBytes / 1_000_000}MB",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun MLStatChip(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
