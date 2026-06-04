package com.shadowinspect.app.domain.mitre

import com.shadowinspect.app.domain.research.ResearchDataPoint

data class ResearchDataset(
    val generationId: String,
    val generatedAt: Long,
    val totalRecords: Int,
    val detections: List<ResearchDataPoint>
)
