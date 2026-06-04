package com.shadowinspect.app.domain.mitre

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a MITRE ATT&CK Technique
 * Example: T1517 - Access Notifications
 */
data class MitreTechnique(
    val id: String,              // e.g., "T1517"
    val name: String,             // e.g., "Access Notifications"
    val description: String,      // Full description
    val tactics: List<String>,    // e.g., ["Collection"]
    val platforms: List<String>,  // e.g., ["Android", "iOS"]
    val permissionsRequired: List<String>, // Android permissions needed
    val url: String,              // MITRE reference URL
    val detection: String?,       // How to detect this technique
    val mitigation: String?       // How to mitigate
)

/**
 * Represents a MITRE ATT&CK Tactic
 * Example: TA0037 - Collection
 */
data class MitreTactic(
    val id: String,              // e.g., "TA0037"
    val name: String,             // e.g., "Collection"
    val description: String,
    val techniques: List<String>  // List of technique IDs
)

/**
 * Represents a detected threat with MITRE mapping
 */
@Entity(tableName = "mitre_detections")
data class MitreDetection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val scanId: Long,              // Reference to original scan
    val scanType: String,           // "APK", "URL", "PHONE"
    
    val techniqueId: String,        // MITRE technique ID
    val techniqueName: String,
    val tactic: String,
    
    val confidenceScore: Int,       // 0-100 how confident this technique was used
    val evidence: List<String>,     // What evidence triggered this detection
    
    val detectedAt: Long = System.currentTimeMillis()
)

/**
 * MITRE Threat Report - Complete analysis
 */
data class MitreThreatReport(
    val scanId: Long,
    val scanTarget: String,         // App name, URL, phone number
    val scanType: String,
    
    val techniques: List<MitreTechnique>,
    val tactics: List<String>,
    
    val riskScore: Int,
    val riskLevel: String,
    
    val summary: String,
    val recommendations: List<String>,
    
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Permission to MITRE mapping
 */
data class PermissionMapping(
    val permission: String,          // Android permission name
    val techniqueId: String,         // MITRE technique ID
    val description: String,         // Why this permission maps to this technique
    val weight: Double               // 0.0-1.0 importance for this technique
)

/**
 * MITRE Matrix for Mobile
 * Complete list of mobile tactics
 */
object MitreMobileMatrix {
    val tactics = listOf(
        MitreTactic("TA0037", "Collection", "Gathering data from device", emptyList()),
        MitreTactic("TA0038", "Command and Control", "Communicating with attacker", emptyList()),
        MitreTactic("TA0039", "Credential Access", "Stealing credentials", emptyList()),
        MitreTactic("TA0040", "Defense Evasion", "Avoiding detection", emptyList()),
        MitreTactic("TA0041", "Discovery", "Learning device information", emptyList()),
        MitreTactic("TA0042", "Execution", "Running malicious code", emptyList()),
        MitreTactic("TA0043", "Exfiltration", "Stealing data from device", emptyList()),
        MitreTactic("TA0044", "Impact", "Manipulating/interrupting device", emptyList()),
        MitreTactic("TA0045", "Initial Access", "Getting into the device", emptyList()),
        MitreTactic("TA0046", "Persistence", "Staying on the device", emptyList()),
        MitreTactic("TA0047", "Privilege Escalation", "Getting more permissions", emptyList())
    )
}

/**
 * Data class for detected technique with confidence
 */
data class DetectedTechnique(
    val technique: MitreTechnique,
    val confidence: Int, // 0-100
    val evidence: List<String>,
    val source: String
)

/**
 * Result of MITRE analysis
 */
data class MitreAnalysisResult(
    val detectedTechniques: List<DetectedTechnique>,
    val totalTechniques: Int,
    val riskScore: Int,
    val riskLevel: String,
    val summary: String,
    val recommendations: List<String>,
    val analyzedAt: Long,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val comprehensiveRisk: ComprehensiveRiskScore? = null,
    val predictedThreats: List<PredictedThreat> = emptyList()
) {
    companion object {
        fun empty(): MitreAnalysisResult {
            return MitreAnalysisResult(
                detectedTechniques = emptyList(),
                totalTechniques = 0,
                riskScore = 0,
                riskLevel = "NONE",
                summary = "No MITRE techniques detected",
                recommendations = emptyList(),
                analyzedAt = System.currentTimeMillis()
            )
        }

        fun error(message: String): MitreAnalysisResult {
            return MitreAnalysisResult(
                detectedTechniques = emptyList(),
                totalTechniques = 0,
                riskScore = 0,
                riskLevel = "ERROR",
                summary = "Analysis failed: $message",
                recommendations = emptyList(),
                analyzedAt = System.currentTimeMillis(),
                isError = true,
                errorMessage = message
            )
        }
    }
}

/**
 * Comprehensive risk score with detailed breakdown
 */
data class ComprehensiveRiskScore(
    val overallScore: Int,
    val overallLevel: String,
    val tacticScores: Map<String, Int>,
    val techniqueScores: Map<String, Int>,
    val riskFactors: List<RiskFactor>,
    val recommendations: List<String>,
    val confidenceLevel: Double
)

/**
 * Individual risk factor
 */
data class RiskFactor(
    val factor: String,
    val severity: String, // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val description: String,
    val mitreId: String?
)

/**
 * Baseline comparison result
 */
data class BaselineComparison(
    val isAnomaly: Boolean,
    val deviationScore: Int,
    val unusualTechniques: List<String>,
    val typicalRiskLevel: String,
    val explanation: String
)

/**
 * Predicted future threat
 */
data class PredictedThreat(
    val threatType: String,
    val probability: Double, // 0.0-1.0
    val timeFrame: String, // "Immediate", "Soon", "Long-term"
    val description: String,
    val mitigation: String
)

