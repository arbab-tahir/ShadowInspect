package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.score.HistoricalScore
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM score_history ORDER BY timestamp DESC")
    fun getAllScoreHistory(): Flow<List<HistoricalScore>>

    @Query("SELECT * FROM score_history WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getScoreHistorySince(since: Long): List<HistoricalScore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: HistoricalScore)

    @Query("DELETE FROM score_history WHERE timestamp < :before")
    suspend fun deleteOldScores(before: Long)

    @Query("SELECT overallScore FROM score_history")
    suspend fun getAllOverallScores(): List<Int>
}
