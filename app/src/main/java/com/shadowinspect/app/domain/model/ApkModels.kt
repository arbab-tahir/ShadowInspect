package com.shadowinspect.app.domain.model

import com.google.gson.annotations.SerializedName
import com.shadowinspect.app.domain.mitre.MitreAnalysisResult
import com.shadowinspect.app.domain.ml.ApkFeatureVector
import com.shadowinspect.app.domain.ml.MLDetectionResult

/**
 * Basic metadata extracted from an APK file.
 */
data class ApkInfo(
    val fileName: String,
    val fileSize: Long,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Int? = null,
    val minSdkVersion: Int? = null,
    val targetSdkVersion: Int? = null,
    val isDebuggable: Boolean = false,
    val isAllowBackup: Boolean = false,
    val activitiesCount: Int = 0,
    val servicesCount: Int = 0,
    val receiversCount: Int = 0,
    val providersCount: Int = 0,
    val certificateInfo: String? = null,
    val requestedPermissions: List<String> = emptyList()
)

/**
 * Representation of a single Android permission declared or used by the APK.
 */
data class ApkPermission(
    @SerializedName("name") val name: String,
    @SerializedName("isDangerous") val isDangerous: Boolean = false,
    @SerializedName("description") val description: String? = null,
    @SerializedName("protectionLevel") val protectionLevel: String? = null
)

/**
 * Consolidated analysis result for an APK file.
 */
data class ApkAnalysisResult(
    val fileName: String,
    val fileSize: Long,
    val sha256: String,
    val md5: String? = null,
    val packageName: String? = null,
    val permissions: List<ApkPermission> = emptyList(),
    val dangerousPermissions: List<ApkPermission> = emptyList(),
    val riskScore: Int = 0,
    val riskLevel: String = "UNKNOWN",
    val threatCategories: List<String> = emptyList(),
    val mitreTechniques: List<String> = emptyList(),
    val isDebuggable: Boolean = false,
    val isAllowBackup: Boolean = false,
    val activitiesCount: Int = 0,
    val servicesCount: Int = 0,
    val receiversCount: Int = 0,
    val providersCount: Int = 0,
    val certificateInfo: String? = null,
    val explanation: String = "",
    val aiSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val mitreAnalysis: MitreAnalysisResult? = null,
    val mlDetection: MLDetectionResult? = null,
    val featureVector: ApkFeatureVector? = null
) {
    companion object {
        fun error(fileName: String, message: String): ApkAnalysisResult = ApkAnalysisResult(
            fileName = fileName,
            fileSize = 0L,
            sha256 = "",
            md5 = null,
            packageName = null,
            permissions = emptyList(),
            dangerousPermissions = emptyList(),
            riskScore = 0,
            riskLevel = "ERROR",
            threatCategories = emptyList(),
            mitreTechniques = emptyList(),
            isDebuggable = false,
            isAllowBackup = false,
            activitiesCount = 0,
            servicesCount = 0,
            receiversCount = 0,
            providersCount = 0,
            certificateInfo = null,
            explanation = "",
            aiSummary = null,
            timestamp = System.currentTimeMillis(),
            isError = true,
            errorMessage = message,
            mitreAnalysis = null,
            mlDetection = null,
            featureVector = null
        )
    }
}

/**
 * Categorizes permission danger levels used in analysis and UI mapping.
 */
enum class PermissionDangerousLevel {
    NORMAL,
    DANGEROUS,
    SIGNATURE,
    SYSTEM
}
