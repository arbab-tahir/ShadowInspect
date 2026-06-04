package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.data.scan.DayScanCount
import com.shadowinspect.app.data.scan.RiskLevelCount
import kotlinx.coroutines.flow.Flow


/**
 * DAO for reading and writing scan history records.
 */
@Dao
interface ScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanEntity): Long

    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    suspend fun getAllScans(): List<ScanEntity>

    @Query("SELECT * FROM scans WHERE scanType = :type ORDER BY timestamp DESC")
    fun getScansByTypeFlow(type: String): kotlinx.coroutines.flow.Flow<List<ScanEntity>>

    /** Emits the latest X records, newest first. */
    @Query("SELECT * FROM scans ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentScans(limit: Int): List<ScanEntity>

    /** Fetches a single scan by its primary key. */
    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun getScanById(id: Long): ScanEntity?

    @Query("SELECT COUNT(*) FROM scans")
    suspend fun getTotalScans(): Long

    @Query("SELECT COUNT(*) FROM scans WHERE riskScore >= 50")
    suspend fun getHighRiskCount(): Long

    /** Average risk score across all scans (returns 0 when table is empty). */
    @Query("SELECT COALESCE(AVG(riskScore), 0.0) FROM scans")
    suspend fun getAverageRiskScore(): Float

    /** Fetches scans within a specific time range. */
    @Query("SELECT * FROM scans WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getScansByDateRange(start: Long, end: Long): List<ScanEntity>

    /** Deletes records older than a certain timestamp. */
    @Query("DELETE FROM scans WHERE timestamp < :before")
    suspend fun deleteOldScans(before: Long)

    /** Deletes all records from the scan history. */
    @Query("DELETE FROM scans")
    suspend fun deleteAllScans()

    // --- Flow-based queries for real-time dashboard updates ---

    @Query("SELECT * FROM scans ORDER BY timestamp DESC LIMIT 50")
    fun getRecentScansFlow(): Flow<List<ScanEntity>>

    @Query("SELECT COUNT(*) FROM scans")
    fun getTotalScansFlow(): Flow<Long>

    @Query("SELECT COUNT(*) FROM scans WHERE riskScore >= 50")
    fun getHighRiskCountFlow(): Flow<Long>

    @Query("SELECT COALESCE(AVG(riskScore), 0.0) FROM scans")
    fun getAverageScoreFlow(): Flow<Double>

    @Query("""
        SELECT 
            CAST(strftime('%w', datetime(timestamp / 1000, 'unixepoch')) AS INTEGER) AS dayIndex,
            COUNT(*) AS count
        FROM scans
        WHERE timestamp >= (strftime('%s', 'now') - 7 * 86400) * 1000
        GROUP BY dayIndex
        ORDER BY dayIndex ASC
    """)
    suspend fun getWeeklyScanCounts(): List<DayScanCount>


    @Query("""
        SELECT riskLevel, COUNT(*) AS count
        FROM scans
        GROUP BY riskLevel
    """)
    suspend fun getRiskLevelDistribution(): List<RiskLevelCount>

    // ── Phone-specific queries (Module 7) ─────────────────────────────────────

    /** All phone scans, newest first. */
    @Query("SELECT * FROM scans WHERE scanType = 'PHONE' ORDER BY timestamp DESC")
    fun getPhoneScans(): Flow<List<ScanEntity>>

    /** Most recent scan for a specific phone number (for caching / de-dup). */
    @Query("SELECT * FROM scans WHERE phoneNumber = :number ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPhoneScan(number: String): ScanEntity?

    /** Phone scans filtered by minimum spam score. */
    @Query("SELECT * FROM scans WHERE scanType = 'PHONE' AND phoneSpamScore >= :minScore ORDER BY timestamp DESC")
    fun getPhoneScansByRisk(minScore: Int): Flow<List<ScanEntity>>

    // --- Dashboard Analytics Queries (Module 9.1) ---

    @Query("""
        SELECT 
            CAST(COUNT(*) AS INTEGER) as totalScans,
            CAST(COALESCE(SUM(CASE WHEN riskLevel = 'CRITICAL' THEN 1 ELSE 0 END), 0) AS INTEGER) as criticalCount,
            CAST(COALESCE(SUM(CASE WHEN riskLevel = 'HIGH' THEN 1 ELSE 0 END), 0) AS INTEGER) as highRiskCount,
            CAST(COALESCE(SUM(CASE WHEN riskLevel = 'MEDIUM' THEN 1 ELSE 0 END), 0) AS INTEGER) as mediumRiskCount,
            CAST(COALESCE(SUM(CASE WHEN riskLevel = 'LOW' THEN 1 ELSE 0 END), 0) AS INTEGER) as lowRiskCount,
            CAST(COALESCE(SUM(CASE WHEN riskLevel = 'SAFE' THEN 1 ELSE 0 END), 0) AS INTEGER) as safeCount,
            CAST(0 AS INTEGER) as mitigatedCount
        FROM scans
        WHERE timestamp > :since
    """)
    suspend fun getScanStats(since: Long = 0): com.shadowinspect.app.domain.dashboard.ScanStats

    @Query("SELECT * FROM scans WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getScansInDateRange(start: Long, end: Long): List<ScanEntity>

    @Query("""
        SELECT 
            id,
            scanType,
            target,
            riskScore,
            riskLevel,
            timestamp,
            CAST((SELECT COUNT(*) FROM mitre_detections WHERE mitre_detections.scanId = scans.id) AS INTEGER) as techniqueCount,
            CAST(NULL AS INTEGER) as iconRes
        FROM scans 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getRecentActivity(limit: Int): Flow<List<com.shadowinspect.app.domain.dashboard.ActivityItem>>

    @Query("""
        SELECT 
            id,
            scanType,
            target,
            riskScore,
            riskLevel,
            timestamp,
            CAST((SELECT COUNT(*) FROM mitre_detections WHERE mitre_detections.scanId = scans.id) AS INTEGER) as techniqueCount,
            CAST(NULL AS INTEGER) as iconRes
        FROM scans 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentActivitySnapshot(limit: Int): List<com.shadowinspect.app.domain.dashboard.ActivityItem>

    @Query("SELECT CAST(COALESCE(AVG(riskScore), 0.0) AS REAL) FROM scans WHERE timestamp > :since")
    suspend fun getAverageRiskScoreSince(since: Long = 0): Double

    @Query("SELECT * FROM scans WHERE scanType = :type AND timestamp > :since ORDER BY timestamp DESC")
    suspend fun getScansByTypeSince(type: String, since: Long): List<ScanEntity>

    @Query("SELECT overallScore FROM score_history")
    suspend fun getAllUserScores(): List<Int>
    @Query("""
        SELECT * FROM scans 
        WHERE id IN (SELECT DISTINCT scanId FROM mitre_detections)
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getScansWithMitre(limit: Int): List<ScanEntity>
}
