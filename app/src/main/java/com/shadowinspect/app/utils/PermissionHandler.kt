package com.shadowinspect.app.utils

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Helper to check storage-related permissions across Android versions.
 */
class PermissionHandler(private val context: Context) {


    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val readImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
            val readVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
            val readAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
            readImages == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    readVideo == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    readAudio == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            val readExternal = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            readExternal == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPermissionExplanation(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "ShadowInspect needs media access so you can pick APK files from your device."
        } else {
            "ShadowInspect needs storage access so you can pick APK files from your device."
        }
    }
}

/**
 * Composable wrapper that ensures storage permission is granted before launching a file picker.
 */
@Composable
fun PermissionAwareFilePicker(
    onFileSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val handler = remember { PermissionHandler(ctx) }
    val hasPermission = remember { mutableStateOf(handler.checkStoragePermission()) }
    val status = remember { mutableStateOf<String?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    ctx.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}
                status.value = "File selected"
                onFileSelected(it)
            } ?: run {
                status.value = "No file selected"
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            val granted = perms.values.any { it }
            hasPermission.value = granted
            if (granted) {
                openDocumentLauncher.launch(arrayOf("*/*"))
            }
        }
    )

    LaunchedEffect(key1 = Unit) {
        hasPermission.value = handler.checkStoragePermission()
    }

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        if (!hasPermission.value) {
            Text(
                text = handler.getPermissionExplanation(),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = {
                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                permissionLauncher.launch(perms)
            }) {
                Text(text = "Grant storage permission")
            }
        } else {
            Button(onClick = { openDocumentLauncher.launch(arrayOf("*/*")) }) {
                Text(text = "Pick APK file")
            }
        }
        
        status.value?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
