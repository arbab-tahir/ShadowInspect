package com.shadowinspect.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shadowinspect.app.domain.model.ImageAnalysisResult

@Dao
interface ImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ImageAnalysisResult): Long

    @Query("SELECT * FROM image_scans ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentScans(limit: Int = 20): List<ImageAnalysisResult>

    @Query("SELECT * FROM image_scans WHERE id = :id")
    suspend fun getScanById(id: Long): ImageAnalysisResult?

    @Query("SELECT COUNT(*) FROM image_scans")
    suspend fun getTotalScans(): Int

    @Query("DELETE FROM image_scans")
    suspend fun deleteAll()
}
