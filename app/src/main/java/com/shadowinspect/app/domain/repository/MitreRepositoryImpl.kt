package com.shadowinspect.app.domain.repository

import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.ScanEntity
import com.shadowinspect.app.domain.mitre.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MitreRepositoryImpl @Inject constructor(
    private val jsonParser: MitreJsonParser,
    private val explanationGenerator: MitreExplanationGenerator,
    private val scanDao: ScanDao,
    private val searchEngine: MitreSearchEngine,
    private val mitreDetectionDao: com.shadowinspect.app.data.db.MitreDetectionDao,
    private val mitreReportDao: com.shadowinspect.app.data.db.MitreReportDao
) : MitreRepository {
    
    override suspend fun analyzeApkForMitre(
        scanId: Long,
        permissions: List<String>,
        packageName: String
    ): List<MitreDetection> = withContext(Dispatchers.IO) {
        val detections = mutableListOf<MitreDetection>()
        
        // Check each permission against MITRE techniques
        permissions.forEach { permission ->
            val matchingTechniques = jsonParser.loadMitreData().filter { technique ->
                technique.permissionsRequired.any { 
                    it.equals(permission, ignoreCase = true) 
                }
            }
            
            matchingTechniques.forEach { technique ->
                val confidence = calculateConfidence(permission, technique)
                if (confidence > 30) { // Only report if confidence > 30%
                    detections.add(
                        MitreDetection(
                            scanId = scanId,
                            scanType = "APK",
                            techniqueId = technique.id,
                            techniqueName = technique.name,
                            tactic = technique.tactics.firstOrNull() ?: "Unknown",
                            confidenceScore = confidence,
                            evidence = listOf("Permission: $permission"),
                            detectedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        
        // Also check package name for known malware families
        checkKnownMalwareFamilies()?.let { technique ->
            detections.add(
                MitreDetection(
                    scanId = scanId,
                    scanType = "APK",
                    techniqueId = technique.id,
                    techniqueName = technique.name,
                    tactic = technique.tactics.firstOrNull() ?: "Unknown",
                    confidenceScore = 90,
                    evidence = listOf("Known malware family pattern"),
                    detectedAt = System.currentTimeMillis()
                )
            )
        }
        
        return@withContext detections.distinctBy { it.techniqueId }
    }
    
    override suspend fun analyzeUrlForMitre(
        scanId: Long,
        url: String,
        threatIndicators: List<String>
    ): List<MitreDetection> = withContext(Dispatchers.IO) {
        val techniques = jsonParser.loadMitreData()
        val detections = mutableListOf<MitreDetection>()
        
        // URL-specific MITRE techniques
        // T1534 - Internal Spearphishing
        // T1566 - Phishing
        threatIndicators.forEach { indicator ->
            when {
                indicator.contains("phishing", ignoreCase = true) -> {
                    jsonParser.getTechniqueById("T1566")?.let { technique ->
                        detections.add(
                            MitreDetection(
                                scanId = scanId,
                                scanType = "URL",
                                techniqueId = technique.id,
                                techniqueName = technique.name,
                                tactic = "Initial Access",
                                confidenceScore = 70,
                                evidence = listOf("Phishing indicators detected"),
                                detectedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
        
        return@withContext detections
    }
    
    override suspend fun analyzePhoneForMitre(
        scanId: Long,
        phoneNumber: String,
        spamIndicators: List<String>
    ): List<MitreDetection> = withContext(Dispatchers.IO) {
        val detections = mutableListOf<MitreDetection>()
        
        // Phone-specific MITRE techniques
        // T1512 - Pretexting (vishing)
        if (spamIndicators.isNotEmpty()) {
            detections.add(
                MitreDetection(
                    scanId = scanId,
                    scanType = "PHONE",
                    techniqueId = "T1512",
                    techniqueName = "Pretexting",
                    tactic = "Initial Access",
                    confidenceScore = 60,
                    evidence = spamIndicators,
                    detectedAt = System.currentTimeMillis()
                )
            )
        }
        
        return@withContext detections
    }
    
    override suspend fun getMitreDetections(scanId: Long): List<MitreDetection> {
        return mitreDetectionDao.getDetectionsForScan(scanId)
    }
    
    override suspend fun saveMitreDetections(detections: List<MitreDetection>) {
        mitreDetectionDao.insertAllDetections(detections)
    }

    override suspend fun saveMitreReport(report: MitreReport): Long {
        return mitreReportDao.insertReport(report)
    }

    override fun getRecentMitreThreats(): Flow<List<MitreDetection>> {
        return mitreDetectionDao.getRecentDetections(10)
    }
    
    override suspend fun generateThreatReport(scanId: Long): MitreThreatReport = withContext(Dispatchers.IO) {
        val scan = scanDao.getScanById(scanId) ?: throw IllegalArgumentException("Scan not found")
        val detections = getMitreDetections(scanId)
        val techniques = detections.mapNotNull { jsonParser.getTechniqueById(it.techniqueId) }
        
        val riskScore = calculateRiskScore(detections)
        
        MitreThreatReport(
            scanId = scanId,
            scanTarget = when (scan.scanType) {
                "URL" -> scan.target
                "APK" -> scan.target
                "PHONE" -> scan.phoneNumber ?: "Unknown"
                else -> "Unknown"
            },
            scanType = scan.scanType,
            techniques = techniques,
            tactics = techniques.flatMap { it.tactics }.distinct(),
            riskScore = riskScore,
            riskLevel = getRiskLevel(riskScore),
            summary = explanationGenerator.generateUserFriendlyExplanation(techniques),
            recommendations = generateRecommendations(techniques),
            generatedAt = System.currentTimeMillis()
        )
    }
    
    override suspend fun getTechniqueDetails(techniqueId: String): MitreTechnique? {
        return jsonParser.getTechniqueById(techniqueId)
    }
    
    override fun getAllTechniques(): Flow<List<MitreTechnique>> {
        return kotlinx.coroutines.flow.flow { emit(jsonParser.loadMitreData()) }
    }
    
    override fun searchTechniques(query: String): Flow<List<MitreTechnique>> {
        return kotlinx.coroutines.flow.flow { emit(searchEngine.searchTechniques(query)) }
    }
    
    private fun calculateConfidence(permission: String, technique: MitreTechnique): Int {
        val mapping = MitrePermissionMapping.mappings.find {
            it.permission == permission && it.techniqueId == technique.id
        }
        return (mapping?.weight ?: 0.5).times(100).toInt()
    }
    
    private fun checkKnownMalwareFamilies(): MitreTechnique? {
        // This would connect to a database of known malware
        // For now, return null
        return null
    }
    
    private fun calculateRiskScore(detections: List<MitreDetection>): Int {
        if (detections.isEmpty()) return 0
        
        val baseScore = detections.sumOf { it.confidenceScore } / detections.size
        val multiplier = when (detections.size) {
            1 -> 1.0
            2 -> 1.3
            3 -> 1.5
            else -> 1.8
        }
        
        return (baseScore * multiplier).toInt().coerceIn(0, 100)
    }
    
    private fun getRiskLevel(score: Int): String {
        return when {
            score >= 70 -> "CRITICAL"
            score >= 50 -> "HIGH"
            score >= 30 -> "MEDIUM"
            score >= 10 -> "LOW"
            else -> "SAFE"
        }
    }
    
    private fun generateRecommendations(techniques: List<MitreTechnique>): List<String> {
        val recommendations = mutableListOf<String>()
        
        techniques.forEach { technique ->
            technique.mitigation?.let { 
                recommendations.add("• ${technique.id}: $it")
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("• No specific mitigations found")
            recommendations.add("• Keep your device updated")
            recommendations.add("• Only install apps from trusted sources")
        }
        
        return recommendations.distinct()
    }
}
