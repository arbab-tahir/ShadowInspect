package com.shadowinspect.app.presentation.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shadowinspect.app.presentation.theme.NeonGreen
import com.shadowinspect.app.presentation.theme.NeonRed
import java.io.File
import java.io.FileOutputStream

@Composable
fun DocumentFilePicker(
    onDocumentSelected: (Uri, File) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Select Document"
) {
    val context = LocalContext.current
    
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                // Get file name from URI
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                var displayName = "document_${System.currentTimeMillis()}"
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            displayName = it.getString(nameIndex)
                        }
                    }
                }
                
                // Create a temporary copy with the original file extension
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, displayName)
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                inputStream?.close()
                
                onDocumentSelected(uri, tempFile)
                Toast.makeText(context, "Document selected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "No document selected", Toast.LENGTH_SHORT).show()
        }
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                documentPickerLauncher.launch("*/*")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen.copy(alpha = 0.2f),
                contentColor = NeonGreen
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText)
        }
        
        Text(
            text = "Supported: PDF, DOCX, XLSX, PPTX, TXT",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SelectedDocumentCard(
    fileName: String,
    fileSize: String,
    documentType: String?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val icon = when {
                    documentType?.contains("PDF") == true -> Icons.Default.PictureAsPdf
                    documentType?.contains("Word") == true -> Icons.Default.Description
                    documentType?.contains("Excel") == true -> Icons.Default.TableChart
                    documentType?.contains("PowerPoint") == true -> Icons.Default.Slideshow
                    else -> Icons.Default.InsertDriveFile
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        documentType?.contains("PDF") == true -> NeonRed
                        else -> NeonGreen
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                    Text(
                        text = "$fileSize • ${documentType ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = NeonRed
                )
            }
        }
    }
}
