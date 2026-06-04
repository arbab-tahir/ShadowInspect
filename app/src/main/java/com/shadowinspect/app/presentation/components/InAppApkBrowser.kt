package com.shadowinspect.app.presentation.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

/**
 * Simple in-app browser that asks for a directory (via SAF tree) and lists APK files inside.
 * Useful as a reliable fallback when external pickers fail on some OEMs/emulators.
 */
@Composable
fun InAppApkBrowser(onFileSelected: (Uri) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val treeUriState = remember { mutableStateOf<Uri?>(null) }
    val files = remember { mutableStateListOf<DocumentFile>() }
    val status = remember { mutableStateOf<String?>(null) }

    val treeLauncher = rememberLauncherForActivityResult(contract = OpenDocumentTree()) { uri ->
        uri?.let {
            try {
                ctx.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                treeUriState.value = it
                status.value = "Directory granted: ${it.lastPathSegment ?: it}"
            } catch (e: Exception) {
                status.value = "Failed to persist directory permission: ${e.message}"
            }
        } ?: run { status.value = "No directory selected" }
    }

    LaunchedEffect(treeUriState.value) {
        files.clear()
        treeUriState.value?.let { uri ->
            val doc = DocumentFile.fromTreeUri(ctx, uri)
            doc?.listFiles()?.filter { f ->
                val name = f.name?.lowercase() ?: ""
                (name.endsWith(".apk") || f.type == "application/vnd.android.package-archive") && f.isFile
            }?.forEach { files.add(it) }
            if (files.isEmpty()) status.value = "No APK files found in selected directory"
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Button(onClick = { treeLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Select directory to browse (Downloads recommended)")
        }

        status.value?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }

        if (files.isNotEmpty()) {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(files) { f ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onFileSelected(f.uri) }) {
                        Text(text = f.name ?: "Unknown", modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}
