package com.shadowinspect.app.data.scan

import com.google.gson.Gson
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.ScanEntity
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.domain.mitre.MitreAnalysisResult
import com.shadowinspect.app.domain.mitre.MitreDetection
import com.shadowinspect.app.domain.model.ApkAnalysisResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import com.shadowinspect.app.data.scan.ScanSummary
import com.shadowinspect.app.data.scan.DashboardStats


@Singleton
class ScanRepository @Inject constructor(
    private val dao: ScanDao,
    private val mitreDetectionDao: MitreDetectionDao
) {

    private val gson = Gson()

    suspend fun saveScan(entity: ScanEntity): Long = dao.insert(entity)

    /**
     * Save an APK analysis result into the scans table and return the generated id.
     */
    suspend fun saveApkScan(result: ApkAnalysisResult): Long {
        val entity = result.toScanEntity()
        val scanId = dao.insert(entity)

        // Save MITRE detections if present
        result.mitreAnalysis?.let { mitreResult ->
            saveMitreDetections(scanId, mitreResult)
        }

        return scanId
    }

    /**
     * Save MITRE detections associated with a scan.
     */
    suspend fun saveMitreDetections(
        scanId: Long,
        mitreResult: MitreAnalysisResult
    ) {
        val detections = mitreResult.detectedTechniques.map { detection ->
            MitreDetection(
                scanId = scanId,
                scanType = "APK",
                techniqueId = detection.technique.id,
                techniqueName = detection.technique.name,
                tactic = detection.technique.tactics.firstOrNull() ?: "Unknown",
                confidenceScore = detection.confidence,
                evidence = detection.evidence,
                detectedAt = mitreResult.analyzedAt
            )
        }
        if (detections.isNotEmpty()) {
            mitreDetectionDao.insertAllDetections(detections)
        }
    }

    fun getRecentScans(): Flow<List<ScanSummary>> =
        dao.getRecentScansFlow().map { list ->
            list.map { e -> e.toScanSummary() }
        }

    fun getApkScans(): Flow<List<ScanSummary>> =
        dao.getScansByTypeFlow("APK").map { list -> list.map { it.toScanSummary() } }

    fun getDashboardStats(): Flow<DashboardStats> =
        combine(
            dao.getTotalScansFlow(),
            dao.getHighRiskCountFlow(),
            dao.getAverageScoreFlow()
        ) { total, highRisk, avg ->
            DashboardStats(
                totalScans = total,
                highRiskCount = highRisk,
                averageScore = avg.toFloat()
            )
        }

    suspend fun getApkDetails(id: Long): ApkAnalysisResult? {
        val entity = dao.getScanById(id) ?: return null
        return try {
            gson.fromJson(entity.detailsJson, ApkAnalysisResult::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // --- History features migrated from ScanHistoryRepository ---
    val totalScansCount: Flow<Long> = dao.getTotalScansFlow()
    val highRiskScansCount: Flow<Long> = dao.getHighRiskCountFlow()
    val avgRiskScore: Flow<Double> = dao.getAverageScoreFlow()

    suspend fun getRecentScansList(limit: Int) = dao.getRecentScans(limit)
    suspend fun getScanById(id: Long) = dao.getScanById(id)
    suspend fun deleteOldHistory(before: Long) = dao.deleteOldScans(before)
    suspend fun deleteAllHistory() = dao.deleteAllScans()
    suspend fun getWeeklyBarData(): List<DayScanCount> = dao.getWeeklyScanCounts()
    suspend fun getRiskDistribution(): List<RiskLevelCount> = dao.getRiskLevelDistribution()
}


// --- Conversion helpers ---
private fun ApkAnalysisResult.toScanEntity(): ScanEntity {
    val json = Gson().toJson(this)
    return ScanEntity(
        scanType = "APK",
        target = this.fileName,
        riskScore = this.riskScore,
        riskLevel = this.riskLevel,
        timestamp = this.timestamp,
        detailsJson = json
    )
}

private fun ScanEntity.toScanSummary(): ScanSummary = ScanSummary(
    id = this.id,
    type = this.scanType,
    target = this.target,
    riskScore = this.riskScore,
    riskLevel = this.riskLevel,
    timestamp = this.timestamp
)

private fun ScanEntity.toApkAnalysisResult(): ApkAnalysisResult? {
    return try {
        Gson().fromJson(this.detailsJson, ApkAnalysisResult::class.java)
    } catch (e: Exception) {
        null
    }
}
