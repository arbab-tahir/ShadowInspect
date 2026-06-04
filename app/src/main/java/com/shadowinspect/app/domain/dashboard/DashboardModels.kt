package com.shadowinspect.app.domain.dashboard

import com.shadowinspect.app.data.db.ScanEntity
import java.util.Date

/**
 * Main Dashboard State
 */
data class DashboardState(
    val securityScore: Int,                    // Overall device security score (0-100)
    val totalScans: Int,                        // Total scans performed
    val highRiskCount: Int,                      // Number of high risk findings
    val criticalCount: Int,                      // Critical threats
    val mitigatedCount: Int,                     // Issues resolved
    
    val riskDistribution: RiskDistribution,      // Risk levels breakdown
    val techniqueDistribution: List<TechniqueCount>, // Top MITRE techniques
    val tacticDistribution: List<TacticCount>,   // Tactics breakdown
    val recentActivity: List<ActivityItem>,      // Recent scans
    val threatTrends: List<ThreatTrend>,         // Trends over time
    
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Risk Level Distribution
 */
data class RiskDistribution(
    val critical: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val safe: Int
) {
    val total: Int = critical + high + medium + low + safe
    
    fun criticalPercentage(): Int = (critical * 100 / maxOf(total, 1))
    fun highPercentage(): Int = (high * 100 / maxOf(total, 1))
    fun mediumPercentage(): Int = (medium * 100 / maxOf(total, 1))
    fun lowPercentage(): Int = (low * 100 / maxOf(total, 1))
    fun safePercentage(): Int = (safe * 100 / maxOf(total, 1))
}

/**
 * Recent Activity Item
 */
data class ActivityItem(
    val id: Long,
    val scanType: String,        // "APK", "URL", "PHONE"
    val target: String,           // App name, URL, phone number
    val riskScore: Int,
    val riskLevel: String,
    val timestamp: Long,
    val techniqueCount: Int,
    val iconRes: Int? = null
)

/**
 * Threat Trend over time
 */
data class ThreatTrend(
    val date: String,              // "2024-03-01"
    val critical: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val total: Int
)

/**
 * Security Tip of the Day
 */
data class SecurityTip(
    val id: Int,
    val title: String,
    val content: String,
    val category: String,          // "Permissions", "Phishing", "Updates", etc.
    val icon: String,               // Emoji or icon name
    val priority: Int               // 1-5 (5 = highest)
)

/**
 * Device Security Score Breakdown
 */
data class SecurityScoreBreakdown(
    val appSecurity: Int,           // Score from app scans
    val urlSecurity: Int,           // Score from URL scans
    val phoneSecurity: Int,         // Score from phone scans
    val systemHealth: Int,          // Device configuration
    val overall: Int
)

/**
 * Time Range for Analytics
 */
enum class TimeRange {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL
}

/**
 * Scan Statistics data class
 */
data class ScanStats(
    val totalScans: Int,
    val criticalCount: Int,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val lowRiskCount: Int,
    val safeCount: Int,
    val mitigatedCount: Int
)

/**
 * MITRE Technique Count for Analytics
 */
data class TechniqueCount(
    val techniqueId: String,
    val techniqueName: String,
    val count: Int,
    val maxConfidence: Int = 0
)

/**
 * MITRE Tactic Count for Analytics
 */
data class TacticCount(
    val tactic: String,
    val count: Int,
    val avgConfidence: Double = 0.0
)
