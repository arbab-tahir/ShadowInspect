package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.mitre.MitreReport
import kotlinx.coroutines.flow.Flow

@Dao
interface MitreReportDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: MitreReport): Long
    
    @Query("SELECT * FROM mitre_reports WHERE scanId = :scanId")
    suspend fun getReportsForScan(scanId: Long): List<MitreReport>
    
    @Query("SELECT * FROM mitre_reports ORDER BY generatedAt DESC")
    fun getAllReports(): Flow<List<MitreReport>>
    
    @Query("SELECT * FROM mitre_reports WHERE generatedAt > :since ORDER BY generatedAt DESC")
    fun getRecentReports(since: Long): Flow<List<MitreReport>>
    
    @Query("SELECT * FROM mitre_reports WHERE overallRiskLevel = :riskLevel")
    suspend fun getReportsByRiskLevel(riskLevel: String): List<MitreReport>
    
    @Update
    suspend fun updateReport(report: MitreReport)
    
    @Delete
    suspend fun deleteReport(report: MitreReport)
    
    @Query("DELETE FROM mitre_reports WHERE generatedAt < :before")
    suspend fun deleteOldReports(before: Long)
    
    @Query("SELECT COUNT(*) FROM mitre_reports")
    suspend fun getTotalReportCount(): Int

    @Query("DELETE FROM mitre_reports")
    suspend fun deleteAllReports()
}
