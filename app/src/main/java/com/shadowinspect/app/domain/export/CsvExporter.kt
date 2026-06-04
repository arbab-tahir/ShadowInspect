package com.shadowinspect.app.domain.export

import android.content.Context
import com.google.gson.Gson
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.domain.dashboard.ThreatTrend
import com.shadowinspect.app.domain.dashboard.TechniqueCount
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: ScanDao,
    private val mitreDao: MitreDetectionDao
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * Export scans to CSV
     */
    suspend fun exportScans(
        config: ExportConfig
    ): ExportResult = withContext(Dispatchers.IO) {
        
        val timeRange = getTimeRange(config)
        val scans = scanDao.getScansByDateRange(timeRange.first, timeRange.second)
        
        val fileName = "scans_export_${fileDateFormat.format(Date())}.csv"
        val exportDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ShadowInspect")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("ID,Timestamp,Type,Target,Risk Score,Risk Level,Details\n")
            
            // Write data
            scans.forEach { scan ->
                val line = listOf(
                    scan.id.toString(),
                    dateFormat.format(Date(scan.timestamp)),
                    scan.scanType,
                    scan.target ?: "N/A",
                    scan.riskScore.toString(),
                    scan.riskLevel,
                    scan.detailsJson?.take(50) ?: "N/A"
                ).joinToString(",") { field ->
                    "\"${field.replace("\"", "\"\"")}\""
                }
                writer.append(line)
                writer.append("\n")
            }
        }
        
        ExportResult(
            filePath = file.absolutePath,
            fileSize = file.length(),
            format = ExportFormat.CSV,
            recordCount = scans.size,
            generatedAt = System.currentTimeMillis(),
            timeRange = formatTimeRange(timeRange.first, timeRange.second),
            checksum = calculateChecksum(file)
        )
    }
    
    /**
     * Export MITRE detections to CSV
     */
    suspend fun exportMitreDetections(
        config: ExportConfig
    ): ExportResult = withContext(Dispatchers.IO) {
        
        val timeRange = getTimeRange(config)
        val detections = mitreDao.getDetectionsInDateRange(timeRange.first, timeRange.second)
        
        val fileName = "mitre_export_${fileDateFormat.format(Date())}.csv"
        val exportDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ShadowInspect")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("ID,Timestamp,Scan ID,Technique ID,Technique Name,Tactic,Confidence,Evidence\n")
            
            // Write data
            detections.forEach { detection ->
                val line = listOf(
                    detection.id.toString(),
                    dateFormat.format(Date(detection.detectedAt)),
                    detection.scanId.toString(),
                    detection.techniqueId,
                    detection.techniqueName,
                    detection.tactic,
                    detection.confidenceScore.toString(),
                    detection.evidence.joinToString("; ")
                ).joinToString(",") { field ->
                    "\"${field.replace("\"", "\"\"")}\""
                }
                writer.append(line)
                writer.append("\n")
            }
        }
        
        ExportResult(
            filePath = file.absolutePath,
            fileSize = file.length(),
            format = ExportFormat.CSV,
            recordCount = detections.size,
            generatedAt = System.currentTimeMillis(),
            timeRange = formatTimeRange(timeRange.first, timeRange.second),
            checksum = calculateChecksum(file)
        )
    }
    
    /**
     * Export comprehensive analytics
     */
    suspend fun exportAnalytics(
        config: ExportConfig,
        threatTrends: List<ThreatTrend>,
        topTechniques: List<TechniqueCount>
    ): ExportResult = withContext(Dispatchers.IO) {
        
        val timeRange = getTimeRange(config)
        val scans = scanDao.getScansByDateRange(timeRange.first, timeRange.second)
        
        val fileName = "analytics_export_${fileDateFormat.format(Date())}.csv"
        val exportDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ShadowInspect")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)
        
        FileWriter(file).use { writer ->
            // Summary section
            writer.append("=== ANALYTICS SUMMARY ===\n")
            writer.append("Generated,${dateFormat.format(Date())}\n")
            writer.append("Period,${formatTimeRange(timeRange.first, timeRange.second)}\n")
            writer.append("Total Scans,${scans.size}\n")
            writer.append("\n")
            
            // Daily trends
            writer.append("=== DAILY TRENDS ===\n")
            writer.append("Date,Total Scans,Avg Risk Score,Critical,High,Medium,Low\n")
            
            val scansByDay = scans.groupBy {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
            }.toSortedMap()
            
            scansByDay.forEach { (date, dayScans) ->
                val avgRisk = dayScans.map { it.riskScore }.average()
                val critical = dayScans.count { it.riskLevel == "CRITICAL" }
                val high = dayScans.count { it.riskLevel == "HIGH" }
                val medium = dayScans.count { it.riskLevel == "MEDIUM" }
                val low = dayScans.count { it.riskLevel == "LOW" }
                
                writer.append("$date,${dayScans.size},${"%.1f".format(avgRisk)},$critical,$high,$medium,$low\n")
            }
            writer.append("\n")
            
            // Top techniques
            writer.append("=== TOP TECHNIQUES ===\n")
            writer.append("Technique ID,Technique Name,Count,Average Confidence\n")
            
            topTechniques.forEach { tech ->
                writer.append("${tech.techniqueId},${tech.techniqueName},${tech.count},${tech.maxConfidence}\n")
            }
        }
        
        ExportResult(
            filePath = file.absolutePath,
            fileSize = file.length(),
            format = ExportFormat.CSV,
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
