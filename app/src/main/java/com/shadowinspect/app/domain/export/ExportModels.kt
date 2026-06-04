package com.shadowinspect.app.domain.export

import java.util.Date

/**
 * Export format options
 */
enum class ExportFormat {
    CSV,
    JSON,
    PDF,
    EXCEL,
    MARKDOWN
}

/**
 * Export scope options
 */
enum class ExportScope {
    ALL_TIME,
    LAST_YEAR,
    LAST_6_MONTHS,
    LAST_3_MONTHS,
    LAST_MONTH,
    LAST_WEEK,
    CUSTOM_RANGE
}

/**
 * Export configuration
 */
data class ExportConfig(
    val format: ExportFormat,
    val scope: ExportScope,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val includeCharts: Boolean = true,
    val includeRawData: Boolean = false,
    val anonymizeData: Boolean = true,
    val includeRecommendations: Boolean = true,
    val includePredictions: Boolean = true
)

/**
 * Export result
 */
data class ExportResult(
    val filePath: String,
    val fileSize: Long,
    val format: ExportFormat,
    val recordCount: Int,
    val generatedAt: Long,
    val timeRange: String,
    val checksum: String
)

/**
 * Analytics summary for export
 */
data class AnalyticsSummary(
    val totalScans: Int,
    val uniqueTechniques: Int,
    val averageRiskScore: Float,
    val highestRiskScore: Int,
    val lowestRiskScore: Int,
    val mostCommonTechnique: String,
    val mostActiveDay: String,
    val riskTrend: String,
    val topRecommendations: List<String>
)

/**
 * Time series data point
 */
data class TimeSeriesPoint(
    val timestamp: Long,
    val date: String,
    val riskScore: Int,
    val scanCount: Int,
    val techniqueCount: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int
)

/**
 * Technique frequency data
 */
data class TechniqueFrequency(
    val techniqueId: String,
    val techniqueName: String,
    val count: Int,
    val averageConfidence: Float,
    val firstSeen: Long,
    val lastSeen: Long,
    val trend: String // "increasing", "decreasing", "stable"
)

/**
 * Export metadata for listing
 */
data class ExportMetadata(
    val id: String,
    val fileName: String,
    val format: ExportFormat,
    val size: Long,
    val generatedAt: Long,
    val recordCount: Int,
    val thumbnailPath: String? = null
)

/**
 * Basic stats regarding actual scan severity.
 */
data class ScanExportStats(
    val total: Long,
    val highRisk: Long,
    val safe: Long,
    val avgRiskScore: Float
)
