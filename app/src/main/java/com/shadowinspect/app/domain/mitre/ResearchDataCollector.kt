package com.shadowinspect.app.domain.mitre

import com.shadowinspect.app.data.db.ResearchDataDao
import com.shadowinspect.app.domain.research.ResearchDataPoint
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class ResearchDataCollector @Inject constructor(
    private val researchDataDao: ResearchDataDao
) {

    /**
     * Anonymously tracks a detection for research purposes.
     */
    suspend fun trackDetection(
        techniqueId: String,
        tactic: String,
        confidenceScore: Int,
        riskLevel: String,
        appCategory: String? = null
    ) {
        val anonymousDetection = ResearchDataPoint(
            scanType = "APK",
            riskScore = confidenceScore,
            riskLevel = riskLevel,
            techniqueCount = 1,
            detectedTechniques = listOf(techniqueId),
            timestamp = System.currentTimeMillis(),
            androidVersion = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL,
            appVersion = "3.0.0"
        )
        researchDataDao.insertDataPoint(anonymousDetection)
    }

    /**
     * Generates a dataset for research paper export.
     */
    suspend fun generateResearchDataset(): ResearchDataset {
        val detections = researchDataDao.getAllDataPoints()
        return ResearchDataset(
            generationId = UUID.randomUUID().toString(),
            generatedAt = System.currentTimeMillis(),
            totalRecords = detections.size,
            detections = detections
        )
    }

    /**
     * Wipes research data for privacy.
     */
    suspend fun clearResearchData() {
        researchDataDao.deleteAllData()
    }

    /**
     * Returns total count of tracked research items.
     */
    suspend fun getResearchCount(): Int {
        return researchDataDao.getTotalScans()
    }
}
