package com.shadowinspect.app.data.scan

data class ScanSummary(
    val id: Long,
    val type: String,
    val target: String,
    val riskScore: Int,
    val riskLevel: String,
    val timestamp: Long
)

data class DashboardStats(
    val totalScans: Long,
    val highRiskCount: Long,
    val averageScore: Float
)

data class DayScanCount(
    val dayIndex: Int,
    val count: Long
)

data class RiskLevelCount(
    val riskLevel: String,
    val count: Long
)

