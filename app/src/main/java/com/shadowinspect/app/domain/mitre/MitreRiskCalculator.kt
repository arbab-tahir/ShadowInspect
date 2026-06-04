package com.shadowinspect.app.domain.mitre

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * MITRE Risk Calculator - Advanced threat scoring based on MITRE ATT&CK framework
 * Calculates risk scores using weighted factors: technique criticality, confidence, combinations
 */
@Singleton
class MitreRiskCalculator @Inject constructor(
    private val jsonParser: MitreJsonParser
) {
    
    private val tag = "MitreRiskCalculator"
    
    // Risk weights for different tactics (based on impact severity)
    private val tacticRiskWeights = mapOf(
        "Collection" to 0.8,
        "Command and Control" to 0.7,
        "Credential Access" to 0.9,
        "Defense Evasion" to 0.6,
        "Discovery" to 0.4,
        "Execution" to 0.7,
        "Exfiltration" to 0.9,
        "Impact" to 1.0,
        "Initial Access" to 0.8,
        "Persistence" to 0.7,
        "Privilege Escalation" to 0.8
    )
    
    // Technique severity levels (based on MITRE scoring)
    private val techniqueSeverity = mapOf(
        // Critical techniques (T1500-T1599)
        "T1529" to 1.0, // Capture SMS Messages
        "T1532" to 1.0, // Data Encrypted for Impact
        "T1428" to 0.9, // Capture Video
        "T1429" to 0.9, // Capture Audio
        "T1430" to 0.8, // Location Tracking
        "T1517" to 0.7, // Access Notifications
        "T1518" to 0.6, // Determine Device Type
        "T1525" to 0.9, // Implant Container Image
        "T1406" to 0.8, // Obfuscated Files
        "T1417" to 0.9, // Input Capture
        "T1530" to 0.7  // Data from Local System
    )
    
    /**
     * Calculate comprehensive risk score from detected techniques
     */
    fun calculateRiskScore(
        detectedTechniques: List<DetectedTechnique>,
        includeTacticalWeights: Boolean = true,
        includeCombinationBonus: Boolean = true
    ): ComprehensiveRiskScore {
        
        if (detectedTechniques.isEmpty()) {
            return ComprehensiveRiskScore(
                overallScore = 0,
                overallLevel = "SAFE",
                tacticScores = emptyMap(),
                techniqueScores = emptyMap(),
                riskFactors = emptyList(),
                recommendations = emptyList(),
                confidenceLevel = 1.0
            )
        }
        
        // Calculate individual technique scores
        val techniqueScores = detectedTechniques.associate { detection ->
            detection.technique.id to calculateTechniqueScore(detection)
        }
        
        // Calculate tactical scores (group by tactic)
        val tacticScores = mutableMapOf<String, Double>()
        detectedTechniques.forEach { detection ->
            detection.technique.tactics.forEach { tactic ->
                val currentScore = tacticScores[tactic] ?: 0.0
                val techniqueScore = techniqueScores[detection.technique.id] ?: 0.0
                tacticScores[tactic] = currentScore + techniqueScore
            }
        }
        
        // Apply tactical weights if requested
        val weightedTacticScores = if (includeTacticalWeights) {
            tacticScores.mapValues { (tactic, score) ->
                score * (tacticRiskWeights[tactic] ?: 0.5)
            }
        } else {
            tacticScores
        }
        
        // Calculate combination bonus for related techniques
        val combinationBonus = if (includeCombinationBonus) {
            calculateCombinationBonus(detectedTechniques)
        } else {
            0.0
        }
        
        // Calculate overall score (weighted average of tactic scores + bonus)
        val baseScore = if (weightedTacticScores.isNotEmpty()) {
            weightedTacticScores.values.average()
        } else {
            techniqueScores.values.average()
        }
        
        val overallScore = (baseScore * (1.0 + combinationBonus)).coerceIn(0.0, 100.0)
        
        // Identify risk factors
        val riskFactors = identifyRiskFactors(detectedTechniques)
        
        // Generate recommendations
        val recommendations = generateRiskRecommendations(detectedTechniques, riskFactors)
        
        // Calculate confidence level
        val confidenceLevel = calculateConfidenceLevel(detectedTechniques)
        
        return ComprehensiveRiskScore(
            overallScore = overallScore.toInt(),
            overallLevel = getRiskLevel(overallScore),
            tacticScores = weightedTacticScores.mapValues { it.value.toInt() },
            techniqueScores = techniqueScores.mapValues { it.value.toInt() },
            riskFactors = riskFactors,
            recommendations = recommendations,
            confidenceLevel = confidenceLevel
        )
    }
    
    /**
     * Calculate individual technique score based on:
     * - Base severity of technique
     * - Detection confidence
     * - Number of evidence items
     */
    private fun calculateTechniqueScore(detection: DetectedTechnique): Double {
        val baseSeverity = techniqueSeverity[detection.technique.id] ?: 0.5
        val confidenceWeight = detection.confidence / 100.0
        val evidenceBonus = (detection.evidence.size * 0.05).coerceIn(0.0, 0.2)
        
        return (baseSeverity * confidenceWeight * 100) * (1.0 + evidenceBonus)
    }
    
    /**
     * Calculate bonus for technique combinations that indicate sophisticated attacks
     */
    private fun calculateCombinationBonus(detectedTechniques: List<DetectedTechnique>): Double {
        var bonus = 0.0
        val techniqueIds = detectedTechniques.map { it.technique.id }.toSet()
        
        // Banking Trojan combination
        if (techniqueIds.containsAll(setOf("T1529", "T1417", "T1406"))) {
            bonus += 0.3 // SMS capture + Input capture + Obfuscation
        }
        
        // Spyware combination
        if (techniqueIds.containsAll(setOf("T1428", "T1429", "T1430"))) {
            bonus += 0.3 // Camera + Mic + Location
        }
        
        // Ransomware combination
        if (techniqueIds.containsAll(setOf("T1532", "T1525", "T1406"))) {
            bonus += 0.4 // Encryption + Persistence + Obfuscation
        }
        
        // Surveillance combination
        if (techniqueIds.containsAll(setOf("T1530", "T1517", "T1430"))) {
            bonus += 0.25 // Data theft + Notifications + Location
        }
        
        // Multi-tactic attack (techniques across 3+ tactics)
        val tactics = detectedTechniques.flatMap { it.technique.tactics }.toSet()
        if (tactics.size >= 3) {
            bonus += 0.2
        }
        
        return bonus.coerceIn(0.0, 0.8)
    }
    
    /**
     * Identify specific risk factors from detected techniques
     */
    private fun identifyRiskFactors(
        detectedTechniques: List<DetectedTechnique>
    ): List<RiskFactor> {
        val riskFactors = mutableListOf<RiskFactor>()
        
        // Check for critical techniques
        detectedTechniques.forEach { detection ->
            val severity = techniqueSeverity[detection.technique.id] ?: 0.5
            if (severity >= 0.9) {
                riskFactors.add(
                    RiskFactor(
                        factor = "Critical Technique: ${detection.technique.name}",
                        severity = "CRITICAL",
                        description = "This technique is commonly used in sophisticated malware",
                        mitreId = detection.technique.id
                    )
                )
            }
        }
        
        // Check for data theft risk
        if (detectedTechniques.any { it.technique.id in listOf("T1530", "T1529", "T1430") }) {
            riskFactors.add(
                RiskFactor(
                    factor = "Data Theft Risk",
                    severity = "HIGH",
                    description = "App can steal personal data (SMS, files, location)",
                    mitreId = null
                )
            )
        }
        
        // Check for surveillance risk
        if (detectedTechniques.any { it.technique.id in listOf("T1428", "T1429") }) {
            riskFactors.add(
                RiskFactor(
                    factor = "Surveillance Risk",
                    severity = "HIGH",
                    description = "App can record audio/video without your knowledge",
                    mitreId = null
                )
            )
        }
        
        // Check for persistence risk
        if (detectedTechniques.any { it.technique.id == "T1525" }) {
            riskFactors.add(
                RiskFactor(
                    factor = "Persistence Risk",
                    severity = "HIGH",
                    description = "App can maintain presence after reboot",
                    mitreId = "T1525"
                )
            )
        }
        
        // Check for financial fraud risk
        if (detectedTechniques.any { it.technique.id == "T1529" }) {
            riskFactors.add(
                RiskFactor(
                    factor = "Financial Fraud Risk",
                    severity = "CRITICAL",
                    description = "App can intercept SMS containing OTP/2FA codes",
                    mitreId = "T1529"
                )
            )
        }
        
        return riskFactors
    }
    
    /**
     * Generate recommendations based on risk factors
     */
    private fun generateRiskRecommendations(
        detectedTechniques: List<DetectedTechnique>,
        riskFactors: List<RiskFactor>
    ): List<String> {
        val recommendations = mutableSetOf<String>()
        
        riskFactors.forEach { factor ->
            when (factor.factor) {
                "Data Theft Risk" -> {
                    recommendations.addAll(
                        listOf(
                            "⚠️ Revoke storage and SMS permissions immediately",
                            "🔒 Monitor your accounts for unauthorized access",
                            "📱 Consider factory reset if you've entered sensitive data"
                        )
                    )
                }
                "Surveillance Risk" -> {
                    recommendations.addAll(
                        listOf(
                            "📸 Cover camera when not in use",
                            "🎤 Check for unusual background noise during calls",
                            "🔍 Review app permissions in Settings"
                        )
                    )
                }
                "Financial Fraud Risk" -> {
                    recommendations.addAll(
                        listOf(
                            "🏦 Contact your bank immediately",
                            "🚫 Do NOT use banking apps on this device",
                            "🔑 Change passwords from a different device"
                        )
                    )
                }
                "Persistence Risk" -> {
                    recommendations.addAll(
                        listOf(
                            "🔄 Boot device in safe mode to remove",
                            "🔧 Check for device administrator apps",
                            "📋 Review recently installed apps"
                        )
                    )
                }
            }
        }
        
        // Add MITRE-specific recommendations
        detectedTechniques.forEach { detection ->
            detection.technique.mitigation?.let { mitigation ->
                recommendations.add("• ${detection.technique.id}: $mitigation")
            }
        }
        
        // Add general recommendations if none specific
        if (recommendations.isEmpty()) {
            recommendations.addAll(
                listOf(
                    "✅ Review app permissions regularly",
                    "📚 Learn more about app security in Education section",
                    "🔄 Keep your device and apps updated"
                )
            )
        }
        
        return recommendations.toList()
    }
    
    /**
     * Calculate confidence level in the risk assessment
     */
    private fun calculateConfidenceLevel(detectedTechniques: List<DetectedTechnique>): Double {
        if (detectedTechniques.isEmpty()) return 1.0
        
        val avgConfidence = if (detectedTechniques.isNotEmpty()) {
            detectedTechniques.map { it.confidence }.average()
        } else {
            0.0
        }
        
        val techniqueCountBonus = (detectedTechniques.size * 0.05).coerceIn(0.0, 0.2)
        
        return ((avgConfidence / 100.0) + techniqueCountBonus).coerceIn(0.0, 1.0)
    }
    
    /**
     * Get risk level from score
     */
    private fun getRiskLevel(score: Double): String {
        return when {
            score >= 70 -> "CRITICAL"
            score >= 50 -> "HIGH"
            score >= 30 -> "MEDIUM"
            score >= 10 -> "LOW"
            else -> "SAFE"
        }
    }
    
    /**
     * Compare current analysis with historical baseline
     */
    suspend fun compareWithBaseline(
        currentAnalysis: MitreAnalysisResult,
        historicalAnalyses: List<MitreAnalysisResult>
    ): BaselineComparison = withContext(Dispatchers.IO) {
        
        if (historicalAnalyses.isEmpty()) {
            return@withContext BaselineComparison(
                isAnomaly = false,
                deviationScore = 0,
                unusualTechniques = emptyList(),
                typicalRiskLevel = "UNKNOWN",
                explanation = "No historical data for comparison"
            )
        }
        
        // Calculate baseline averages
        val avgRiskScore = historicalAnalyses.map { it.riskScore }.average()
        val commonTechniques = historicalAnalyses
            .flatMap { it.detectedTechniques.map { dt -> dt.technique.id } }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > historicalAnalyses.size / 2 }
            .keys
        
        // Find unusual techniques
        val currentTechniqueIds = currentAnalysis.detectedTechniques.map { it.technique.id }.toSet()
        val unusualTechniques = currentTechniqueIds.filterNot { it in commonTechniques }
        
        // Calculate deviation
        val deviationScore = if (avgRiskScore > 0) {
            ((currentAnalysis.riskScore - avgRiskScore) / avgRiskScore * 100).toInt()
        } else {
            currentAnalysis.riskScore * 100
        }
        val isAnomaly = deviationScore > 50 || unusualTechniques.isNotEmpty()
        
        BaselineComparison(
            isAnomaly = isAnomaly,
            deviationScore = deviationScore,
            unusualTechniques = unusualTechniques,
            typicalRiskLevel = getRiskLevel(avgRiskScore),
            explanation = buildString {
                if (isAnomaly) {
                    appendLine("⚠️ This app shows unusual behavior compared to typical apps")
                    if (unusualTechniques.isNotEmpty()) {
                        appendLine("Unusual techniques detected: ${unusualTechniques.joinToString()}")
                    }
                    if (deviationScore > 50) {
                        appendLine("Risk score is ${deviationScore}% higher than average")
                    }
                } else {
                    appendLine("✅ Behavior pattern matches typical apps")
                }
            }
        )
    }
    
    /**
     * Predict potential future threats based on current techniques
     */
    fun predictThreatProgression(detectedTechniques: List<DetectedTechnique>): List<PredictedThreat> {
        val predictions = mutableListOf<PredictedThreat>()
        val techniqueIds = detectedTechniques.map { it.technique.id }.toSet()
        
        // If SMS capture detected, predict credential theft
        if (techniqueIds.contains("T1529")) {
            predictions.add(
                PredictedThreat(
                    threatType = "Credential Theft",
                    probability = 0.8,
                    timeFrame = "Immediate",
                    description = "App may attempt to steal banking credentials",
                    mitigation = "Avoid using banking apps on this device"
                )
            )
        }
        
        // If surveillance techniques detected, predict data exfiltration
        if (techniqueIds.any { it in listOf("T1428", "T1429", "T1430") }) {
            predictions.add(
                PredictedThreat(
                    threatType = "Data Exfiltration",
                    probability = 0.7,
                    timeFrame = "Soon",
                    description = "Collected data may be sent to remote servers",
                    mitigation = "Monitor network activity, consider firewall"
                )
            )
        }
        
        // If persistence detected, predict long-term compromise
        if (techniqueIds.contains("T1525")) {
            predictions.add(
                PredictedThreat(
                    threatType = "Persistent Compromise",
                    probability = 0.9,
                    timeFrame = "Already occurring",
                    description = "Malware may survive factory reset",
                    mitigation = "Seek professional malware removal"
                )
            )
        }
        
        return predictions
    }
}
