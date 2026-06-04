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
fun DataEncryptionScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Encryption", style = MaterialTheme.typography.headlineSmall) },
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
                    text = "How Your Data is Protected",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonGreen
                )
            }

            item {
                EncryptionDetailItem(
                    title = "On-Device Storage",
                    description = "ShadowInspect uses Android's EncryptedSharedPreferences and an encrypted SQLite database for all local storage. This ensures that even if your device is compromised, your scan history remains private."
                )
            }

            item {
                EncryptionDetailItem(
                    title = "Network Transmission",
                    description = "When communicating with external threat databases (like VirusTotal), all data is transmitted over securely encrypted TLS 1.3 channels."
                )
            }

            item {
                EncryptionDetailItem(
                    title = "Hardware Security",
                    description = "Where available, ShadowInspect leverages the Android Keystore system and hardware-backed security modules (StrongBox) to manage cryptographic keys."
                )
            }
        }
    }
}

@Composable
fun EncryptionDetailItem(title: String, description: String) {
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
