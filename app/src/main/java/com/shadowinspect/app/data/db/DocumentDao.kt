package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.model.DocumentAnalysisResult
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: DocumentAnalysisResult): Long

    @Query("SELECT * FROM document_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<DocumentAnalysisResult>>

    @Query("SELECT * FROM document_scans WHERE id = :id")
    suspend fun getScanById(id: Long): DocumentAnalysisResult?

    @Query("SELECT * FROM document_scans WHERE hashSha256 = :sha256")
    suspend fun getScanByHash(sha256: String): DocumentAnalysisResult?

    @Delete
    suspend fun deleteScan(scan: DocumentAnalysisResult)

    @Query("DELETE FROM document_scans")
    suspend fun deleteAllScans()
}
