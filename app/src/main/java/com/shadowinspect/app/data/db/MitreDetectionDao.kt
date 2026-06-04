package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.dashboard.TacticCount
import com.shadowinspect.app.domain.dashboard.TechniqueCount
import com.shadowinspect.app.domain.mitre.MitreDetection
import kotlinx.coroutines.flow.Flow

@Dao
interface MitreDetectionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetection(detection: MitreDetection): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDetections(detections: List<MitreDetection>)
    
    @Query("SELECT * FROM mitre_detections WHERE scanId = :scanId")
    suspend fun getDetectionsForScan(scanId: Long): List<MitreDetection>
    
    @Query("SELECT * FROM mitre_detections ORDER BY detectedAt DESC")
    suspend fun getAllDetections(): List<MitreDetection>

    @Query("SELECT * FROM mitre_detections ORDER BY detectedAt DESC LIMIT :limit")
    fun getRecentDetections(limit: Int): Flow<List<MitreDetection>>
    
    @Query("SELECT * FROM mitre_detections WHERE techniqueId = :techniqueId")
    suspend fun getDetectionsByTechnique(techniqueId: String): List<MitreDetection>
    
    @Query("SELECT COUNT(*) FROM mitre_detections WHERE scanId = :scanId")
    suspend fun getDetectionCountForScan(scanId: Long): Int
    
    @Query("DELETE FROM mitre_detections WHERE scanId = :scanId")
    suspend fun deleteDetectionsForScan(scanId: Long)
    
    @Query("SELECT * FROM mitre_detections WHERE confidenceScore >= :minConfidence")
    fun getHighConfidenceDetections(minConfidence: Int = 70): Flow<List<MitreDetection>>
    
    // Statistics queries for dashboard
    @Query("""
        SELECT techniqueId, techniqueName, COUNT(*) as count, 
               CAST(COALESCE(MAX(confidenceScore), 0) AS INTEGER) as maxConfidence
        FROM mitre_detections
        WHERE detectedAt > :since
        GROUP BY techniqueId
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getTopTechniques(limit: Int = 10, since: Long = 0): List<TechniqueCount>
    
    @Query("""
        SELECT tactic, COUNT(*) as count, CAST(COALESCE(AVG(confidenceScore), 0.0) AS REAL) as avgConfidence
        FROM mitre_detections
        WHERE detectedAt > :since
        GROUP BY tactic
        ORDER BY count DESC
    """)
    suspend fun getTacticDistribution(since: Long = 0): List<TacticCount>
    
    @Query("SELECT COUNT(*) FROM mitre_detections WHERE detectedAt > :since")
    suspend fun getTotalDetections(since: Long = 0): Int
    
    @Query("SELECT COUNT(DISTINCT techniqueId) FROM mitre_detections WHERE detectedAt > :since")
    suspend fun getUniqueTechniqueCount(since: Long = 0): Int
    
    @Query("SELECT CAST(COALESCE(AVG(confidenceScore), 0.0) AS REAL) FROM mitre_detections WHERE detectedAt > :since")
    suspend fun getAverageConfidence(since: Long = 0): Double
    
    // Time-based queries for trends
    @Query("""
        SELECT 
            strftime('%Y-%m-%d', datetime(detectedAt/1000, 'unixepoch')) as date,
            COUNT(*) as count,
            AVG(confidenceScore) as avgConfidence
        FROM mitre_detections
        WHERE detectedAt BETWEEN :start AND :end
        GROUP BY date
        ORDER BY date
    """)
    suspend fun getDailyDetections(start: Long, end: Long): List<DailyDetectionStats>
    
    @Query("SELECT * FROM mitre_detections WHERE techniqueId IN (:techniqueIds)")
    suspend fun getDetectionsByTechniqueIds(techniqueIds: List<String>): List<MitreDetection>

    @Query("SELECT * FROM mitre_detections WHERE techniqueId = :techniqueId ORDER BY detectedAt DESC")
    suspend fun getDetectionsByTechniqueId(techniqueId: String): List<MitreDetection>

    @Query("""
        SELECT techniqueId, techniqueName, COUNT(*) as count, 
               CAST(COALESCE(MAX(confidenceScore), 0) AS INTEGER) as maxConfidence
        FROM mitre_detections
        WHERE detectedAt >= :since
        GROUP BY techniqueId
    """)
    suspend fun getTechniquesSince(since: Long): List<TechniqueCount>

    @Query("SELECT * FROM mitre_detections WHERE detectedAt BETWEEN :start AND :end ORDER BY detectedAt DESC")
    suspend fun getDetectionsInDateRange(start: Long, end: Long): List<MitreDetection>

    @Query("DELETE FROM mitre_detections")
    suspend fun deleteAll()
}

data class DailyDetectionStats(
    val date: String,
    val count: Int,
    val avgConfidence: Float
)
