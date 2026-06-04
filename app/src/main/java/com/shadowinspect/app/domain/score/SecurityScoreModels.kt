package com.shadowinspect.app.domain.score

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Complete Security Score with detailed breakdown
 */
data class SecurityScore(
    val overall: Int,                    // 0-100
    val categories: ScoreCategories,
    val factors: List<ScoreFactor>,
    val trend: ScoreTrend,
    val recommendations: List<ScoreRecommendation>,
    val history: List<HistoricalScore>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Score Categories Breakdown
 */
data class ScoreCategories(
    val appSecurity: CategoryScore,       // APK scans
    val urlSafety: CategoryScore,         // URL scans
    val phoneSecurity: CategoryScore,     // Phone scans
    val systemHealth: CategoryScore,      // Device configuration
    val privacyScore: CategoryScore,      // Privacy exposure
    val networkSecurity: CategoryScore    // Network threats
)

/**
 * Individual Category Score
 */
data class CategoryScore(
    val name: String,
    val score: Int,
    val weight: Double,
    val factors: List<String>,
    val threatsFound: Int,
    val lastScanTime: Long?
)

/**
 * Contributing Factor to Score
 */
data class ScoreFactor(
    val name: String,
    val impact: Int,                      // -50 to +50
    val category: String,
    val severity: String,                  // "POSITIVE", "NEGATIVE", "CRITICAL"
    val description: String,
    val mitigation: String?
)

/**
 * Score Trend Analysis
 */
data class ScoreTrend(
    val direction: TrendDirection,         // UP, DOWN, STABLE
    val changePercentage: Float,
    val periodDays: Int,
    val significantEvents: List<String>
)

enum class TrendDirection {
    UP, DOWN, STABLE
}

/**
 * Historical Score Point
 */
@Entity(tableName = "score_history")
data class HistoricalScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val overallScore: Int,
    val appScore: Int,
    val urlScore: Int,
    val phoneScore: Int,
    val systemScore: Int,
    val timestamp: Long,
    val note: String? = null
)

/**
 * Actionable Recommendation
 */
data class ScoreRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val priority: Int,                    // 1-5 (5 = highest)
    val category: String,
    val impact: String,                    // "IMMEDIATE", "SHORT_TERM", "LONG_TERM"
    val effort: String,                     // "LOW", "MEDIUM", "HIGH"
    val actionUrl: String? = null
)

/**
 * Score Comparison with Average Users
 */
data class ScoreComparison(
    val yourScore: Int,
    val averageScore: Int,
    val percentile: Int,                   // 0-100 (higher = better than others)
    val comparison: String
)

/**
 * Weekly Score Summary for Notifications
 */
data class WeeklyScoreSummary(
    val weekStart: Long,
    val weekEnd: Long,
    val averageScore: Int,
    val bestScore: Int,
    val worstScore: Int,
    val improvements: Int,
    val newThreats: Int,
    val topRecommendation: String
)
