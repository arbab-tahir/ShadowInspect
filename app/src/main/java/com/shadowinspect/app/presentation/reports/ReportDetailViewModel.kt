package com.shadowinspect.app.presentation.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.data.scan.ScanRepository
import com.shadowinspect.app.domain.model.ApkAnalysisResult
import com.shadowinspect.app.domain.model.UrlScanResult
import com.shadowinspect.app.domain.model.PhoneAnalysisResult
import com.shadowinspect.app.domain.model.DocumentAnalysisResult
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportDetailUiState(
    val isLoading: Boolean = true,
    val reportId: Long = -1,
    val target: String = "",
    val type: String = "",
    val riskScore: Int = 0,
    val riskLevel: String = "",
    val timestamp: Long = 0,
    val apkDetails: ApkAnalysisResult? = null,
    val urlResult: UrlScanResult? = null,
    val phoneResult: PhoneAnalysisResult? = null,
    val imageResult: com.shadowinspect.app.domain.model.ImageAnalysisResult? = null,
    val docResult: DocumentAnalysisResult? = null,
    val error: String? = null
)

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val repository: ScanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = _uiState.asStateFlow()

    init {
        val idString: String? = savedStateHandle["reportId"]
        val id = idString?.toLongOrNull()
        if (id != null) {
            loadReport(id)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid Report ID"
            )
        }
    }

    private val gson = Gson()

    private fun loadReport(id: Long) {
        viewModelScope.launch {
            try {
                val entity = repository.getScanById(id)
                if (entity != null) {
                    var apkDetails: ApkAnalysisResult? = null
                    var urlScanResult: UrlScanResult? = null
                    var phoneResult: PhoneAnalysisResult? = null
                    var docResult: DocumentAnalysisResult? = null

                    when (entity.scanType) {
                        "APK" -> {
                            apkDetails = try {
                                gson.fromJson(entity.detailsJson, ApkAnalysisResult::class.java)
                            } catch (e: Exception) { null }
                        }
                        "URL" -> {
                            urlScanResult = try {
                                gson.fromJson(entity.detailsJson, UrlScanResult::class.java)
                            } catch (e: Exception) { null }
                        }
                        "PHONE" -> {
                            phoneResult = try {
                                gson.fromJson(entity.detailsJson, PhoneAnalysisResult::class.java)
                            } catch (e: Exception) { null }
                        }
                        "IMAGE" -> {
                            try {
                                val imageRes = gson.fromJson(entity.detailsJson, com.shadowinspect.app.domain.model.ImageAnalysisResult::class.java)
                                _uiState.value = _uiState.value.copy(imageResult = imageRes)
                            } catch (e: Exception) { null }
                        }
                        "DOCUMENT" -> {
                            docResult = try {
                                gson.fromJson(entity.detailsJson, DocumentAnalysisResult::class.java)
                            } catch (e: Exception) { null }
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reportId = entity.id,
                        target = entity.target,
                        type = entity.scanType,
                        riskScore = entity.riskScore,
                        riskLevel = entity.riskLevel,
                        timestamp = entity.timestamp,
                        apkDetails = apkDetails,
                        urlResult = urlScanResult,
                        phoneResult = phoneResult,
                        docResult = docResult
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Report not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load report: ${e.message}"
                )
            }
        }
    }
}
