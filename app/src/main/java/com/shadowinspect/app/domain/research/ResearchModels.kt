package com.shadowinspect.app.domain.research

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Anonymous research data point - NO PII collected!
 */
@Entity(tableName = "research_data")
data class ResearchDataPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val scanId: Long? = null,           // Reference to the actual scan ID
    val scanType: String,               // "APK", "URL", "PHONE"
    val appCategory: String = "Unknown",// e.g. "Finance", "Social", "Utility", "Unknown"
    val riskScore: Int,
    val riskLevel: String,
    val techniqueCount: Int,
    val detectedTechniques: List<String>, // Technique IDs only, no user data
    val detectedTactics: List<String> = emptyList(), // MITRE tactic names (Collection, Exfiltration…)

    // Permission data (anonymised counts + names, no user data)
    val dangerousPermissionCount: Int = 0,
    val dangerousPermissions: List<String> = emptyList(), // e.g. ["READ_SMS","ACCESS_FINE_LOCATION"]

    // Security indicators
    val isDebuggable: Boolean = false,
    val virusTotalVerdict: String = "Not Checked", // "Clean", "Malicious", "Suspicious", "Not Checked"

    val detectionAccuracy: Boolean? = null,   // If we can verify later
    val falsePositive: Boolean? = null,       // User feedback

    val timestamp: Long,

    // Anonymous device info (no identifiers)
    val androidVersion: Int,
    val deviceModel: String,               // Generic like "Pixel" not specific
    val appVersion: String
)

/**
 * Research statistics aggregated for paper
 */
data class ResearchStatistics(
    val totalScans: Int,
    val totalTechniques: Int,
    val uniqueTechniques: Int,
    val averageRiskScore: Float,
    val detectionRate: Float,
    val falsePositiveRate: Float,
    
    val topTechniques: List<TechniqueStat>,
    val riskDistribution: Map<String, Int>,
    val monthlyTrends: List<MonthlyStat>
)

data class TechniqueStat(
    val techniqueId: String,
    val techniqueName: String,
    val detectionCount: Int,
    var accuracy: Float? = null
)

data class MonthlyStat(
    val month: String,                  // "2024-03"
    val scanCount: Int,
    val averageRisk: Float,
    val newTechniques: Int
)

/**
 * User feedback on detection accuracy
 */
@Entity(tableName = "user_feedback")
data class UserFeedback(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val scanId: Long,
    val wasAccurate: Boolean,           // User said "Yes this was correct"
    val comments: String? = null,        // Optional feedback
    val timestamp: Long
)
