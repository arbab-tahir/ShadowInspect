package com.shadowinspect.app.domain.mitre

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MITRE Analyzer - Maps Android permissions and behaviors to MITRE ATT&CK techniques
 */
@Singleton
class MitreAnalyzer @Inject constructor(
    private val jsonParser: MitreJsonParser,
    private val searchEngine: MitreSearchEngine,
    private val explanationGenerator: MitreExplanationGenerator,
    private val riskCalculator: MitreRiskCalculator
) {
    
    private val tag = "MitreAnalyzer"
    
    /**
     * Analyze APK permissions and return matching MITRE techniques
     */
    suspend fun analyzePermissions(
        permissions: List<String>,
        packageName: String? = null,
        additionalBehaviors: List<String> = emptyList()
    ): MitreAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val allTechniques = jsonParser.loadMitreData()
            val detectedTechniques = mutableSetOf<DetectedTechnique>()
            
            // 1. Map permissions to techniques
            permissions.forEach { permission ->
                val matchingTechniques = findTechniquesForPermission(permission, allTechniques)
                matchingTechniques.forEach { technique ->
                    val confidence = calculatePermissionConfidence(permission, technique)
                    if (confidence > 20) { // Only include if confidence > 20%
                        detectedTechniques.add(
                            DetectedTechnique(
                                technique = technique,
                                confidence = confidence,
                                evidence = listOf("Permission: $permission"),
                                source = "Permission Mapping"
                            )
                        )
                    }
                }
            }
            
            // 2. Check for permission combinations (multiple permissions that together indicate a threat)
            val combinationTechniques = findTechniqueCombinations(permissions, allTechniques)
            detectedTechniques.addAll(combinationTechniques)
            
            // 3. Check package name against known malware families
            packageName?.let {
                val malwareTechniques = checkKnownMalwareFamilies()
                detectedTechniques.addAll(malwareTechniques)
            }
            
            // 4. Check for additional behaviors
            if (additionalBehaviors.isNotEmpty()) {
                val behaviorTechniques = mapBehaviorsToTechniques(additionalBehaviors, allTechniques)
                detectedTechniques.addAll(behaviorTechniques)
            }
            
            // Calculate comprehensive risk score using the new calculator
            val comprehensiveRisk = riskCalculator.calculateRiskScore(
                detectedTechniques = detectedTechniques.toList(),
                includeTacticalWeights = true,
                includeCombinationBonus = true
            )
            
            // Predict future threats
            val predictedThreats = riskCalculator.predictThreatProgression(
                detectedTechniques.toList()
            )
            
            // Generate summary
            val summary = generateAnalysisSummary(detectedTechniques.toList())
            
             MitreAnalysisResult(
                detectedTechniques = detectedTechniques.toList().sortedByDescending { it.confidence },
                totalTechniques = detectedTechniques.size,
                riskScore = comprehensiveRisk.overallScore,
                riskLevel = comprehensiveRisk.overallLevel,
                summary = summary,
                recommendations = comprehensiveRisk.recommendations,
                analyzedAt = System.currentTimeMillis(),
                comprehensiveRisk = comprehensiveRisk,
                predictedThreats = predictedThreats
            )
            
        } catch (e: Exception) {
            Log.e(tag, "Analysis failed", e)
            MitreAnalysisResult.error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Find techniques that match a specific permission
     */
    private fun findTechniquesForPermission(
        permission: String,
        allTechniques: List<MitreTechnique>
    ): List<MitreTechnique> {
        val matchingTechniques = mutableListOf<MitreTechnique>()
        
        // Direct permission match
        allTechniques.forEach { technique ->
            if (technique.permissionsRequired.any { 
                it.equals(permission, ignoreCase = true) ||
                permission.contains(it, ignoreCase = true) 
            }) {
                matchingTechniques.add(technique)
            }
        }
        
        // Check permission mapping database
        val mappedTechniques = MitrePermissionMapping.getTechniquesForPermission(permission)
        mappedTechniques.forEach { mapping ->
            allTechniques.find { it.id == mapping.techniqueId }?.let { technique ->
                matchingTechniques.add(technique)
            }
        }
        
        return matchingTechniques.distinct()
    }
    
    /**
     * Calculate confidence score for a permission-technique match
     */
    private fun calculatePermissionConfidence(
        permission: String,
        technique: MitreTechnique
    ): Int {
        // Base confidence
        var confidence = 50
        
        // Check if permission is in technique's required permissions
        if (technique.permissionsRequired.any { it.equals(permission, ignoreCase = true) }) {
            confidence += 30
        }
        
        // Check mapping weight
        val mapping = MitrePermissionMapping.mappings.find {
            it.permission == permission && it.techniqueId == technique.id
        }
        confidence += (mapping?.weight?.times(20) ?: 0).toInt()
        
        // Boost confidence for dangerous permissions
        if (isDangerousPermission(permission)) {
            confidence += 10
        }
        
        return confidence.coerceIn(0, 100)
    }
    
    /**
     * Find techniques based on permission combinations
     */
    private fun findTechniqueCombinations(
        permissions: List<String>,
        allTechniques: List<MitreTechnique>
    ): List<DetectedTechnique> {
        val detected = mutableListOf<DetectedTechnique>()
        
        // Banking Trojan combination: SMS + Accessibility + Overlay
        if (permissions.contains("android.permission.READ_SMS") &&
            permissions.contains("android.permission.RECEIVE_SMS") &&
            permissions.any { it.contains("ACCESSIBILITY") }) {
            
            allTechniques.find { it.id == "T1529" }?.let { technique -> // Capture SMS
                detected.add(
                    DetectedTechnique(
                        technique = technique,
                        confidence = 85,
                        evidence = listOf(
                            "SMS permissions (READ_SMS, RECEIVE_SMS)",
                            "Accessibility service permission"
                        ),
                        source = "Combination Analysis"
                    )
                )
            }
        }
        
        // Spyware combination: Camera + Microphone + Location
        val spywarePermissions = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION"
        )
        val hasSpyware = spywarePermissions.count { permissions.contains(it) } >= 2
        
        if (hasSpyware) {
            // Add Collection techniques
            allTechniques.find { it.id == "T1428" }?.let { technique -> // Capture Video
                detected.add(
                    DetectedTechnique(
                        technique = technique,
                        confidence = 75,
                        evidence = listOf("Camera permission present"),
                        source = "Combination Analysis"
                    )
                )
            }
            
            allTechniques.find { it.id == "T1429" }?.let { technique -> // Capture Audio
                detected.add(
                    DetectedTechnique(
                        technique = technique,
                        confidence = 75,
                        evidence = listOf("Microphone permission present"),
                        source = "Combination Analysis"
                    )
                )
            }
            
            allTechniques.find { it.id == "T1430" }?.let { technique -> // Location Tracking
                detected.add(
                    DetectedTechnique(
                        technique = technique,
                        confidence = 75,
                        evidence = listOf("Location permission present"),
                        source = "Combination Analysis"
                    )
                )
            }
        }
        
        // Ransomware combination: Storage + Install packages
        if (permissions.contains("android.permission.WRITE_EXTERNAL_STORAGE") &&
            permissions.contains("android.permission.READ_EXTERNAL_STORAGE") &&
            permissions.any { it.contains("INSTALL_PACKAGES") }) {
            
            allTechniques.find { it.id == "T1532" }?.let { technique -> // Data Encrypted for Impact
                detected.add(
                    DetectedTechnique(
                        technique = technique,
                        confidence = 80,
                        evidence = listOf(
                            "Storage permissions (READ/WRITE)",
                            "Install packages permission"
                        ),
                        source = "Combination Analysis"
                    )
                )
            }
        }
        
        return detected
    }
    
    /**
     * Check if permission is considered dangerous
     */
    private fun isDangerousPermission(permission: String): Boolean {
        val dangerousPermissions = listOf(
            "READ_SMS", "RECEIVE_SMS", "SEND_SMS",
            "CAMERA", "RECORD_AUDIO",
            "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
            "READ_CONTACTS", "READ_CALL_LOG",
            "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE",
            "BIND_ACCESSIBILITY_SERVICE", "SYSTEM_ALERT_WINDOW"
        )
        
        return dangerousPermissions.any { permission.contains(it, ignoreCase = true) }
    }
    
    /**
     * Check package name against known malware families
     */
    private suspend fun checkKnownMalwareFamilies(): List<DetectedTechnique> {
        val detected = mutableListOf<DetectedTechnique>()
        
        // This would normally query a database
        // For now, return empty list
        return detected
    }
    
    /**
     * Map behavioral indicators to techniques
     */
    private fun mapBehaviorsToTechniques(
        behaviors: List<String>,
        allTechniques: List<MitreTechnique>
    ): List<DetectedTechnique> {
        val detected = mutableListOf<DetectedTechnique>()
        
        behaviors.forEach { behavior ->
            when (behavior.lowercase()) {
                "background_location" -> {
                    allTechniques.find { it.id == "T1430" }?.let { technique ->
                        detected.add(
                            DetectedTechnique(
                                technique = technique,
                                confidence = 70,
                                evidence = listOf("Background location tracking"),
                                source = "Behavior Analysis"
                            )
                        )
                    }
                }
                "accessibility_service" -> {
                    allTechniques.find { it.id == "T1406" }?.let { technique ->
                        detected.add(
                            DetectedTechnique(
                                technique = technique,
                                confidence = 90,
                                evidence = listOf("Accessibility service enabled"),
                                source = "Behavior Analysis"
                            )
                        )
                    }
                }
                "overlay_detected" -> {
                    allTechniques.find { it.id == "T1417" }?.let { technique ->
                        detected.add(
                            DetectedTechnique(
                                technique = technique,
                                confidence = 80,
                                evidence = listOf("Screen overlay detected"),
                                source = "Behavior Analysis"
                            )
                        )
                    }
                }
            }
        }
        
        return detected
    }
    
    /**
     * Generate analysis summary
     */
    private fun generateAnalysisSummary(detectedTechniques: List<DetectedTechnique>): String {
        if (detectedTechniques.isEmpty()) {
            return "✅ [ANALYSIS PASS] No adversarial techniques identified within the scope of current heuristic signatures."
        }
        
        val tactics = detectedTechniques.map { it.technique.tactics }.flatten().distinct()
        
        return buildString {
            appendLine("### MITRE ATT&CK® INTELLIGENCE REPORT")
            appendLine("The analysis identified **${detectedTechniques.size}** distinct techniques mapped across **${tactics.size}** high-level tactics.")
            appendLine()
            appendLine("**TOP TACTICAL OBSERVATIONS:**")
            
            tactics.sortedByDescending { t -> detectedTechniques.count { it.technique.tactics.contains(t) } }.take(4).forEach { tactic ->
                val count = detectedTechniques.count { it.technique.tactics.contains(tactic) }
                appendLine("- **$tactic**: $count technique(s) detected with high structural correlation.")
            }
            
            appendLine()
            appendLine("*Note: This mapping is based on static permission-to-behavior heuristics.*")
        }
    }
    
    /**
     * Analyze permissions and return results for saving to database.
     * Persistence is typically handled by the Repository.
     */
    suspend fun analyzeAndSave(
        scanId: Long,
        permissions: List<String>,
        packageName: String? = null,
        additionalBehaviors: List<String> = emptyList()
    ): MitreAnalysisResult {
        val result = analyzePermissions(permissions, packageName, additionalBehaviors)
        
        // Note: The actual database insertion is handled in the Repository
        // to keep the domain layer clean of DAO dependencies.
        return result
    }

    /**
     * Get human-readable explanation for a technique
     */
    suspend fun getTechniqueExplanation(techniqueId: String): String? {
        val technique = jsonParser.getTechniqueById(techniqueId) ?: return null
        
        return buildString {
            appendLine("**${technique.id}: ${technique.name}**")
            appendLine()
            appendLine(technique.description)
            appendLine()
            appendLine("**Tactics:** ${technique.tactics.joinToString()}")
            if (technique.permissionsRequired.isNotEmpty()) {
                appendLine("**Required Permissions:** ${technique.permissionsRequired.joinToString()}")
            }
            technique.detection?.let {
                appendLine()
                appendLine("**Detection:** $it")
            }
        }
    }
}
