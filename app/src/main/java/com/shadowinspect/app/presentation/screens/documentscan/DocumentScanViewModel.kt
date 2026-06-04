package com.shadowinspect.app.presentation.screens.documentscan

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.data.db.DocumentDao
import com.shadowinspect.app.domain.analyzer.DocumentAnalyzer
import com.shadowinspect.app.domain.model.DocumentAnalysisResult
import com.shadowinspect.app.domain.model.DocumentType
import com.google.gson.Gson
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.ScanEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject

data class DocumentScanUiState(
    val selectedFileUri: Uri? = null,
    val fileName: String = "",
    val fileSize: String = "",
    val documentType: DocumentType = DocumentType.UNKNOWN,
    val isScanning: Boolean = false,
    val showLoadingWarning: Boolean = false,
    val scanResult: DocumentAnalysisResult? = null,
    val lastScanId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class DocumentScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentAnalyzer: DocumentAnalyzer,
    private val documentDao: DocumentDao,
    private val scanDao: ScanDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentScanUiState())
    val uiState: StateFlow<DocumentScanUiState> = _uiState.asStateFlow()

    fun selectFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val (name, size) = getFileInfo(uri)
                val docType = detectTypeFromName(name)
                _uiState.value = DocumentScanUiState(
                    selectedFileUri = uri,
                    fileName        = name,
                    fileSize        = formatSize(size),
                    documentType    = docType,
                    error           = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Could not read file: ${e.message}")
            }
        }
    }

    fun clearSelection() { _uiState.value = DocumentScanUiState() }

    fun scanSelectedFile() {
        val uri = _uiState.value.selectedFileUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, showLoadingWarning = false, error = null)
            
            // Timeout warning timer
            val warningJob = launch {
                kotlinx.coroutines.delay(10_000)
                _uiState.value = _uiState.value.copy(showLoadingWarning = true)
            }

            try {
                val file = copyUriToTempFile(uri)
                val result = documentAnalyzer.analyzeDocument(file)
                
                warningJob.cancel()
                
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    showLoadingWarning = false,
                    scanResult = result,
                    error      = if (result.isError) result.errorMessage else null
                )
                if (!result.isError) {
                    val id = saveToDatabase(result)
                    _uiState.value = _uiState.value.copy(lastScanId = id)
                }
                // Clean up temp file
                try { file.delete() } catch (_: Exception) {}
            } catch (e: Exception) {
                warningJob.cancel()
                Log.e("DocScanVM", "Scan failed", e)
                _uiState.value = _uiState.value.copy(isScanning = false, showLoadingWarning = false, error = e.message ?: "Scan failed")
            }
        }
    }

    private suspend fun saveToDatabase(result: DocumentAnalysisResult): Long {
        return try { 
            val docId = documentDao.insertScan(result) 
            
            val genericScan = ScanEntity(
                scanType = ScanEntity.SCAN_TYPE_DOCUMENT,
                target = result.fileName,
                riskScore = result.riskScore,
                riskLevel = result.riskLevel,
                timestamp = result.timestamp,
                detailsJson = Gson().toJson(result)
            )
            scanDao.insert(genericScan)
            
            docId
        } catch (e: Exception) { -1L }
    }

    private suspend fun copyUriToTempFile(uri: Uri): File = withContext(Dispatchers.IO) {
        val (name, _) = getFileInfo(uri)
        val ext  = name.substringAfterLast(".", "tmp")
        val temp = File(context.cacheDir, "doc_scan_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        }
        temp
    }

    private suspend fun getFileInfo(uri: Uri): Pair<String, Long> = withContext(Dispatchers.IO) {
        var name = "document"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        Pair(name, size)
    }

    private fun detectTypeFromName(name: String): DocumentType = when (name.substringAfterLast(".").lowercase()) {
        "pdf"  -> DocumentType.PDF
        "docx" -> DocumentType.DOCX
        "xlsx" -> DocumentType.XLSX
        "pptx" -> DocumentType.PPTX
        "doc"  -> DocumentType.DOC
        "xls"  -> DocumentType.XLS
        "ppt"  -> DocumentType.PPT
        "txt"  -> DocumentType.TXT
        "rtf"  -> DocumentType.RTF
        "odt"  -> DocumentType.ODT
        else   -> DocumentType.UNKNOWN
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B","KB","MB","GB")
        val g = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return "${DecimalFormat("#.##").format(bytes / Math.pow(1024.0, g.toDouble()))} ${units[g]}"
    }
}
