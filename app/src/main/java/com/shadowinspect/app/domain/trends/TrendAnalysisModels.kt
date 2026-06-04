package com.shadowinspect.app.domain.trends

import java.util.Date

/**
 * Complete Trend Analysis Report
 */
data class TrendAnalysis(
    val period: AnalysisPeriod,
    val patterns: List<ThreatPattern>,
    val predictions: List<ThreatPrediction>,
    val anomalies: List<AnomalyDetection>,
    val statistics: TrendStatistics,
    val recommendations: List<TrendRecommendation>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Analysis Period
 */
data class AnalysisPeriod(
    val startDate: Long,
    val endDate: Long,
    val daysAnalyzed: Int,
    val totalScans: Int
)

/**
 * Detected Threat Pattern
 */
data class ThreatPattern(
    val id: String,
    val patternType: PatternType,
    val description: String,
    val frequency: Int,
    val confidence: Float,           // 0-1
    val firstDetected: Long,
    val lastDetected: Long,
    val relatedTechniques: List<String>,
    val severity: String
)

enum class PatternType {
    REPEATING,                      // Same threat keeps appearing
    ESCALATING,                      // Threats getting worse
    EMERGING,                        // New threat type
    CORRELATED,                      // Threats that appear together
    SEASONAL,                         // Threats that appear at certain times
    CYCLICAL                          // Threats that follow a cycle
}

/**
 * Future Threat Prediction
 */
data class ThreatPrediction(
    val id: String,
    val threatType: String,
    val probability: Float,          // 0-1
    val timeframe: Timeframe,
    val expectedSeverity: String,
    val contributingFactors: List<String>,
    val recommendedActions: List<String>,
    val confidenceInterval: ConfidenceInterval
)

enum class Timeframe {
    NEXT_24_HOURS,
    NEXT_WEEK,
    NEXT_MONTH,
    NEXT_3_MONTHS
}

data class ConfidenceInterval(
    val lowerBound: Float,
    val upperBound: Float,
    val confidence: Float            // 0-1
)

/**
 * Anomaly Detection
 */
data class AnomalyDetection(
    val id: String,
    val anomalyType: String,
    val description: String,
    val expectedValue: Float,
    val actualValue: Float,
    val deviation: Float,            // Standard deviations
    val timestamp: Long,
    val severity: String,
    val investigation: String?
)

/**
 * Trend Statistics
 */
data class TrendStatistics(
    val averageRiskScore: Float,
    val medianRiskScore: Float,
    val standardDeviation: Float,
    val riskVelocity: Float,          // Rate of change (points/day)
    val threatDiversity: Int,         // Unique threat types
    val peakThreatDay: String,
    val lowThreatDay: String,
    val dayOfWeekPatterns: Map<String, Int>,
    val hourOfDayPatterns: Map<Int, Int>
)

/**
 * Trend-Based Recommendation
 */
data class TrendRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val priority: Int,
    val basedOn: String,              // What pattern/anomaly triggered this
    val timeframe: String,
    val actionable: Boolean,
    val estimatedImpact: String
)

/**
 * Weekly Threat Forecast
 */
data class WeeklyForecast(
    val weekStarting: Long,
    val predictedThreats: List<PredictedThreatForDay>,
    val overallRiskLevel: String,
    var accuracy: Float? = null       // Filled later when actual data comes
)

data class PredictedThreatForDay(
    val day: String,
    val predictedThreats: Int,
    val predictedRisk: Int,
    var actualThreats: Int? = null,
    var actualRisk: Int? = null
)

/**
 * ML Model Performance Tracking
 */
data class ModelPerformance(
    val modelName: String,
    val predictionsMade: Int,
    val accuracy: Float,
    val lastTraining: Long,
    val falsePositiveRate: Float,
    val falseNegativeRate: Float,
    val confusionMatrix: ConfusionMatrix
)

data class ConfusionMatrix(
    val truePositive: Int,
    val trueNegative: Int,
    val falsePositive: Int,
    val falseNegative: Int
) {
    val precision: Float = if (truePositive + falsePositive > 0) truePositive.toFloat() / (truePositive + falsePositive) else 0f
    val recall: Float = if (truePositive + falseNegative > 0) truePositive.toFloat() / (truePositive + falseNegative) else 0f
    val f1Score: Float = if (precision + recall > 0) 2 * (precision * recall) / (precision + recall) else 0f
}
