package com.shadowinspect.app.domain.repository

import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.domain.dashboard.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val scanDao: ScanDao,
    private val mitreDao: MitreDetectionDao
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Get complete dashboard state snapshot at the exact current moment
     */
    suspend fun getDashboardStateSnapshot(timeRange: TimeRange = TimeRange.WEEK): DashboardState = withContext(Dispatchers.IO) {
        val scanStats = scanDao.getScanStats()
        val topTechniques = mitreDao.getTopTechniques(10)
        val tacticDist = mitreDao.getTacticDistribution()
        val activities = scanDao.getRecentActivitySnapshot(20)
        val trends = getThreatTrendsSnapshot(timeRange)
        
        val riskDist = calculateRiskDistribution(scanStats)
        val securityScore = calculateSecurityScore(riskDist)
        
        DashboardState(
            securityScore = securityScore,
            totalScans = scanStats.totalScans,
            highRiskCount = scanStats.highRiskCount,
            criticalCount = scanStats.criticalCount,
            mitigatedCount = scanStats.mitigatedCount,
            riskDistribution = riskDist,
            techniqueDistribution = topTechniques,
            tacticDistribution = tacticDist,
            recentActivity = activities,
            threatTrends = trends
        )
    }

    /**
     * Get recent MITRE detections
     */
    suspend fun getMitreDetectionsSnapshot(): List<com.shadowinspect.app.domain.mitre.MitreDetection> = withContext(Dispatchers.IO) {
        mitreDao.getAllDetections()
    }
    
    /**
     * Calculate risk distribution from scan stats
     */
    private fun calculateRiskDistribution(stats: ScanStats): RiskDistribution {
        return RiskDistribution(
            critical = stats.criticalCount,
            high = stats.highRiskCount,
            medium = stats.mediumRiskCount,
            low = stats.lowRiskCount,
            safe = stats.safeCount
        )
    }
    
    /**
     * Calculate overall security score (0-100)
     */
    private fun calculateSecurityScore(distribution: RiskDistribution): Int {
        if (distribution.total == 0) return 100
        
        // Base penalty for any risk found
        var penalty = 0.0
        
        // Critical issues impact the score heavily regardless of total count
        penalty += distribution.critical * 40.0
        penalty += distribution.high * 20.0
        penalty += distribution.medium * 8.0
        penalty += distribution.low * 2.0
        
        // Scaling penalty based on density if many scans
        val densityPenalty = if (distribution.total > 0) {
            val weightedSum = (distribution.critical * 10.0 + distribution.high * 5.0 + distribution.medium * 2.0 + distribution.low * 1.0)
            weightedSum / distribution.total * 20.0
        } else 0.0
        
        val totalPenalty = (penalty + densityPenalty).coerceAtMost(100.0)
        return (100.0 - totalPenalty).toInt()
    }
    
    /**
     * Predict future risk score based on recent trends
     * returns a predicted score (0-100) for the next week
     */
    fun predictFutureRisk(trends: List<ThreatTrend>): Int {
        if (trends.size < 2) return 100
        
        // Calculate the rate of change in high/critical threats
        var totalChange = 0.0
        for (i in 1 until trends.size) {
            val prevRisk = trends[i-1].critical + trends[i-1].high
            val currRisk = trends[i].critical + trends[i].high
            totalChange += (currRisk - prevRisk)
        }
        
        val averageChange = totalChange / (trends.size - 1)
        
        // Predict next 7 days based on current trend
        val currentRisk = trends.last().critical + trends.last().high
        val predictedRisk = (currentRisk + averageChange * 7).coerceAtLeast(0.0)
        
        // Convert to a score (0-100)
        val predictionScore = (100 - (predictedRisk * 15)).coerceIn(0.0, 100.0)
        return predictionScore.toInt()
    }
    
    /**
     * Get threat trends over time
     */
    private suspend fun getThreatTrendsSnapshot(timeRange: TimeRange): List<ThreatTrend> = withContext(Dispatchers.IO) {
        val endCal = Calendar.getInstance()
        val endDate = endCal.timeInMillis
        
        val startCal = Calendar.getInstance()
        startCal.add(
            when (timeRange) {
                TimeRange.DAY -> Calendar.DAY_OF_MONTH
                TimeRange.WEEK -> Calendar.WEEK_OF_YEAR
                TimeRange.MONTH -> Calendar.MONTH
                TimeRange.YEAR -> Calendar.YEAR
                TimeRange.ALL -> Calendar.YEAR
            },
            -1
        )
        val startDate = startCal.timeInMillis
        
        val scans = scanDao.getScansInDateRange(startDate, endDate)
        
        // Group scans by date string
        val groupedByDate = scans.groupBy { scan ->
            dateFormat.format(Date(scan.timestamp))
        }
        
        // Fill ALL days in range so the chart always has data points
        val trends = mutableListOf<ThreatTrend>()
        val dayCal = Calendar.getInstance()
        dayCal.timeInMillis = startDate
        // Normalize to start of day
        dayCal.set(Calendar.HOUR_OF_DAY, 0)
        dayCal.set(Calendar.MINUTE, 0)
        dayCal.set(Calendar.SECOND, 0)
        dayCal.set(Calendar.MILLISECOND, 0)
        
        val endNormalized = Calendar.getInstance()
        endNormalized.timeInMillis = endDate
        endNormalized.set(Calendar.HOUR_OF_DAY, 23)
        endNormalized.set(Calendar.MINUTE, 59)
        
        while (!dayCal.after(endNormalized)) {
            val dateStr = dateFormat.format(dayCal.time)
            val scansInDay = groupedByDate[dateStr] ?: emptyList()
            
            trends.add(
                ThreatTrend(
                    date = dateStr,
                    critical = scansInDay.count { it.riskLevel == "CRITICAL" },
                    high = scansInDay.count { it.riskLevel == "HIGH" },
                    medium = scansInDay.count { it.riskLevel == "MEDIUM" },
                    low = scansInDay.count { it.riskLevel == "LOW" },
                    total = scansInDay.size
                )
            )
            
            dayCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        trends
    }
    
    /**
     * Get security tips based on user's risk profile
     */
    suspend fun getPersonalizedTips(riskLevel: String): List<SecurityTip> = withContext(Dispatchers.IO) {
        val allTips = getAllSecurityTips()
        
        when (riskLevel) {
            "CRITICAL", "HIGH" -> allTips.filter { it.priority >= 4 }
            "MEDIUM" -> allTips.filter { it.priority >= 3 }
            else -> allTips.take(3)
        }
    }
    
    /**
     * Get all security tips
     */
    private fun getAllSecurityTips(): List<SecurityTip> {
        return listOf(
            SecurityTip(
                id = 1,
                title = "🔐 Check App Permissions",
                content = "Apps with SMS, Camera, and Location permissions can spy on you. Review them in Settings.",
                category = "Permissions",
                icon = "🔐",
                priority = 5
            ),
            SecurityTip(
                id = 2,
                title = "📱 Update Your Apps",
                content = "Outdated apps have security holes. Enable auto-updates in Play Store.",
                category = "Updates",
                icon = "📱",
                priority = 4
            ),
            SecurityTip(
                id = 3,
                title = "🚫 Avoid Unknown Links",
                content = "Don't click links in SMS from unknown numbers. They could be phishing.",
                category = "Phishing",
                icon = "🚫",
                priority = 5
            ),
            SecurityTip(
                id = 4,
                title = "🔒 Use Strong Passwords",
                content = "Use different passwords for each account. Try a password manager.",
                category = "Passwords",
                icon = "🔒",
                priority = 4
            ),
            SecurityTip(
                id = 5,
                title = "📸 Cover Your Camera",
                content = "Physically cover your camera when not in use. Malware can activate it remotely.",
                category = "Hardware",
                icon = "📸",
                priority = 3
            ),
            SecurityTip(
                id = 6,
                title = "📍 Limit Location Sharing",
                content = "Only give location to apps that truly need it (like Maps).",
                category = "Privacy",
                icon = "📍",
                priority = 4
            ),
            SecurityTip(
                id = 7,
                title = "🔍 Read MITRE Reports",
                content = "Our MITRE reports show exactly how apps might attack you. Check them!",
                category = "Education",
                icon = "🔍",
                priority = 3
            )
        )
    }
}
