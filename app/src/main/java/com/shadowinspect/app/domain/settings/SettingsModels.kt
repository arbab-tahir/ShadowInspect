package com.shadowinspect.app.domain.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Legal document types
 */
enum class LegalDocumentType {
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
    GDPR_COMPLIANCE,
    DISCLAIMER,
    LICENSES,
    COPYRIGHT,
    ATTRIBUTIONS
}

/**
 * Legal document
 */
data class LegalDocument(
    val type: LegalDocumentType,
    val title: String,
    val content: String,
    val lastUpdated: String,
    val version: String
)

/**
 * Open source library
 */
data class OpenSourceLibrary(
    val name: String,
    val description: String,
    val license: String,
    val licenseUrl: String,
    val copyright: String,
    val usedFor: String
)

/**
 * App permission explanation
 */
data class AppPermission(
    val name: String,
    val purpose: String,
    val isRequired: Boolean,
    val isDangerous: Boolean,
    val dataCollected: String,
    val userControl: String
)

/**
 * Diagnostic info (for support)
 */
data class DiagnosticInfo(
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String,
    val screenSize: String,
    val totalScans: Int,
    val databaseSize: Long,
    val mlModelsLoaded: List<String>,
    val lastCrashTime: Long? = null,
    val batteryOptimization: Boolean
)
