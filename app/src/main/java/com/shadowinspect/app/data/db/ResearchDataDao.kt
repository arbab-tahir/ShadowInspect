package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.research.ResearchDataPoint

@Dao
interface ResearchDataDao {
    
    @Insert
    suspend fun insertDataPoint(point: ResearchDataPoint)
    
    @Query("SELECT * FROM research_data")
    suspend fun getAllDataPoints(): List<ResearchDataPoint>
    
    @Query("SELECT COUNT(*) FROM research_data")
    suspend fun getTotalScans(): Int
    
    @Query("SELECT SUM(techniqueCount) FROM research_data")
    suspend fun getTotalTechniqueDetections(): Int
    
    @Query("SELECT COUNT(DISTINCT detectedTechniques) FROM research_data")
    suspend fun getUniqueTechniqueCount(): Int
    
    @Query("SELECT AVG(riskScore) FROM research_data")
    suspend fun getAverageRiskScore(): Float
    
    // Note: We'll handle the Map conversion in the repository or use a wrapper class
    @Query("""
        SELECT riskLevel, COUNT(*) as count 
        FROM research_data 
        GROUP BY riskLevel
    """)
    suspend fun getRiskDistributionRaw(): List<RiskDistributionRaw>
    
    @Query("""
        UPDATE research_data 
        SET detectionAccuracy = :wasAccurate 
        WHERE scanId = :scanId
    """)
    suspend fun updateAccuracyForScan(scanId: Long, wasAccurate: Boolean)
    
    @Query("""
        SELECT 
            strftime('%Y-%m', datetime(timestamp/1000, 'unixepoch')) as month,
            COUNT(*) as scanCount,
            AVG(riskScore) as avgRisk
        FROM research_data
        WHERE timestamp > :since
        GROUP BY month
        ORDER BY month
    """)
    suspend fun getMonthlyStats(since: Long): List<MonthlyStatRaw>
    
    @Query("""
        SELECT * FROM research_data 
        WHERE strftime('%Y-%m', datetime(timestamp/1000, 'unixepoch')) = :month
    """)
    suspend fun getDataPointsForMonth(month: String): List<ResearchDataPoint>

    @Query("DELETE FROM research_data")
    suspend fun deleteAllData()
}

data class RiskDistributionRaw(
    val riskLevel: String,
    val count: Int
)

data class MonthlyStatRaw(
    val month: String,
    val scanCount: Int,
    val avgRisk: Float
)
