package com.shadowinspect.app.domain.export

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.data.db.ScanEntity
import com.shadowinspect.app.domain.mitre.MitreDetection
import com.shadowinspect.app.domain.dashboard.ThreatTrend
import com.shadowinspect.app.domain.dashboard.TechniqueCount
import com.shadowinspect.app.domain.trends.ThreatPrediction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: ScanDao,
    private val mitreDao: MitreDetectionDao
) {
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * Export complete analytics as JSON
     */
    suspend fun exportCompleteAnalytics(
        config: ExportConfig,
        threatTrends: List<ThreatTrend>,
        topTechniques: List<TechniqueCount>,
        predictions: List<ThreatPrediction>,
        mitreDetections: List<com.shadowinspect.app.domain.mitre.MitreDetection>? = null,
        scanStats: com.shadowinspect.app.domain.export.ScanExportStats? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        
        val timeRange = getTimeRange(config)
        val scans = scanDao.getScansByDateRange(timeRange.first, timeRange.second)
        val mitreDetections = mitreDao.getDetectionsInDateRange(timeRange.first, timeRange.second)
        
        // Build complete export object
        val exportData = mapOf(
            "metadata" to mapOf(
                "exportedAt" to dateFormat.format(Date()),
                "version" to "1.0",
                "generatedBy" to "ShadowInspect Analytics"
            ),
            "timeRange" to mapOf(
                "start" to dateFormat.format(Date(timeRange.first)),
                "end" to dateFormat.format(Date(timeRange.second)),
                "days" to ((timeRange.second - timeRange.first) / (24 * 60 * 60 * 1000))
            ),
            "summary" to generateSummary(scans, mitreDetections),
            "scans" to scans.map { scan ->
                mapOf(
                    "id" to scan.id,
                    "timestamp" to dateFormat.format(Date(scan.timestamp)),
                    "type" to scan.scanType,
                    "target" to (scan.target ?: "Unknown"),
                    "riskScore" to scan.riskScore,
                    "riskLevel" to scan.riskLevel
                )
            },
            "mitreDetections" to mitreDetections.map { detection ->
                mapOf(
                    "id" to detection.id,
                    "scanId" to detection.scanId,
                    "timestamp" to dateFormat.format(Date(detection.detectedAt)),
                    "techniqueId" to detection.techniqueId,
                    "techniqueName" to detection.techniqueName,
                    "tactic" to detection.tactic,
                    "confidence" to detection.confidenceScore,
                    "evidence" to detection.evidence
                )
            },
            "trends" to threatTrends.map { trend ->
                mapOf(
                    "date" to trend.date,
                    "critical" to trend.critical,
                    "high" to trend.high,
                    "medium" to trend.medium,
                    "total" to trend.total
                )
            },
            "topTechniques" to topTechniques.map { tech ->
                mapOf(
                    "techniqueId" to tech.techniqueId,
                    "techniqueName" to tech.techniqueName,
                    "count" to tech.count,
                    "maxConfidence" to tech.maxConfidence
                )
            },
            "predictions" to predictions.map { pred ->
                mapOf(
                    "threatType" to pred.threatType,
                    "probability" to pred.probability,
                    "timeframe" to pred.timeframe.name,
                    "expectedSeverity" to pred.expectedSeverity
                )
            },
            "statistics" to mapOf(
                "averageRisk" to (scans.map { it.riskScore }.average()),
                "medianRisk" to calculateMedian(scans.map { it.riskScore }),
                "totalDetections" to mitreDetections.size,
                "uniqueTechniques" to mitreDetections.map { it.techniqueId }.distinct().size
            )
        )
        
        val fileName = "analytics_export_${fileDateFormat.format(Date())}.json"
        val exportsDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ShadowInspect")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val file = File(exportsDir, fileName)
        file.writeText(gson.toJson(exportData))
        
        ExportResult(
            filePath = file.absolutePath,
            fileSize = file.length(),
            format = ExportFormat.JSON,
            recordCount = scans.size,
            generatedAt = System.currentTimeMillis(),
            timeRange = formatTimeRange(timeRange.first, timeRange.second),
            checksum = calculateChecksum(file)
        )
    }
    
    private fun getTimeRange(config: ExportConfig): Pair<Long, Long> {
        val end = config.endDate ?: System.currentTimeMillis()
        val start = when (config.scope) {
            ExportScope.ALL_TIME -> 0L
            ExportScope.LAST_YEAR -> end - 365L * 24 * 60 * 60 * 1000
            ExportScope.LAST_6_MONTHS -> end - 180L * 24 * 60 * 60 * 1000
            ExportScope.LAST_3_MONTHS -> end - 90L * 24 * 60 * 60 * 1000
            ExportScope.LAST_MONTH -> end - 30L * 24 * 60 * 60 * 1000
            ExportScope.LAST_WEEK -> end - 7L * 24 * 60 * 60 * 1000
            ExportScope.CUSTOM_RANGE -> config.startDate ?: (end - 30L * 24 * 60 * 60 * 1000)
        }
        return Pair(start, end)
    }
    
    private fun generateSummary(scans: List<ScanEntity>, detections: List<MitreDetection>): Map<String, Any> {
        return mapOf(
            "totalScans" to scans.size,
            "totalDetections" to detections.size,
            "uniqueTechniques" to detections.map { it.techniqueId }.distinct().size,
            "riskDistribution" to mapOf(
                "critical" to scans.count { it.riskLevel == "CRITICAL" },
                "high" to scans.count { it.riskLevel == "HIGH" },
                "medium" to scans.count { it.riskLevel == "MEDIUM" },
                "low" to scans.count { it.riskLevel == "LOW" },
                "safe" to scans.count { it.riskLevel == "SAFE" }
            ),
            "scanTypeBreakdown" to mapOf(
                "apk" to scans.count { it.scanType == "APK" },
                "url" to scans.count { it.scanType == "URL" },
                "phone" to scans.count { it.scanType == "PHONE" }
            )
        )
    }
    
    private fun calculateMedian(numbers: List<Int>): Float {
        if (numbers.isEmpty()) return 0f
        val sorted = numbers.sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0f
        } else {
            sorted[sorted.size / 2].toFloat()
        }
    }
    
    private fun formatTimeRange(start: Long, end: Long): String {
        val startStr = dateFormat.format(Date(start))
        val endStr = dateFormat.format(Date(end))
        return "$startStr to $endStr"
    }
    
    private fun calculateChecksum(file: File): String {
        return java.security.MessageDigest.getInstance("MD5")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
    }
}
