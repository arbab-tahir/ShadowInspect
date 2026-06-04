package com.shadowinspect.app.presentation.home

data class HomeScanSummary(
    val title: String,
    val type: String,
    val status: String,
    val isHighRisk: Boolean
)

data class HomeSystemLog(
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)


enum class LogLevel {
    INFO, WARNING, CRITICAL
}

data class HomeState(
    val totalScans: Int = 0,
    val highRiskCount: Int = 0,
    val averageScore: Int = 0,
    val recentScans: List<HomeScanSummary> = emptyList(),
    val systemLogs: List<HomeSystemLog> = emptyList(),

    val systemStatus: String = "OPTIMAL",
    val uptime: String = "00:00:00",
    val isLoading: Boolean = true,
    /** 7 floats: scan count per day of week (Sun=0, Sat=6) for the last 7 days. */
    val weeklyBarValues: List<Float> = List(7) { 0f }
)

