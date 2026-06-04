package com.shadowinspect.app.domain.repository

import com.shadowinspect.app.domain.mitre.MitreDetection
import com.shadowinspect.app.domain.mitre.MitreTechnique
import com.shadowinspect.app.domain.mitre.MitreThreatReport
import com.shadowinspect.app.domain.mitre.MitreReport
import kotlinx.coroutines.flow.Flow

interface MitreRepository {
    suspend fun analyzeApkForMitre(
        scanId: Long,
        permissions: List<String>,
        packageName: String
    ): List<MitreDetection>

    suspend fun analyzeUrlForMitre(
        scanId: Long,
        url: String,
        threatIndicators: List<String>
    ): List<MitreDetection>

    suspend fun analyzePhoneForMitre(
        scanId: Long,
        phoneNumber: String,
        spamIndicators: List<String>
    ): List<MitreDetection>

    suspend fun getMitreDetections(scanId: Long): List<MitreDetection>
    
    suspend fun saveMitreDetections(detections: List<MitreDetection>)

    suspend fun saveMitreReport(report: MitreReport): Long
    
    fun getRecentMitreThreats(): Flow<List<MitreDetection>>
    
    suspend fun generateThreatReport(scanId: Long): MitreThreatReport
    
    suspend fun getTechniqueDetails(techniqueId: String): MitreTechnique?
    
    fun getAllTechniques(): Flow<List<MitreTechnique>>
    
    fun searchTechniques(query: String): Flow<List<MitreTechnique>>
}
