package com.shadowinspect.app.presentation.scan

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.analyzer.ApkAnalyzer
import com.shadowinspect.app.domain.model.ApkAnalysisResult
import com.shadowinspect.app.domain.model.ApkPermission
import com.shadowinspect.app.data.scan.ScanRepository
import com.shadowinspect.app.data.db.ScanEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import com.shadowinspect.app.domain.mitre.MitreDetection
import com.shadowinspect.app.domain.repository.MitreRepository
import com.shadowinspect.app.domain.repository.ResearchRepository
import javax.inject.Inject

data class FileScanUiState(
    val selectedFileUri: Uri? = null,
    val fileName: String = "",
    val fileSize: String = "",
    val isScanning: Boolean = false,
    val scanResult: ApkAnalysisResult? = null,
    val lastScanId: Long? = null,
    val error: String? = null,
    val permissions: List<ApkPermission> = emptyList()
)

@HiltViewModel
class FileScanViewModel @Inject constructor(
    private val apkAnalyzer: ApkAnalyzer,
    private val scanRepository: ScanRepository,
    private val mitreRepository: MitreRepository,
    private val researchRepository: ResearchRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            // Test MITRE analyzer with sample permissions
            try {
                mitreRepository.getAllTechniques().collect { techniques ->
                    Log.d("MITRE_INIT", "Techniques loaded: ${techniques.size}")
                }
            } catch (e: Exception) {
                Log.e("MITRE_INIT", "Failed to load techniques", e)
            }
        }
    }

    private val _uiState = MutableStateFlow(FileScanUiState())
    val uiState: StateFlow<FileScanUiState> = _uiState.asStateFlow()

    fun selectFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val info = apkAnalyzer.probeApk(uri)
                val name = info?.fileName ?: uri.lastPathSegment ?: "unknown.apk"
                val size = info?.fileSize ?: 0L
                _uiState.value = _uiState.value.copy(
                    selectedFileUri = uri,
                    fileName = name,
                    fileSize = formatSize(size),
                    error = null,
                    scanResult = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to read file")
            }
        }
    }

    fun clearSelection() {
        _uiState.value = FileScanUiState()
    }

    fun scanSelectedFile() {
        val uri = _uiState.value.selectedFileUri ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, error = null)
            try {
                val result = apkAnalyzer.analyzeApk(uri)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanResult = result,
                    permissions = result.permissions,
                    error = if (result.isError) result.errorMessage else null
                )
                if (!result.isError) {
                    val scanId = saveToDatabase(result)
                    _uiState.value = _uiState.value.copy(lastScanId = scanId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isScanning = false, error = e.message ?: "Scan failed")
            }
        }
    }

    fun submitFeedback(scanId: Long, wasAccurate: Boolean, comments: String? = null) {
        viewModelScope.launch {
            researchRepository.submitFeedback(scanId, wasAccurate, comments)
        }
    }

    private suspend fun saveToDatabase(result: ApkAnalysisResult): Long {
        return try {
            val entity = ScanEntity(
                scanType = "APK",
                target = result.fileName,
                riskScore = result.riskScore,
                riskLevel = result.riskLevel,
                detailsJson = com.google.gson.Gson().toJson(result)
            )
            val scanId = scanRepository.saveScan(entity)
            
            // Save MITRE detections
            result.mitreAnalysis?.let { mitre ->
                if (mitre.detectedTechniques.isNotEmpty()) {
                    val detections = mitre.detectedTechniques.map { tech ->
                        MitreDetection(
                            scanId = scanId,
                            scanType = "APK",
                            techniqueId = tech.technique.id,
                            techniqueName = tech.technique.name,
                            tactic = tech.technique.tactics.firstOrNull() ?: "Unknown",
                            confidenceScore = tech.confidence,
                            evidence = tech.evidence,
                            detectedAt = System.currentTimeMillis()
                        )
                    }
                    mitreRepository.saveMitreDetections(detections)
                    
                    // Collect research data with full enrichment
                    val dangerousPerms = result.dangerousPermissions.map { it.name }
                    val appCategory = inferAppCategory(result.packageName, result.permissions.map { it.name })
                    researchRepository.collectResearchData(
                        scanId = scanId,
                        scanType = "APK",
                        mitreResult = mitre,
                        appCategory = appCategory,
                        dangerousPermissions = dangerousPerms,
                        isDebuggable = result.isDebuggable,
                        virusTotalVerdict = "Not Checked" // updated later by VirusTotal integration
                    )

                }
            }
            scanId
        } catch (e: Exception) {
            -1L
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#.##")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    /**
     * Infers a broad app category from the package name and declared permissions.
     * This is a lightweight heuristic — no network call needed.
     */
    private fun inferAppCategory(packageName: String?, permissions: List<String>): String {
        val pkg = packageName?.lowercase() ?: ""
        val perms = permissions.map { it.lowercase() }

        return when {
            pkg.contains("bank") || pkg.contains("pay") || pkg.contains("wallet") ||
                pkg.contains("finance") || pkg.contains("money") ||
                perms.any { it.contains("nfc") }
            -> "Finance"

            pkg.contains("social") || pkg.contains("chat") || pkg.contains("message") ||
                pkg.contains("messenger") || pkg.contains("whatsapp") || pkg.contains("telegram") ||
                (perms.contains("android.permission.read_contacts") && perms.contains("android.permission.send_sms"))
            -> "Social / Messaging"

            pkg.contains("game") || pkg.contains("play") || pkg.contains("arcade") ||
                pkg.contains("puzzle")
            -> "Gaming"

            pkg.contains("health") || pkg.contains("medical") || pkg.contains("fitness") ||
                pkg.contains("doctor") || pkg.contains("pharma")
            -> "Health & Fitness"

            pkg.contains("shop") || pkg.contains("store") || pkg.contains("market") ||
                pkg.contains("commerce") || pkg.contains("amazon") || pkg.contains("ebay")
            -> "Shopping"

            pkg.contains("vpn") || pkg.contains("security") || pkg.contains("antivirus") ||
                pkg.contains("protect")
            -> "Security / VPN"

            pkg.contains("news") || pkg.contains("media") || pkg.contains("video") ||
                pkg.contains("music") || pkg.contains("stream") || pkg.contains("youtube")
            -> "Media & Entertainment"

            pkg.contains("map") || pkg.contains("navigation") || pkg.contains("location") ||
                pkg.contains("gps") || perms.contains("android.permission.access_fine_location")
            -> "Navigation"

            pkg.contains("edu") || pkg.contains("learn") || pkg.contains("school") ||
                pkg.contains("study") || pkg.contains("tutor")
            -> "Education"

            pkg.contains("tool") || pkg.contains("util") || pkg.contains("manager") ||
                pkg.contains("cleaner") || pkg.contains("booster")
            -> "Utility / Tools"

            else -> "Unknown"
        }
    }
}
