package com.shadowinspect.app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.mitre.MitreAnalysisResult
import com.shadowinspect.app.domain.mitre.ReportTemplate
import com.shadowinspect.app.domain.report.MitreReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MitreReportViewModel @Inject constructor(
    val reportGenerator: MitreReportGenerator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
    
    fun generateReport(
        analysisResult: MitreAnalysisResult,
        scanTarget: String,
        scanType: String,
        scanId: Long,
        template: ReportTemplate = ReportTemplate.EDUCATIONAL
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null
            )
            
            try {
                if (template == ReportTemplate.JSON_DATA) {
                    // Generate ONLY JSON (Raw Data)
                    val jsonFile = reportGenerator.generateJsonReport(
                        analysisResult = analysisResult,
                        scanTarget = scanTarget,
                        scanType = scanType,
                        scanId = scanId
                    )
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedJsonFile = jsonFile,
                        generatedReports = reportGenerator.getGeneratedReports()
                    )
                } else {
                    // Generate BOTH PDF and JSON (Standard for comprehensive)
                    val pdfFile = reportGenerator.generatePdfReport(
                        analysisResult = analysisResult,
                        scanTarget = scanTarget,
                        scanType = scanType,
                        scanId = scanId,
                        template = template
                    )
                    
                    // Also generate JSON in the background for consistency
                    val jsonFile = reportGenerator.generateJsonReport(
                        analysisResult = analysisResult,
                        scanTarget = scanTarget,
                        scanType = scanType,
                        scanId = scanId
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedPdfFile = pdfFile,
                        generatedJsonFile = jsonFile,
                        generatedReports = reportGenerator.getGeneratedReports()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "Report generation failed: ${e.message}"
                )
            }
        }
    }
    
    fun loadReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                generatedReports = reportGenerator.getGeneratedReports()
            )
        }
    }
    
    fun clearGeneratedFiles() {
        _uiState.value = _uiState.value.copy(
            generatedPdfFile = null,
            generatedJsonFile = null
        )
    }
    
    fun cleanupOldReports() {
        viewModelScope.launch {
            reportGenerator.cleanupOldReports()
            loadReports()
        }
    }

    fun deleteReport(file: File) {
        viewModelScope.launch {
            reportGenerator.deleteReport(file)
            loadReports()
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            reportGenerator.deleteAllReports()
            loadReports()
        }
    }

    suspend fun saveToPublicDownloads(file: File): Boolean {
        return reportGenerator.saveToDownloads(file)
    }
    
    data class ReportUiState(
        val isGenerating: Boolean = false,
        val generatedPdfFile: File? = null,
        val generatedJsonFile: File? = null,
        val generatedReports: List<File> = emptyList(),
        val error: String? = null
    )
}
