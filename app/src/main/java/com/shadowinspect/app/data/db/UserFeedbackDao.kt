package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.research.UserFeedback
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFeedbackDao {
    
    @Insert
    suspend fun insertFeedback(feedback: UserFeedback)
    
    @Query("SELECT * FROM user_feedback WHERE scanId = :scanId")
    suspend fun getFeedbackForScan(scanId: Long): UserFeedback?
    
    @Query("SELECT COUNT(*) FROM user_feedback")
    suspend fun getTotalFeedbackCount(): Int
    
    @Query("SELECT COUNT(*) FROM user_feedback WHERE wasAccurate = 1")
    suspend fun getAccurateFeedbackCount(): Int
    
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN wasAccurate = 1 THEN 1 ELSE 0 END) as accurate
        FROM user_feedback f
        JOIN mitre_detections d ON f.scanId = d.scanId
        WHERE d.techniqueId = :techniqueId
    """)
    suspend fun getAccuracyForTechnique(techniqueId: String): TechniqueAccuracy?
    
    @Query("SELECT * FROM user_feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<UserFeedback>>
}

data class TechniqueAccuracy(
    val total: Int,
    val accurate: Int
)
