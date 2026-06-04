package com.shadowinspect.app.domain.model

data class UrlScanResult(
    val url: String,
    val scanId: String,
    val riskScore: Int,
    val riskLevel: String,
    val explanation: String,
    val aiSummary: String? = null,
    val stats: UrlAnalysisStats,
    // Advanced fields from urlscan.io
    val screenshotUrl: String? = null,
    val pageTitle: String? = null,
    val server: String? = null,
    val ipAddress: String? = null,
    val country: String? = null,
    val asnName: String? = null,
    val detectedTech: List<String> = emptyList(),
    val urlscanScore: Int? = null,
    val engineResults: Map<String, String> = emptyMap(), // Kept for backward compatibility or simple use cases
    val fullEngineResults: Map<String, EngineDetail> = emptyMap(),
    val communityStats: String? = null
)

data class EngineDetail(
    val category: String, // "malicious", "suspicious", "harmless", "undetected", "timeout"
    val result: String? = null
)

data class UrlAnalysisStats(
    val malicious: Int,
    val suspicious: Int,
    val undetected: Int,
    val harmless: Int,
    val timeout: Int = 0
)


