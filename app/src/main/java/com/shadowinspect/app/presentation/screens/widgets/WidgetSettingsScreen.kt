package com.shadowinspect.app.presentation.screens.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.domain.widgets.WidgetConfig
import com.shadowinspect.app.domain.widgets.WidgetTemplate
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: WidgetSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "CUSTOMIZE DASHBOARD",
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
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefault() }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset")
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
            // Current widgets section
            item {
                Text(
                    text = "Current Widgets",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonGreen
                )
            }
            
            if (state.currentWidgets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No widgets added",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else {
                items(state.currentWidgets) { widget ->
                    CurrentWidgetItem(
                        widget = widget,
                        onVisibilityToggle = { viewModel.toggleWidgetVisibility(widget.id) },
                        onRemove = { viewModel.removeWidget(widget.id) }
                    )
                }
            }
            
            // Available widgets section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Add Widgets",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonGreen
                )
            }
            
            val availableWidgets = viewModel.getAvailableWidgets()
            items(availableWidgets) { template ->
                AvailableWidgetItem(
                    template = template,
                    onAdd = { viewModel.addWidget(template) }
                )
            }
            
            // Presets section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Dashboard Presets",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonGreen
                )
            }
            
            item {
                PresetCard(
                    name = "Security Focus",
                    description = "Focus on security scores and threats",
                    onSelect = { 
                        viewModel.applyPreset("security")
                        scope.launch {
                            snackbarHostState.showSnackbar("Security Focus preset applied!")
                        }
                    }
                )
            }
            
            item {
                PresetCard(
                    name = "Analytics Dashboard",
                    description = "Detailed analytics and predictions",
                    onSelect = { 
                        viewModel.applyPreset("analytics")
                        scope.launch {
                            snackbarHostState.showSnackbar("Analytics Dashboard preset applied!")
                        }
                    }
                )
            }
            
            item {
                PresetCard(
                    name = "Minimal",
                    description = "Clean and simple layout",
                    onSelect = { 
                        viewModel.applyPreset("minimal")
                        scope.launch {
                            snackbarHostState.showSnackbar("Minimal preset applied!")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CurrentWidgetItem(
    widget: WidgetConfig,
    onVisibilityToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = widget.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${widget.size} • ${widget.widgetType.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Row {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        if (widget.isVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle visibility"
                    )
                }
                
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AvailableWidgetItem(
    template: WidgetTemplate,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = template.icon,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = template.defaultTitle,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Size: ${template.defaultSize} • Category: ${template.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            OutlinedButton(
                onClick = onAdd,
                enabled = !template.isPremium,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (template.isPremium) Color.Gray else NeonGreen
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (template.isPremium) Color.Gray else NeonGreen,
                    containerColor = Color.Black
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (template.isPremium) "Premium" else "Add",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PresetCard(
    name: String,
    description: String,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            OutlinedButton(
                onClick = onSelect,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonGreen,
                    containerColor = Color.Black
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Apply",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
