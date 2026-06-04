package com.shadowinspect.app.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMLSettings: () -> Unit,
    onNavigateToLegal: (String) -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToLicenses: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SETTINGS",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- GENERAL ---
            SettingsSectionHeader("GENERAL")
            
            // About Card (Informational)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "About ShadowInspect",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version: 3.0.1\nSecurity analysis powered by MITRE ATT&CK® framework. Designed for advanced mobile threat intelligence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }

            // Advanced Settings Card (Navigational)
            SettingsCard(
                title = "Advanced Settings",
                subtitle = "System diagnostics, data maintenance, and core engine resets.",
                onClick = onNavigateToAdvanced
            )

            // --- SCAN ENGINES ---
            SettingsSectionHeader("SCAN ENGINES")

            SettingsCard(
                title = "ML Detection Engine",
                subtitle = "Configure on-device machine learning models for malware detection.",
                onClick = onNavigateToMLSettings
            )

            SettingsCard(
                title = "Storage & Data",
                subtitle = "Manage local scan history, database, and export locations.",
                onClick = onNavigateToStorage
            )

            // --- SECURITY ---
            SettingsSectionHeader("SECURITY")

            SettingsCard(
                title = "Security Settings",
                subtitle = "App permissions explained, data encryption, and vulnerability reporting.",
                onClick = onNavigateToSecurity
            )

            SettingsCard(
                title = "Data & Privacy",
                subtitle = "Review how we protect your privacy with local-only processing.",
                onClick = { onNavigateToLegal("privacy") }
            )

            // --- LEGAL & COMPLIANCE ---
            SettingsSectionHeader("LEGAL")

            SettingsCard(
                title = "Terms of Service",
                subtitle = "Read our official terms and user responsibilities.",
                onClick = { onNavigateToLegal("terms") }
            )

            SettingsCard(
                title = "GDPR Compliance",
                subtitle = "Data subject rights and compliance for EEA users.",
                onClick = { onNavigateToLegal("gdpr") }
            )

            SettingsCard(
                title = "Disclaimer",
                subtitle = "Limitations of liability and usage disclosures.",
                onClick = { onNavigateToLegal("disclaimer") }
            )

            SettingsCard(
                title = "Open Source Licenses",
                subtitle = "Third-party libraries and software attributions.",
                onClick = onNavigateToLicenses
            )

            // --- CREDITS ---
            SettingsSectionHeader("CREDITS")

            SettingsCard(
                title = "Copyright & Credits",
                subtitle = "Developer info, project credits, and legal ownership.",
                onClick = { onNavigateToLegal("copyright") }
            )

            SettingsCard(
                title = "MITRE ATT&CK® Attributions",
                subtitle = "Framework usage disclosures and trademark attributions.",
                onClick = { onNavigateToLegal("mitre") }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = NeonGreen,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.1f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )
        }
    }
}
