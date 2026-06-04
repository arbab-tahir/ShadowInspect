package com.shadowinspect.app.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.OutlinedButton
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.CyberSurface
import com.shadowinspect.app.presentation.theme.NeonCyan

@Composable
fun FilePickerButton(
    onFileSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Select File",
    mimeType: String = "*/*"
) {
    val launcher = rememberLauncherForActivityResult(contract = GetContent()) { uri: Uri? ->
        uri?.let { onFileSelected(it) }
    }

    val isLoading = remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            launcher.launch(mimeType)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = NeonGreen,
            containerColor = Color.Black
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NeonGreen)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = buttonText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
        if (isLoading.value) {
            Spacer(modifier = Modifier.width(12.dp))
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonGreen)
        }
    }
}

@Composable
fun SelectedFileInfo(
    fileName: String,
    fileSize: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = fileName, color = NeonCyan)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = fileSize, style = MaterialTheme.typography.bodySmall)
            }

            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = "Clear selection")
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
