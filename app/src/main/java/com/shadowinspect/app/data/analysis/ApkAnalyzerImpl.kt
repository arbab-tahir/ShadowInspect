package com.shadowinspect.app.data.analysis

import android.content.Context
import android.net.Uri
import com.shadowinspect.app.domain.analyzer.ApkAnalyzer
import com.shadowinspect.app.domain.model.ApkAnalysisResult
import com.shadowinspect.app.utils.ApkParser
import com.shadowinspect.app.domain.util.RiskScoreEngine
import com.shadowinspect.app.domain.util.GeminiSummaryEngine
import com.shadowinspect.app.domain.mitre.MitreAnalyzer
import com.shadowinspect.app.domain.mitre.MitreAnalysisResult
import com.shadowinspect.app.domain.model.ApkInfo
import com.shadowinspect.app.domain.model.ApkPermission
import com.shadowinspect.app.domain.ml.FeatureExtractor
import com.shadowinspect.app.domain.ml.MLModelManager
import com.shadowinspect.app.domain.ml.MLDetectionResult
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkAnalyzerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apkParser: ApkParser,
    private val riskEngine: RiskScoreEngine,
    private val geminiSummaryEngine: GeminiSummaryEngine,
    private val mitreAnalyzer: MitreAnalyzer,
    private val featureExtractor: FeatureExtractor,
    private val mlModelManager: MLModelManager
) : ApkAnalyzer {



    override fun probeApk(uri: Uri) = apkParser.parseApk(uri)

    override suspend fun analyzeApk(uri: Uri): ApkAnalysisResult {
        return try {
            val info = apkParser.parseApk(uri) ?: return ApkAnalysisResult.error(
                uri.lastPathSegment ?: "unknown.apk",
                "Failed to parse APK manifest"
            )

            // Create temporary file for ML analysis since APK content URI can't be read natively by zip libraries
            var tempApkFile: File? = null
            try {
                tempApkFile = File.createTempFile("ml_extract_", ".apk", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempApkFile).use { out ->
                        input.copyTo(out)
                    }
                }
            } catch (e: Exception) {
                // Ignore failure to create temp file, will fallback gracefully
            }

            val sha256 = apkParser.calculateSha256(uri) ?: ""
            val md5 = apkParser.calculateMd5(uri)

            val permissions = apkParser.getApkPermissions(info.requestedPermissions)
            val dangerous = permissions.filter { it.isDangerous }
            
            // --- ML Analysis ---
            var featureVector: com.shadowinspect.app.domain.ml.ApkFeatureVector? = null
            var mlResult: MLDetectionResult? = null
            
            if (tempApkFile != null && tempApkFile.exists()) {
                featureVector = featureExtractor.extractFeatures(tempApkFile, permissions)
                
                if (mlModelManager.modelState.value.isInitialized) {
                    mlResult = mlModelManager.predict(featureVector)
                }
            }
            
            // Placeholder for VirusTotal lookup (to be implemented if needed)
            val vtScore = 0f 

            val hasSuspiciousPatterns = (info.packageName?.contains("bank", true) == true) || 
                                      (info.packageName?.contains("trojan", true) == true) ||
                                      info.isDebuggable || info.isAllowBackup

            val rawRiskScore = riskEngine.calculateApkRisk(
                permissions = permissions.map { it.name },
                dangerousPermissionsCount = dangerous.size,
                totalPermissionsCount = permissions.size,
                hasSuspiciousPatterns = hasSuspiciousPatterns,
                threatIntelScore = vtScore
            )
            
            val threatCategories = riskEngine.categorizeThreats(permissions.map { it.name })

            // --- MITRE ATT&CK Analysis ---
            val mitreResult = if (permissions.isNotEmpty()) {
                mitreAnalyzer.analyzePermissions(
                    permissions = permissions.map { it.name },
                    packageName = info.packageName,
                    additionalBehaviors = detectBehaviors(permissions)
                )
            } else {
                null
            }

            // Combine existing risk score with ML score and MITRE score
            // If ML is Benign, it drives the score down. If Malicious, it drives the score up.
            val mlRisk = if (mlResult != null) {
                if (mlResult.isMalicious) (mlResult.confidence * 100).toInt()
                else (100 - (mlResult.confidence * 100)).toInt() // E.g., 80% Benign = 20% Risk
            } else null

            val mitreRisk = mitreResult?.riskScore

            val combinedRiskScore = when {
                mlRisk != null && mitreRisk != null -> {
                    // All 3 signals available: Base (30%), MITRE (30%), ML (40%)
                    (rawRiskScore * 0.3 + mitreRisk * 0.3 + mlRisk * 0.4).toInt()
                }
                mlRisk != null -> {
                    // ML + Base: ML (60%), Base (40%)
                    (rawRiskScore * 0.4 + mlRisk * 0.6).toInt()
                }
                mitreRisk != null -> {
                    // MITRE + Base: MITRE (40%), Base (60%)
                    (rawRiskScore * 0.6 + mitreRisk * 0.4).toInt()
                }
                else -> rawRiskScore
            }.coerceIn(0, 100)

            val finalRiskLevel = riskEngine.getRiskLevel(combinedRiskScore)

            val explanation = riskEngine.generateApkExplanation(combinedRiskScore, dangerous, permissions.map { it.name })
            val aiSummary = try {
                geminiSummaryEngine.generateApkSummary(
                    fileName = info.fileName,
                    packageName = info.packageName,
                    riskScore = combinedRiskScore,
                    permissions = permissions.size,
                    dangerous = dangerous.size,
                    threats = threatCategories.joinToString(", ")
                )
            } catch (e: Exception) {
                null
            }
            
            // Generate enhanced explanation with MITRE and ML findings
            val enhancedExplanation = generateEnhancedExplanation(
                baseExplanation = explanation,
                mitreResult = mitreResult,
                mlResult = mlResult
            )

            // Cleanup temp file
            try { tempApkFile?.delete() } catch (_: Exception) {}

            ApkAnalysisResult(
                fileName = info.fileName,
                fileSize = info.fileSize,
                sha256 = sha256,
                md5 = md5,
                packageName = info.packageName,
                permissions = permissions,
                dangerousPermissions = dangerous,
                riskScore = combinedRiskScore,
                riskLevel = finalRiskLevel,
                threatCategories = threatCategories,
                mitreTechniques = mitreResult?.detectedTechniques?.map { it.technique.id } ?: emptyList(),
                isDebuggable = info.isDebuggable,
                isAllowBackup = info.isAllowBackup,
                activitiesCount = info.activitiesCount,
                servicesCount = info.servicesCount,
                receiversCount = info.receiversCount,
                providersCount = info.providersCount,
                certificateInfo = info.certificateInfo,
                explanation = enhancedExplanation,
                aiSummary = aiSummary,
                timestamp = System.currentTimeMillis(),
                mitreAnalysis = mitreResult,
                mlDetection = mlResult,
                featureVector = featureVector
            )
        } catch (e: Exception) {
            ApkAnalysisResult.error(uri.lastPathSegment ?: "unknown.apk", e.message ?: "Analysis failed")
        }
    }

    /**
     * Helper to detect behavioral indicators based on APK metadata and permissions.
     */
    private fun detectBehaviors(
        permissions: List<ApkPermission>
    ): List<String> {
        val behaviors = mutableListOf<String>()

        // Check for background location
        if (permissions.any { it.name.contains("ACCESS_BACKGROUND_LOCATION") }) {
            behaviors.add("background_location")
        }

        // Check for accessibility service
        if (permissions.any { it.name.contains("BIND_ACCESSIBILITY_SERVICE") }) {
            behaviors.add("accessibility_service")
        }

        // Check for overlay
        if (permissions.any { it.name.contains("SYSTEM_ALERT_WINDOW") }) {
            behaviors.add("overlay_detected")
        }

        return behaviors
    }

    /**
     * Combines the base analysis explanation with MITRE ATT&CK findings.
     */
    private fun generateEnhancedExplanation(
        baseExplanation: String,
        mitreResult: MitreAnalysisResult?,
        mlResult: MLDetectionResult? = null
    ): String {
        return buildString {
            appendLine(baseExplanation)
            
            if (mlResult != null) {
                appendLine()
                appendLine("🧠 **ML Analysis** (${mlResult.modelType.name})")
                appendLine("• Classification: ${mlResult.threatClass}")
                appendLine("• Confidence: ${(mlResult.confidence * 100).toInt()}%")
                appendLine("• Inference time: ${mlResult.inferenceTimeMs}ms")
                
                if (mlResult.isMalicious && mlResult.threatFamily != null) {
                    appendLine("• Threat family: ${mlResult.threatFamily}")
                }
            }
            
            if (mitreResult != null && mitreResult.detectedTechniques.isNotEmpty()) {
                appendLine()
                appendLine("--- MITRE ATT&CK Analysis ---")
                appendLine(mitreResult.summary)
                appendLine()
                appendLine("Top techniques detected:")
                mitreResult.detectedTechniques.take(3).forEach { detection ->
                    appendLine("• ${detection.technique.id}: ${detection.technique.name} (${detection.confidence}% confidence)")
                }
            }
        }
    }
}

