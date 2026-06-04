package com.shadowinspect.app.presentation.screens.education

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowinspect.app.domain.education.MitreExplained
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MitreExplorerScreen(
    onNavigateBack: () -> Unit,
    viewModel: MitreExplorerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MITRE ATT&CK EXPLORER",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
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
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "Search techniques (T1529, SMS, camera...)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = NeonGreen.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = NeonGreen
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Intro card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.07f)),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "ABOUT MITRE ATT&CK", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MITRE ATT&CK is the global dictionary of hacker techniques. Each technique has an ID (e.g. T1529) and describes exactly how attackers operate. ShadowInspect maps app behaviors to these techniques.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Techniques list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.filteredTechniques.isEmpty()) {
                    item {
                        Text(
                            text = "No techniques match your search.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(state.filteredTechniques) { technique ->
                        MitreExplainedCard(technique = technique)
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun MitreExplainedCard(technique: MitreExplained) {
    val severityColor = when (technique.severityLevel) {
        "HIGH" -> NeonRed
        "MEDIUM" -> Color(0xFFFFA500)
        else -> NeonGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = technique.techniqueId,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = technique.techniqueName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = technique.severityLevel, style = MaterialTheme.typography.labelSmall, color = severityColor, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Simple explanation
            Text(
                text = technique.simpleExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Real world example
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonCyan.copy(alpha = 0.07f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(text = "REAL WORLD", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Text(text = technique.realWorldExample, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Protection tip
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Text("🛡️ ", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = technique.howToProtect,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonGreen.copy(alpha = 0.85f)
                )
            }
        }
    }
}
