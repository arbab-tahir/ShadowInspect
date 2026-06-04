package com.shadowinspect.app.presentation.screens.imagescan

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.analyzer.ImageAnalyzer
import com.shadowinspect.app.domain.model.ImageAnalysisResult
import com.shadowinspect.app.data.db.ImageDao
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.ScanEntity
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImageScanViewModel @Inject constructor(
    private val imageAnalyzer: ImageAnalyzer,
    private val imageDao: ImageDao,
    private val scanDao: ScanDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageScanUiState())
    val uiState: StateFlow<ImageScanUiState> = _uiState.asStateFlow()

    private val tag = "ImageScanViewModel"
    private val gson = Gson()

    fun selectImage(uri: Uri, file: File) {
        viewModelScope.launch {
            try {
                val fileName = file.name
                val fileSize = file.length()
                val formattedSize = formatFileSize(fileSize)

                Log.d(tag, "Image selected: $fileName, Size: $formattedSize")

                _uiState.value = _uiState.value.copy(
                    selectedFileUri = uri,
                    selectedFile = file,
                    fileName = fileName,
                    fileSize = formattedSize,
                    imageType = detectImageType(fileName),
                    error = null
                )
            } catch (e: Exception) {
                Log.e(tag, "Error selecting image", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun scanImage() {
        val file = _uiState.value.selectedFile
        if (file == null) {
            _uiState.value = _uiState.value.copy(error = "No image selected")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                error = null,
                scanResult = null
            )

            try {
                val result = imageAnalyzer.analyzeImage(file)

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanResult = result
                )

                saveToDatabase(result)
            } catch (e: Exception) {
                android.util.Log.e("IMAGE_SCAN", "Failed to scan image", e)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = "Scan failed: ${e.message}"
                )
            }
        }
    }

    private fun saveToDatabase(result: ImageAnalysisResult) {
        viewModelScope.launch {
            try {
                // Save to image_scans table
                imageDao.insertScan(result)

                // Also save to main scans table for dashboard integration
                val scanEntity = ScanEntity(
                    scanType = "IMAGE",
                    target = result.fileName,
                    riskScore = result.riskScore,
                    riskLevel = result.riskLevel,
                    timestamp = result.timestamp,
                    detailsJson = gson.toJson(result)
                )
                scanDao.insert(scanEntity)

                Log.d(tag, "Saved to database")
            } catch (e: Exception) {
                Log.e(tag, "Error saving to database", e)
            }
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedFileUri = null,
            selectedFile = null,
            fileName = "",
            fileSize = "",
            imageType = null,
            scanResult = null
        )
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            scanResult = null,
            error = null
        )
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun detectImageType(fileName: String): String? {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "JPEG Image"
            "png" -> "PNG Image"
            "gif" -> "GIF Image"
            "bmp" -> "BMP Image"
            "webp" -> "WebP Image"
            "heic" -> "HEIC Image"
            else -> null
        }
    }
}

data class ImageScanUiState(
    val selectedFileUri: Uri? = null,
    val selectedFile: File? = null,
    val fileName: String = "",
    val fileSize: String = "",
    val imageType: String? = null,
    val isScanning: Boolean = false,
    val scanResult: ImageAnalysisResult? = null,
    val error: String? = null
)
