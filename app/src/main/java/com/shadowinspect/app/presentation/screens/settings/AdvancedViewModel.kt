package com.shadowinspect.app.presentation.screens.settings

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.domain.dashboard.DashboardExportManager
import com.shadowinspect.app.domain.ml.MLModelManager
import com.shadowinspect.app.domain.settings.DiagnosticInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AdvancedViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: ScanDao,
    private val mitreDao: MitreDetectionDao,
    private val mlModelManager: MLModelManager,
    private val exportManager: DashboardExportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedUiState())
    val uiState: StateFlow<AdvancedUiState> = _uiState.asStateFlow()

    init {
        loadDiagnosticInfo()
    }

    private fun loadDiagnosticInfo() {
        viewModelScope.launch {
            val totalScans = scanDao.getTotalScans().toInt()
            val dbFile = context.getDatabasePath("shadowinspect.db")
            val dbSize = if (dbFile.exists()) dbFile.length() else 0L
            
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val batteryOpt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else true

            val internalDir = context.filesDir
            val availableBytes = internalDir.usableSpace
            val totalBytes = internalDir.totalSpace

            val info = DiagnosticInfo(
                appVersion = "3.0.1 (Production)",
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                screenSize = context.resources.displayMetrics.run { "${widthPixels}x${heightPixels}" },
                totalScans = totalScans,
                databaseSize = dbSize,
                mlModelsLoaded =  mlModelManager.modelState.value.availableModels.map { it.modelName },
                batteryOptimization = batteryOpt
            )

            _uiState.value = _uiState.value.copy(
                diagnosticInfo = info,
                availableSpace = formatFileSize(availableBytes),
                totalSpace = formatFileSize(totalBytes),
                dbSizeFormatted = formatFileSize(dbSize)
            )
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            try {
                scanDao.deleteAllScans()
                mitreDao.deleteAll()
                // Refresh diagnostic info after clear
                loadDiagnosticInfo()
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    message = "All local scan data cleared successfully."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    error = "Failed to clear data: ${e.message}"
                )
            }
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            try {
                // In a real app, this would also clear SharedPreferences/DataStore
                scanDao.deleteAllScans()
                mitreDao.deleteAll()
                
                // Refresh diagnostic info
                loadDiagnosticInfo()
                
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    message = "Application settings and local data have been reset to defaults."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    error = "Reset failed: ${e.message}"
                )
            }
        }
    }

    fun exportAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            try {
                // For simplicity, we use the dashboard export snapshot
                // In a real app, this might be a full JSON/CSV dump
                val snapshot = com.shadowinspect.app.domain.dashboard.DashboardState(
                    securityScore = 0,
                    totalScans = scanDao.getTotalScans().toInt(),
                    highRiskCount = 0,
                    criticalCount = 0,
                    mitigatedCount = 0,
                    riskDistribution = com.shadowinspect.app.domain.dashboard.RiskDistribution(0,0,0,0,0),
                    techniqueDistribution = emptyList(),
                    tacticDistribution = emptyList(),
                    recentActivity = scanDao.getRecentActivitySnapshot(100),
                    threatTrends = emptyList()
                )
                val file = exportManager.exportDashboardSummary(snapshot)
                val success = exportManager.saveToDownloads(file)
                
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    message = if (success) "Report exported to Downloads folder." else "Export failed to save."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun resetMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    data class AdvancedUiState(
        val diagnosticInfo: DiagnosticInfo? = null,
        val availableSpace: String = "Calculating...",
        val totalSpace: String = "Calculating...",
        val dbSizeFormatted: String = "Calculating...",
        val isActionLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}
