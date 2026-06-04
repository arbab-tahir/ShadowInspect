package com.shadowinspect.app.domain.mitre

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Complete MITRE Threat Report
 */
@Entity(tableName = "mitre_reports")
data class MitreReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val scanId: Long,                    // Reference to original scan
    val scanTarget: String,               // App name, URL, phone number
    val scanType: String,                  // "APK", "URL", "PHONE"
    
    val reportTitle: String,
    val reportSummary: String,
    
    val overallRiskScore: Int,
    val overallRiskLevel: String,
    
    val detectedTechniques: List<MitreTechnique>,
    val techniqueCount: Int,
    val tacticCount: Int,
    
    val riskFactors: List<RiskFactor>,
    val recommendations: List<String>,
    val predictedThreats: List<PredictedThreat>,
    
    val confidenceLevel: Double,
    
    val generatedAt: Long = System.currentTimeMillis(),
    val reportFormat: String = "PDF",
    val isExported: Boolean = false,
    val exportPath: String? = null
)

/**
 * Threat Intelligence Summary for sharing
 */
data class ThreatIntelSummary(
    val reportId: Long,
    val threatLevel: String,
    val primaryThreats: List<String>,
    val affectedComponents: List<String>,
    val remediationSteps: List<String>,
    val iocs: List<String> = emptyList() // Indicators of Compromise
)

/**
 * Report Template configurations
 */
enum class ReportTemplate {
    TECHNICAL,      // For security professionals
    EXECUTIVE,      // For management
    EDUCATIONAL,    // For end users
    FORENSIC,       // Detailed for legal purposes
    JSON_DATA       // Multi-format raw data exchange
}

/**
 * Report Export Format
 */
enum class ReportFormat {
    PDF,
    JSON,
    CSV,
    HTML
}
