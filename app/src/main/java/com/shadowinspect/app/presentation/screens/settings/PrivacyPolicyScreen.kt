package com.shadowinspect.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowinspect.app.presentation.theme.NeonCyan
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "PRIVACY POLICY", 
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = NeonCyan
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last Updated: March 15, 2026",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            item {
                Text(
                    text = "Your privacy is critically important to us. ShadowInspect follows a strict 'Privacy First' architecture where analysis is performed locally on your device.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Text(
                    text = "Our Privacy Promise",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonGreen
                )
                Text(
                    text = "✅ NO data collection - We do not collect personal information\n" +
                           "✅ ON-DEVICE processing - All analysis stays on your phone\n" +
                           "✅ NO tracking - No analytics, no trackers, no fingerprints\n" +
                           "✅ NO selling - We never sell any information",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Permission Table - Responsive
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Permissions Explained",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Header
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(NeonGreen.copy(alpha = 0.1f))
                                .padding(8.dp)
                        ) {
                            Text("Permission", Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Purpose", Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Required", Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        listOf(
                            Triple("Internet", "Scan URLs against databases", "Yes"),
                            Triple("Storage", "Select app files to scan", "Yes"),
                            Triple("Camera", "QR code scanning (future)", "Optional"),
                            Triple("Alerts", "Notify about scan results", "Optional")
                        ).forEach { (perm, purp, req) ->
                            HorizontalDivider(color = NeonGreen.copy(alpha = 0.1f))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(perm, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
                                Text(purp, Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    req, 
                                    Modifier.weight(0.8f), 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (req == "Yes") NeonRed else NeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Information Processing",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonGreen
                )
                Text(
                    text = "The following information is processed ENTIRELY on your device and never leaves your phone:\n\n" +
                           "• Files You Choose to Scan (APK, PDF, Images)\n" +
                           "• Scan Results & Risk Scores\n" +
                           "• MITRE ATT&CK technique mappings\n" +
                           "• App Preferences & Learning Progress",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Text(
                    text = "Data Retention & Your Rights",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonGreen
                )
                Text(
                    text = "All data is stored locally. You have the right to access, delete, or export your data at any time through the Advanced settings menu.\n\n" +
                           "This app complies with GDPR, CCPA, and COPPA standards by design through data minimization and local processing.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = NeonCyan.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your trust matters. We built ShadowInspect to protect your privacy, not exploit it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
