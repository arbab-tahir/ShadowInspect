package com.shadowinspect.app.presentation.screens.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.export.ExportConfig
import com.shadowinspect.app.domain.export.ExportFormat
import com.shadowinspect.app.domain.export.ExportMetadata
import com.shadowinspect.app.domain.export.ExportScope
import com.shadowinspect.app.domain.repository.DashboardRepository
import com.shadowinspect.app.domain.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    init {
        loadExports()
    }
    
    private fun loadExports() {
        viewModelScope.launch {
            exportRepository.getExports().collect { exports ->
                _uiState.value = _uiState.value.copy(
                    previousExports = exports
                )
            }
        }
    }
    
    fun quickExport(format: ExportFormat, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            
            try {
                val config = ExportConfig(
                    format = format,
                    scope = ExportScope.LAST_MONTH,
                    includeCharts = true,
                    includeRawData = true
                )
                
                // Get required data using snapshots
                val dashboardState = dashboardRepository.getDashboardStateSnapshot()
                val scanStats = dashboardRepository.getDashboardStateSnapshot().run {
                    com.shadowinspect.app.domain.export.ScanExportStats(
                        total = totalScans.toLong(),
                        highRisk = highRiskCount.toLong(),
                        safe = totalScans.toLong() - (highRiskCount.toLong() + criticalCount.toLong()),
                        avgRiskScore = securityScore.toFloat()
                    )
                }
                
                val result = exportRepository.export(
                    config = config,
                    threatTrends = dashboardState.threatTrends,
                    topTechniques = dashboardState.techniqueDistribution,
                    predictions = emptyList(), // Predictions would come from TrendAnalyzer if integrated here
                    scanStats = scanStats
                )
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportPath = result.filePath
                )
                
                Toast.makeText(
                    context,
                    "Export complete: ${result.filePath}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = e.message
                )
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun exportWithConfig(config: ExportConfig, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                // Get all required data using snapshots
                val dashboardState = dashboardRepository.getDashboardStateSnapshot()
                val mitreDetections = dashboardRepository.getMitreDetectionsSnapshot()
                val scanStats = dashboardRepository.getDashboardStateSnapshot().run {
                    com.shadowinspect.app.domain.export.ScanExportStats(
                        total = totalScans.toLong(),
                        highRisk = highRiskCount.toLong(),
                        safe = totalScans.toLong() - (highRiskCount.toLong() + criticalCount.toLong()),
                        avgRiskScore = securityScore.toFloat()
                    )
                }
                
                val result = exportRepository.export(
                    config = config,
                    threatTrends = dashboardState.threatTrends,
                    topTechniques = dashboardState.techniqueDistribution,
                    predictions = emptyList(),
                    mitreDetections = mitreDetections,
                    scanStats = scanStats
                )
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportPath = result.filePath
                )
                
                Toast.makeText(
                    context,
                    "Export complete: ${result.filePath}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = e.message
                )
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun shareExport(fileName: String, context: Context) {
        try {
            val intent = exportRepository.shareExport(fileName)
            context.startActivity(Intent.createChooser(intent, "Share Analytics Export"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun deleteExport(fileName: String) {
        viewModelScope.launch {
            exportRepository.deleteExport(fileName)
            loadExports()
        }
    }
    
    fun clearAllExports() {
        viewModelScope.launch {
            exportRepository.clearAllExports()
            loadExports()
        }
    }
    
    data class ExportUiState(
        val previousExports: List<ExportMetadata> = emptyList(),
        val isExporting: Boolean = false,
        val lastExportPath: String? = null,
        val error: String? = null
    )
}
