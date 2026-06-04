package com.shadowinspect.app.domain.score

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.data.db.ScoreDao
import com.shadowinspect.app.data.db.ScanEntity
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class SecurityScoreEngine @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val scanDao: ScanDao,
    private val mitreDao: MitreDetectionDao,
    private val scoreDao: ScoreDao
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Calculate complete security score
     */
    suspend fun calculateScore(): SecurityScore = withContext(Dispatchers.IO) {
        val categories = calculateCategoryScores()
        val factors = identifyScoreFactors(categories)
        val overall = calculateOverallScore(categories, factors)
        val trend = analyzeTrend()
        val recommendations = generateRecommendations(categories, factors)
        val history = getScoreHistory(30) // Last 30 days
        
        // Save to history if it's a new day or significant change
        saveScoreToHistory(overall, categories)
        
        SecurityScore(
            overall = overall,
            categories = categories,
            factors = factors,
            trend = trend,
            recommendations = recommendations,
            history = history
        )
    }
    
    /**
     * Save the current score to history
     */
    private suspend fun saveScoreToHistory(overall: Int, categories: ScoreCategories) {
        val lastScore = scoreDao.getScoreHistorySince(System.currentTimeMillis() - 24 * 60 * 60 * 1000L).lastOrNull()
        
        // Save if no score today or if it changed more than 5 points
        if (lastScore == null || Math.abs(lastScore.overallScore - overall) > 5) {
            scoreDao.insertScore(
                HistoricalScore(
                    overallScore = overall,
                    appScore = categories.appSecurity.score,
                    urlScore = categories.urlSafety.score,
                    phoneScore = categories.phoneSecurity.score,
                    systemScore = categories.systemHealth.score,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Calculate individual category scores
     */
    private suspend fun calculateCategoryScores(): ScoreCategories {
        return ScoreCategories(
            appSecurity = calculateAppSecurityScore(),
            urlSafety = calculateUrlSafetyScore(),
            phoneSecurity = calculatePhoneSecurityScore(),
            systemHealth = calculateSystemHealthScore(),
            privacyScore = calculatePrivacyScore(),
            networkSecurity = calculateNetworkSecurityScore()
        )
    }
    
    /**
     * App Security Score based on APK scans
     */
    private suspend fun calculateAppSecurityScore(): CategoryScore {
        val scans = scanDao.getScansByTypeSince("APK", System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L) // Last 30 days
        
        // If no scans, return default 100 but with note
        if (scans.isEmpty()) {
            return CategoryScore(
                name = "App Security",
                score = 100,
                weight = 0.25,
                factors = listOf("No apps scanned yet - score is default 100"),
                threatsFound = 0,
                lastScanTime = null
            )
        }
        
        var totalScore = 100
        val factors = mutableListOf<String>()
        var threatCount = 0
        
        scans.forEach { scan ->
            when (scan.riskLevel) {
                "CRITICAL" -> {
                    totalScore -= 25
                    threatCount++
                    factors.add("Critical app: ${scan.target}")
                }
                "HIGH" -> {
                    totalScore -= 15
                    threatCount++
                    factors.add("High risk app: ${scan.target}")
                }
                "MEDIUM" -> {
                    totalScore -= 8
                    threatCount++
                    factors.add("Medium risk app: ${scan.target}")
                }
                "LOW" -> {
                    totalScore -= 3
                    threatCount++
                    factors.add("Low risk app: ${scan.target}")
                }
            }
        }
        
        // Check for outdated apps
        val outdatedApps = checkOutdatedApps()
        if (outdatedApps.isNotEmpty()) {
            totalScore -= outdatedApps.size * 5
            factors.add("${outdatedApps.size} outdated apps")
        }
        
        return CategoryScore(
            name = "App Security",
            score = max(totalScore, 0).coerceIn(0, 100), // Ensure between 0-100
            weight = 0.25,
            factors = factors,
            threatsFound = threatCount,
            lastScanTime = scans.maxOfOrNull { it.timestamp }
        )
    }
    
    /**
     * URL Safety Score based on URL scans
     */
    private suspend fun calculateUrlSafetyScore(): CategoryScore {
        val scans = scanDao.getScansByTypeSince("URL", System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L)
        if (scans.isEmpty()) {
            return CategoryScore(
                name = "URL Safety",
                score = 100,
                weight = 0.20,
                factors = listOf("No URLs scanned yet"),
                threatsFound = 0,
                lastScanTime = null
            )
        }
        
        var totalScore = 100
        val factors = mutableListOf<String>()
        var threatCount = 0
        
        scans.forEach { scan ->
            when (scan.riskLevel) {
                "CRITICAL" -> {
                    totalScore -= 15
                    threatCount++
                    factors.add("Critical URL: ${scan.target}")
                }
                "HIGH" -> {
                    totalScore -= 8
                    threatCount++
                    factors.add("Suspicious URL: ${scan.target}")
                }
                "MEDIUM" -> {
                    totalScore -= 3
                    threatCount++
                    factors.add("Questionable URL: ${scan.target}")
                }
            }
        }
        
        return CategoryScore(
            name = "URL Safety",
            score = max(totalScore, 0),
            weight = 0.20,
            factors = factors,
            threatsFound = threatCount,
            lastScanTime = scans.maxOfOrNull { it.timestamp }
        )
    }
    
    /**
     * Phone Security Score based on call scans
     */
    private suspend fun calculatePhoneSecurityScore(): CategoryScore {
        val scans = scanDao.getScansByTypeSince("PHONE", System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L)
        if (scans.isEmpty()) {
            return CategoryScore(
                name = "Phone Security",
                score = 100,
                weight = 0.15,
                factors = listOf("No phone numbers scanned yet"),
                threatsFound = 0,
                lastScanTime = null
            )
        }
        
        var totalScore = 100
        val factors = mutableListOf<String>()
        var threatCount = 0
        
        scans.forEach { scan ->
            when (scan.riskLevel) {
                "CRITICAL" -> {
                    totalScore -= 25
                    threatCount++
                    factors.add("Scam number detected")
                }
                "HIGH" -> {
                    totalScore -= 12
                    threatCount++
                    factors.add("Suspicious caller")
                }
                "MEDIUM" -> {
                    totalScore -= 5
                    threatCount++
                    factors.add("Unknown risk number")
                }
            }
        }
        
        return CategoryScore(
            name = "Phone Security",
            score = max(totalScore, 0),
            weight = 0.15,
            factors = factors,
            threatsFound = threatCount,
            lastScanTime = scans.maxOfOrNull { it.timestamp }
        )
    }
    
    /**
     * System Health Score based on device configuration
     */
    private suspend fun calculateSystemHealthScore(): CategoryScore {
        var score = 100
        val factors = mutableListOf<String>()
        
        // Check for unknown sources installation
        if (canInstallUnknownApps()) {
            score -= 20
            factors.add("Unknown sources enabled - security risk")
        }
        
        // Check for developer options
        if (isDeveloperOptionsEnabled()) {
            score -= 10
            factors.add("Developer mode enabled")
        }
        
        // Check for screen lock
        if (!isScreenLockEnabled()) {
            score -= 15
            factors.add("No screen lock configured")
        }
        
        // Check Android version (older versions are less secure)
        val androidVersion = Build.VERSION.SDK_INT
        when {
            androidVersion < Build.VERSION_CODES.Q -> { // Android 10
                score -= 30
                factors.add("Android version too old (${Build.VERSION.RELEASE})")
            }
            androidVersion < Build.VERSION_CODES.TIRAMISU -> { // Android 13
                score -= 10
                factors.add("Consider updating Android version")
            }
        }
        
        // Check for security updates
        val securityPatch = Build.VERSION.SECURITY_PATCH
        try {
            val patchDate = dateFormat.parse(securityPatch) ?: Date()
            val sixMonthsAgo = Date(System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000)
            if (patchDate.before(sixMonthsAgo)) {
                score -= 15
                factors.add("Security patch out of date")
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        
        return CategoryScore(
            name = "System Health",
            score = max(score, 0),
            weight = 0.20,
            factors = factors,
            threatsFound = factors.size,
            lastScanTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Privacy Score based on permission exposure
     */
    private suspend fun calculatePrivacyScore(): CategoryScore {
        var score = 100
        val factors = mutableListOf<String>()
        
        // Check for sensitive permissions granted
        val dangerousPermissions = checkDangerousPermissions()
        if (dangerousPermissions.isNotEmpty()) {
            score -= dangerousPermissions.size * 5
            factors.add("${dangerousPermissions.size} sensitive permissions granted")
        }
        
        // Check for installed apps with many permissions
        val overprivilegedApps = findOverprivilegedApps()
        if (overprivilegedApps.isNotEmpty()) {
            score -= overprivilegedApps.size * 8
            factors.add("${overprivilegedApps.size} apps have excessive permissions")
        }
        
        return CategoryScore(
            name = "Privacy Score",
            score = max(score, 0),
            weight = 0.10,
            factors = factors,
            threatsFound = overprivilegedApps.size,
            lastScanTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Network Security Score
     */
    private suspend fun calculateNetworkSecurityScore(): CategoryScore {
        // Placeholder - will be enhanced with network scanning later
        return CategoryScore(
            name = "Network Security",
            score = 85,
            weight = 0.10,
            factors = listOf("Basic network protection active"),
            threatsFound = 0,
            lastScanTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Calculate overall score weighted by categories
     */
    private fun calculateOverallScore(
        categories: ScoreCategories,
        factors: List<ScoreFactor>
    ): Int {
        val weightedScore = (
            categories.appSecurity.score * categories.appSecurity.weight +
            categories.urlSafety.score * categories.urlSafety.weight +
            categories.phoneSecurity.score * categories.phoneSecurity.weight +
            categories.systemHealth.score * categories.systemHealth.weight +
            categories.privacyScore.score * categories.privacyScore.weight +
            categories.networkSecurity.score * categories.networkSecurity.weight
        ).toInt()
        
        // Apply factor impacts
        val factorImpact = factors.sumOf { it.impact }
        return (weightedScore + factorImpact).coerceIn(0, 100)
    }
    
    /**
     * Identify positive and negative factors affecting score
     */
    private suspend fun identifyScoreFactors(categories: ScoreCategories): List<ScoreFactor> {
        val factors = mutableListOf<ScoreFactor>()
        
        // Positive factors
        if (categories.appSecurity.score > 80) {
            factors.add(
                ScoreFactor(
                    name = "Good App Hygiene",
                    impact = 5,
                    category = "App Security",
                    severity = "POSITIVE",
                    description = "Your apps are generally safe",
                    mitigation = null
                )
            )
        }
        
        if (categories.systemHealth.score > 80) {
            factors.add(
                ScoreFactor(
                    name = "Healthy Device",
                    impact = 5,
                    category = "System Health",
                    severity = "POSITIVE",
                    description = "Your device is well-configured",
                    mitigation = null
                )
            )
        }
        
        // Negative factors from categories
        categories.appSecurity.factors.forEach { factor ->
            if (factor.contains("Critical")) {
                factors.add(
                    ScoreFactor(
                        name = "Critical Apps Found",
                        impact = -20,
                        category = "App Security",
                        severity = "CRITICAL",
                        description = factor,
                        mitigation = "Uninstall suspicious apps immediately"
                    )
                )
            }
        }
        
        categories.systemHealth.factors.forEach { factor ->
            factors.add(
                ScoreFactor(
                    name = "System Issue",
                    impact = -10,
                    category = "System Health",
                    severity = "NEGATIVE",
                    description = factor,
                    mitigation = getSystemMitigation(factor)
                )
            )
        }
        
        return factors
    }
    
    /**
     * Analyze score trend over time
     */
    private suspend fun analyzeTrend(): ScoreTrend {
        val history = getScoreHistory(7) // Last 7 days
        if (history.size < 2) {
            return ScoreTrend(
                direction = TrendDirection.STABLE,
                changePercentage = 0f,
                periodDays = 7,
                significantEvents = emptyList()
            )
        }
        
        val oldest = history.first().overallScore
        val newest = history.last().overallScore
        val change = newest - oldest
        val percentChange = (change.toFloat() / max(oldest, 1)) * 100
        
        val direction = when {
            percentChange > 5 -> TrendDirection.UP
            percentChange < -5 -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }
        
        val events = mutableListOf<String>()
        if (history.size >= 2) {
            val yesterday = history[history.size - 2]
            if (newest > yesterday.overallScore + 10) {
                events.add("Big improvement! +${newest - yesterday.overallScore} points")
            } else if (newest < yesterday.overallScore - 10) {
                events.add("Alert! Score dropped by ${yesterday.overallScore - newest} points")
            }
        }
        
        return ScoreTrend(
            direction = direction,
            changePercentage = percentChange,
            periodDays = 7,
            significantEvents = events
        )
    }
    
    /**
     * Generate actionable recommendations
     */
    private suspend fun generateRecommendations(
        categories: ScoreCategories,
        factors: List<ScoreFactor>
    ): List<ScoreRecommendation> {
        val recommendations = mutableListOf<ScoreRecommendation>()
        
        // Critical recommendations first
        factors.filter { it.severity == "CRITICAL" }.forEach { factor ->
            recommendations.add(
                ScoreRecommendation(
                    id = UUID.randomUUID().toString(),
                    title = "Critical Issue: ${factor.name}",
                    description = factor.description,
                    priority = 5,
                    category = factor.category,
                    impact = "IMMEDIATE",
                    effort = "LOW",
                    actionUrl = null
                )
            )
        }
        
        // App security recommendations
        if (categories.appSecurity.score < 70) {
            recommendations.add(
                ScoreRecommendation(
                    id = UUID.randomUUID().toString(),
                    title = "Review Installed Apps",
                    description = "You have apps with security risks. Review and uninstall suspicious apps.",
                    priority = 4,
                    category = "App Security",
                    impact = "SHORT_TERM",
                    effort = "MEDIUM",
                    actionUrl = "app_scan"
                )
            )
        }
        
        // System health recommendations
        if (categories.systemHealth.score < 60) {
            if (categories.systemHealth.factors.any { it.lowercase().contains("unknown sources") }) {
                recommendations.add(
                    ScoreRecommendation(
                        id = UUID.randomUUID().toString(),
                        title = "Disable Unknown Sources",
                        description = "Installing apps from unknown sources is risky. Disable this in Settings.",
                        priority = 5,
                        category = "System Health",
                        impact = "IMMEDIATE",
                        effort = "LOW",
                        actionUrl = "settings"
                    )
                )
            }
            
            if (categories.systemHealth.factors.any { it.lowercase().contains("screen lock") }) {
                recommendations.add(
                    ScoreRecommendation(
                        id = UUID.randomUUID().toString(),
                        title = "Set Up Screen Lock",
                        description = "Your device has no screen lock. Anyone can access your data.",
                        priority = 5,
                        category = "System Health",
                        impact = "IMMEDIATE",
                        effort = "LOW",
                        actionUrl = "screen_lock"
                    )
                )
            }
        }
        
        // Privacy recommendations
        if (categories.privacyScore.score < 70) {
            recommendations.add(
                ScoreRecommendation(
                    id = UUID.randomUUID().toString(),
                    title = "Review App Permissions",
                    description = "Some apps have unnecessary permissions. Review and revoke sensitive permissions.",
                    priority = 3,
                    category = "Privacy",
                    impact = "SHORT_TERM",
                    effort = "MEDIUM",
                    actionUrl = "permissions"
                )
            )
        }
        
        // General maintenance
        recommendations.add(
            ScoreRecommendation(
                id = UUID.randomUUID().toString(),
                title = "Weekly Security Scan",
                description = "Run a full security scan weekly to stay protected",
                priority = 2,
                category = "General",
                impact = "LONG_TERM",
                effort = "LOW",
                actionUrl = "scan"
            )
        )
        
        return recommendations.distinctBy { it.title }.sortedByDescending { it.priority }
    }
    
    /**
     * Get score history for a period
     */
    private suspend fun getScoreHistory(days: Int): List<HistoricalScore> {
        val since = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        return scoreDao.getScoreHistorySince(since)
    }
    
    /**
     * Compare score with average users
     */
    suspend fun compareWithAverage(): ScoreComparison {
        val yourScore = calculateScore().overall
        val allScores = scanDao.getAllUserScores() 
        val average = if (allScores.isNotEmpty()) allScores.average().toInt() else 75
        
        val percentile = calculatePercentile(yourScore, allScores)
        val comparison = when {
            yourScore > average + 15 -> "Excellent! You're doing much better than average"
            yourScore > average + 5 -> "Good! You're above average"
            yourScore > average - 5 -> "Average - room for improvement"
            yourScore > average - 15 -> "Below average - take action"
            else -> "Critical - immediate action needed"
        }
        
        return ScoreComparison(
            yourScore = yourScore,
            averageScore = average,
            percentile = percentile,
            comparison = comparison
        )
    }
    
    /**
     * Get weekly summary for notifications
     */
    suspend fun getWeeklySummary(): WeeklyScoreSummary? {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val history = getScoreHistory(7)
        if (history.isEmpty()) return null
        
        val scores = history.map { it.overallScore }
        val firstScore = history.firstOrNull()?.overallScore ?: 0
        val lastScore = history.lastOrNull()?.overallScore ?: 0
        val improvement = lastScore - firstScore
        
        return WeeklyScoreSummary(
            weekStart = weekAgo,
            weekEnd = System.currentTimeMillis(),
            averageScore = scores.average().toInt(),
            bestScore = scores.maxOrNull() ?: 0,
            worstScore = scores.minOrNull() ?: 0,
            improvements = max(improvement, 0),
            newThreats = mitreDao.getTotalDetections(weekAgo),
            topRecommendation = generateRecommendations(
                calculateCategoryScores(),
                emptyList()
            ).firstOrNull()?.title ?: "Keep up the good work!"
        )
    }
    
    // Helper functions for system checks
    
    private fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS,
                0
            ) == 1
        }
    }
    
    private fun isDeveloperOptionsEnabled(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    }
    
    private fun isScreenLockEnabled(): Boolean {
        val lockManager = context.getSystemService(Context.KEYGUARD_SERVICE)
        return (lockManager as android.app.KeyguardManager).isKeyguardSecure
    }
    
    private fun checkDangerousPermissions(): List<String> {
        val dangerous = listOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_CONTACTS
        )
        
        return dangerous.filter { permission ->
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun findOverprivilegedApps(): List<String> {
        // Simplified - would need full package manager query
        return emptyList()
    }
    
    private fun checkOutdatedApps(): List<String> {
        // Simplified - would need version tracking
        return emptyList()
    }
    
    private fun getSystemMitigation(factor: String): String {
        return when {
            factor.lowercase().contains("unknown sources") -> "Settings → Security → Disable Unknown Sources"
            factor.lowercase().contains("screen lock") -> "Settings → Security → Screen Lock"
            factor.lowercase().contains("developer") -> "Settings → Developer Options → Disable"
            factor.lowercase().contains("android version") -> "Settings → System → System Update"
            else -> "Review system settings"
        }
    }
    
    private fun calculatePercentile(score: Int, allScores: List<Int>): Int {
        if (allScores.isEmpty()) return 50
        val better = allScores.count { it < score }
        return (better * 100 / allScores.size).coerceIn(0, 100)
    }
}
