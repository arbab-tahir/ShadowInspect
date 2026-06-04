package com.shadowinspect.app.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.presentation.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionsExplainedScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions Guide", style = MaterialTheme.typography.headlineSmall) },
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
            item {
                Text(
                    text = "Understanding App Permissions",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonGreen
                )
            }

            item {
                Text(
                    text = "ShadowInspect requests minimal permissions to ensure your security while maintaining your privacy.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                PermissionDetailItem(
                    title = "Storage Access",
                    description = "Required to allow you to select APK files and documents from your device for local analysis. ShadowInspect only reads files you specifically select."
                )
            }

            item {
                PermissionDetailItem(
                    title = "Internet Access",
                    description = "Used to check URLs against threat intelligence databases and to download ML model updates. No personal data is transmitted over this connection."
                )
            }

            item {
                PermissionDetailItem(
                    title = "Notifications",
                    description = "Used to alert you when a background scan (like an APK install check) is complete and a potential threat is found."
                )
            }
        }
    }
}

@Composable
fun PermissionDetailItem(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, color = NeonGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
