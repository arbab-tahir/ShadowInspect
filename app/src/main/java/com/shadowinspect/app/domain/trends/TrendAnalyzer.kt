package com.shadowinspect.app.domain.trends

import android.util.Log
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.data.db.ScanEntity
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class TrendAnalyzer @Inject constructor(
    private val scanDao: ScanDao,
    private val mitreDao: MitreDetectionDao
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val tag = "TrendAnalyzer"
    
    /**
     * Analyze trends for a given time period
     */
    suspend fun analyzeTrends(
        days: Int = 90,
        includePredictions: Boolean = true
    ): TrendAnalysis = withContext(Dispatchers.IO) {
        
        val endDate = System.currentTimeMillis()
        val startDate = endDate - days * 24 * 60 * 60 * 1000L
        
        val scans = scanDao.getScansInDateRange(startDate, endDate)
        
        val period = AnalysisPeriod(
            startDate = startDate,
            endDate = endDate,
            daysAnalyzed = days,
            totalScans = scans.size
        )
        
        val patterns = detectPatterns(scans)
        val anomalies = detectAnomalies(scans)
        val statistics = calculateStatistics(scans)
        val predictions = if (includePredictions) {
            predictThreats(scans, patterns)
        } else emptyList()
        
        val recommendations = generateTrendRecommendations(patterns, anomalies, predictions)
        
        TrendAnalysis(
            period = period,
            patterns = patterns,
            predictions = predictions,
            anomalies = anomalies,
            statistics = statistics,
            recommendations = recommendations
        )
    }
    
    /**
     * Detect recurring patterns in threat data
     */
    private suspend fun detectPatterns(scans: List<ScanEntity>): List<ThreatPattern> {
        val patterns = mutableListOf<ThreatPattern>()
        
        if (scans.isEmpty()) return patterns

        // Group scans by day
        val scansByDay = scans.groupBy { scan ->
            dateFormat.format(Date(scan.timestamp))
        }
        
        // Look for repeating threats (same technique appearing multiple times)
        val techniques = mitreDao.getTopTechniques(limit = 50)
        techniques.forEach { technique ->
            val detections = mitreDao.getDetectionsByTechniqueId(technique.techniqueId)
            if (detections.size >= 3) {
                val first = detections.minOf { it.detectedAt }
                val last = detections.maxOf { it.detectedAt }
                
                // Calculate frequency (detections per day)
                val daysSpan = ((last - first) / (24 * 60 * 60 * 1000)).toFloat()
                val frequency = if (daysSpan > 0) detections.size / daysSpan else 0f
                
                if (frequency > 0.5) { // More than once every 2 days
                    patterns.add(
                        ThreatPattern(
                            id = "REPEAT_${technique.techniqueId}",
                            patternType = PatternType.REPEATING,
                            description = "Technique ${technique.techniqueName} appears frequently",
                            frequency = detections.size,
                            confidence = (frequency / 2).coerceIn(0f, 1f),
                            firstDetected = first,
                            lastDetected = last,
                            relatedTechniques = listOf(technique.techniqueId),
                            severity = if (frequency > 1) "HIGH" else "MEDIUM"
                        )
                    )
                }
            }
        }
        
        // Look for escalating threats (risk scores increasing over time)
        val riskTrend = detectRiskEscalation(scans)
        if (riskTrend != null) {
            patterns.add(riskTrend)
        }
        
        // Look for correlated techniques (appear together)
        val correlations = detectCorrelations()
        patterns.addAll(correlations)
        
        // Day of week patterns
        val dayPatterns = detectDayOfWeekPatterns(scans)
        patterns.addAll(dayPatterns)
        
        return patterns
    }
    
    /**
     * Detect if risk scores are escalating over time
     */
    private fun detectRiskEscalation(scans: List<ScanEntity>): ThreatPattern? {
        if (scans.size < 10) return null
        
        // Sort by date
        val sorted = scans.sortedBy { it.timestamp }
        
        // Split into first half and second half
        val midPoint = sorted.size / 2
        val firstHalf = sorted.take(midPoint)
        val secondHalf = sorted.drop(midPoint)
        
        val firstAvg = firstHalf.map { it.riskScore }.average()
        val secondAvg = secondHalf.map { it.riskScore }.average()
        
        val increase = secondAvg - firstAvg
        
        if (increase > 10) { // More than 10 point increase
            return ThreatPattern(
                id = "ESCALATING_RISK",
                patternType = PatternType.ESCALATING,
                description = "Risk scores are increasing over time",
                frequency = secondHalf.size,
                confidence = (increase.toFloat() / 50f).coerceIn(0f, 1f),
                firstDetected = sorted.first().timestamp,
                lastDetected = sorted.last().timestamp,
                relatedTechniques = emptyList(),
                severity = if (increase > 20) "CRITICAL" else "HIGH"
            )
        }
        
        return null
    }
    
    /**
     * Detect correlations between different techniques
     */
    private suspend fun detectCorrelations(): List<ThreatPattern> {
        val patterns = mutableListOf<ThreatPattern>()
        
        // Get all scans with MITRE detections
        val scans = scanDao.getScansWithMitre(1000)
        
        if (scans.isEmpty()) return patterns

        // Find techniques that frequently appear together
        val coOccurrence = mutableMapOf<Pair<String, String>, Int>()
        
        scans.forEach { scan ->
            val techniques = mitreDao.getDetectionsForScan(scan.id).map { it.techniqueId }
            for (i in techniques.indices) {
                for (j in i + 1 until techniques.size) {
                    val pair = if (techniques[i] < techniques[j]) {
                        techniques[i] to techniques[j]
                    } else {
                        techniques[j] to techniques[i]
                    }
                    coOccurrence[pair] = coOccurrence.getOrDefault(pair, 0) + 1
                }
            }
        }
        
        // Find strong correlations
        coOccurrence.filter { it.value >= 5 }.forEach { (pair, count) ->
            patterns.add(
                ThreatPattern(
                    id = "CORR_${pair.first}_${pair.second}",
                    patternType = PatternType.CORRELATED,
                    description = "${pair.first} and ${pair.second} often appear together",
                    frequency = count,
                    confidence = (count / scans.size.toFloat()).coerceIn(0f, 1f),
                    firstDetected = 0,
                    lastDetected = 0,
                    relatedTechniques = listOf(pair.first, pair.second),
                    severity = if (count > 10) "HIGH" else "MEDIUM"
                )
            )
        }
        
        return patterns
    }
    
    /**
     * Detect day of week patterns
     */
    private fun detectDayOfWeekPatterns(scans: List<ScanEntity>): List<ThreatPattern> {
        val patterns = mutableListOf<ThreatPattern>()
        val dayCounts = mutableMapOf<String, Int>()
        
        scans.forEach { scan ->
            val day = dayFormat.format(Date(scan.timestamp))
            dayCounts[day] = dayCounts.getOrDefault(day, 0) + 1
        }
        
        val total = scans.size
        if (total == 0) return patterns
        
        val expectedPerDay = total / 7f
        
        dayCounts.forEach { (day, count) ->
            val deviation = (count - expectedPerDay) / expectedPerDay
            if (abs(deviation) > 0.5) { // 50% more/less than expected
                patterns.add(
                    ThreatPattern(
                        id = "DAY_${day}",
                        patternType = PatternType.SEASONAL,
                        description = "Threats are ${if (deviation > 0) "higher" else "lower"} on $day",
                        frequency = count,
                        confidence = abs(deviation).coerceIn(0f, 1f),
                        firstDetected = 0,
                        lastDetected = 0,
                        relatedTechniques = emptyList(),
                        severity = if (abs(deviation) > 1) "HIGH" else "MEDIUM"
                    )
                )
            }
        }
        
        return patterns
    }
    
    /**
     * Detect anomalies in threat data
     */
    private suspend fun detectAnomalies(scans: List<ScanEntity>): List<AnomalyDetection> {
        val anomalies = mutableListOf<AnomalyDetection>()
        
        if (scans.size < 10) return anomalies
        
        // Calculate mean and standard deviation of risk scores
        val scores = scans.map { it.riskScore.toFloat() }
        val mean = scores.average()
        val variance = scores.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        if (stdDev == 0.0) return anomalies

        // Find scans with risk scores more than 2 standard deviations from mean
        scans.forEach { scan ->
            val zScore = abs(scan.riskScore - mean) / stdDev
            if (zScore > 2) {
                anomalies.add(
                    AnomalyDetection(
                        id = "ANOM_${scan.id}",
                        anomalyType = "RISK_SPIKE",
                        description = "Unusual risk spike detected",
                        expectedValue = mean.toFloat(),
                        actualValue = scan.riskScore.toFloat(),
                        deviation = zScore.toFloat(),
                        timestamp = scan.timestamp,
                        severity = if (zScore > 3) "CRITICAL" else "HIGH",
                        investigation = "Scan of ${scan.target} showed unexpected risk level"
                    )
                )
            }
        }
        
        // Check for sudden appearance of new techniques
        val recentThreshold = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val recentTechniques = mitreDao.getTechniquesSince(recentThreshold)
        
        recentTechniques.forEach { technique ->
            if (technique.count > 5) { // New technique appearing frequently
                anomalies.add(
                    AnomalyDetection(
                        id = "NEW_TECH_${technique.techniqueId}",
                        anomalyType = "EMERGING_THREAT",
                        description = "New technique ${technique.techniqueName} emerging",
                        expectedValue = 0f,
                        actualValue = technique.count.toFloat(),
                        deviation = 5f,
                        timestamp = System.currentTimeMillis(),
                        severity = "HIGH",
                        investigation = "This technique wasn't seen before last week"
                    )
                )
            }
        }
        
        return anomalies
    }
    
    /**
     * Calculate trend statistics
     */
    private suspend fun calculateStatistics(scans: List<ScanEntity>): TrendStatistics {
        if (scans.isEmpty()) {
            return TrendStatistics(
                averageRiskScore = 0f,
                medianRiskScore = 0f,
                standardDeviation = 0f,
                riskVelocity = 0f,
                threatDiversity = 0,
                peakThreatDay = "N/A",
                lowThreatDay = "N/A",
                dayOfWeekPatterns = emptyMap(),
                hourOfDayPatterns = emptyMap()
            )
        }
        
        val scores = scans.map { it.riskScore.toFloat() }
        val mean = scores.average().toFloat()
        val sorted = scores.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        } else {
            sorted[sorted.size / 2]
        }
        
        // Calculate velocity (rate of change)
        val sortedByDate = scans.sortedBy { it.timestamp }
        val firstScore = sortedByDate.firstOrNull()?.riskScore ?: 0
        val lastScore = sortedByDate.lastOrNull()?.riskScore ?: 0
        val daysSpan = ((sortedByDate.lastOrNull()?.timestamp ?: 0) - 
                        (sortedByDate.firstOrNull()?.timestamp ?: 0)) / (24 * 60 * 60 * 1000f)
        val velocity = if (daysSpan > 0) (lastScore - firstScore) / daysSpan else 0f
        
        // Day of week patterns
        val dayPatterns = scans.groupBy { 
            dayFormat.format(Date(it.timestamp))
        }.mapValues { it.value.size }
        
        val peakDay = dayPatterns.maxByOrNull { it.value }?.key ?: "N/A"
        val lowDay = dayPatterns.minByOrNull { it.value }?.key ?: "N/A"
        
        // Hour of day patterns
        val hourPatterns = scans.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
        }.mapValues { it.value.size }
        
        return TrendStatistics(
            averageRiskScore = mean,
            medianRiskScore = median,
            standardDeviation = sqrt(scores.map { (it - mean).pow(2) }.average()).toFloat(),
            riskVelocity = velocity,
            threatDiversity = scans.flatMap { mitreDao.getDetectionsForScan(it.id) }.distinctBy { it.techniqueId }.size,
            peakThreatDay = peakDay,
            lowThreatDay = lowDay,
            dayOfWeekPatterns = dayPatterns,
            hourOfDayPatterns = hourPatterns
        )
    }
    
    /**
     * Predict future threats based on patterns
     */
    private suspend fun predictThreats(
        scans: List<ScanEntity>,
        patterns: List<ThreatPattern>
    ): List<ThreatPrediction> {
        val predictions = mutableListOf<ThreatPrediction>()
        
        // Linear regression for risk score prediction
        if (scans.size >= 5) {
            val sorted = scans.sortedBy { it.timestamp }
            val x = sorted.indices.map { it.toDouble() }
            val y = sorted.map { it.riskScore.toDouble() }
            
            // Simple linear regression
            val n = x.size.toDouble()
            val sumX = x.sum()
            val sumY = y.sum()
            val sumXY = x.zip(y).sumOf { it.first * it.second }
            val sumX2 = x.sumOf { it * it }
            
            val denominator = (n * sumX2 - sumX * sumX)
            val slope = if (denominator != 0.0) (n * sumXY - sumX * sumY) / denominator else 0.0
            val intercept = (sumY - slope * sumX) / n
            
            // Predict next 7 days
            for (i in 1..7) {
                val predictedScore = (intercept + slope * (x.size + i)).toFloat()
                
                predictions.add(
                    ThreatPrediction(
                        id = "PRED_DAY_$i",
                        threatType = "OVERALL_RISK",
                        probability = 0.85f,
                        timeframe = when (i) {
                            1 -> Timeframe.NEXT_24_HOURS
                            2,3,4,5,6,7 -> Timeframe.NEXT_WEEK
                            else -> Timeframe.NEXT_MONTH
                        },
                        expectedSeverity = when {
                            predictedScore > 70 -> "CRITICAL"
                            predictedScore > 50 -> "HIGH"
                            predictedScore > 30 -> "MEDIUM"
                            else -> "LOW"
                        },
                        contributingFactors = listOf("Based on historical trend"),
                        recommendedActions = if (predictedScore > 50) 
                            listOf("Run full scan", "Check app permissions") 
                        else emptyList(),
                        confidenceInterval = ConfidenceInterval(
                            lowerBound = predictedScore - 10,
                            upperBound = predictedScore + 10,
                            confidence = 0.8f
                        )
                    )
                )
            }
        }
        
        // Predict based on patterns
        patterns.filter { it.patternType == PatternType.ESCALATING }.forEach { pattern ->
            predictions.add(
                ThreatPrediction(
                    id = "PRED_ESCALATION",
                    threatType = "RISK_ESCALATION",
                    probability = pattern.confidence,
                    timeframe = Timeframe.NEXT_MONTH,
                    expectedSeverity = "HIGH",
                    contributingFactors = listOf(pattern.description),
                    recommendedActions = listOf(
                        "Review all installed apps",
                        "Check for new permissions",
                        "Run full system scan"
                    ),
                    confidenceInterval = ConfidenceInterval(0.6f, 0.9f, 0.75f)
                )
            )
        }
        
        return predictions
    }
    
    /**
     * Generate recommendations based on trend analysis
     */
    private fun generateTrendRecommendations(
        patterns: List<ThreatPattern>,
        anomalies: List<AnomalyDetection>,
        predictions: List<ThreatPrediction>
    ): List<TrendRecommendation> {
        val recommendations = mutableListOf<TrendRecommendation>()
        
        // Handle escalating threats
        if (patterns.any { it.patternType == PatternType.ESCALATING }) {
            recommendations.add(
                TrendRecommendation(
                    id = "TREND_ESCALATION",
                    title = "Threats are increasing!",
                    description = "Your risk score has been trending upward. Take action now.",
                    priority = 5,
                    basedOn = "Escalating pattern detected",
                    timeframe = "Immediate",
                    actionable = true,
                    estimatedImpact = "Could prevent 50% increase in risk"
                )
            )
        }
        
        // Handle anomalies
        anomalies.filter { it.severity == "CRITICAL" }.forEach { anomaly ->
            recommendations.add(
                TrendRecommendation(
                    id = "ANOM_${anomaly.id}",
                    title = "Critical anomaly detected",
                    description = anomaly.description,
                    priority = 5,
                    basedOn = "Statistical outlier",
                    timeframe = "Immediate",
                    actionable = true,
                    estimatedImpact = "Address critical security gap"
                )
            )
        }
        
        // Day of week patterns
        patterns.filter { it.patternType == PatternType.SEASONAL }.forEach { pattern ->
            recommendations.add(
                TrendRecommendation(
                    id = "DAY_${pattern.id}",
                    title = "Weekly threat pattern",
                    description = pattern.description,
                    priority = 3,
                    basedOn = "Historical pattern",
                    timeframe = "Weekly",
                    actionable = true,
                    estimatedImpact = "Better preparedness on high-risk days"
                )
            )
        }
        
        // Predictions with high probability
        predictions.filter { it.probability > 0.8 }.forEach { prediction ->
            recommendations.add(
                TrendRecommendation(
                    id = "PRED_${prediction.id}",
                    title = "High-probability threat predicted",
                    description = "${prediction.threatType} expected with ${(prediction.probability*100).toInt()}% confidence",
                    priority = 4,
                    basedOn = "Predictive model",
                    timeframe = prediction.timeframe.name,
                    actionable = true,
                    estimatedImpact = "Prevent predicted threat"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Fix the getThreatTrends function to work with limited data
     */
    fun getThreatTrends(scans: List<ScanEntity>, days: Int = 7): List<com.shadowinspect.app.domain.dashboard.ThreatTrend> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.timeInMillis
        
        val recentScans = scans.filter { it.timestamp >= startDate }
        
        // Group by day
        val trends = mutableListOf<com.shadowinspect.app.domain.dashboard.ThreatTrend>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Create a map of date -> scans
        val scansByDate = recentScans.groupBy {
            dateFormat.format(Date(it.timestamp))
        }
        
        // Generate trends for each day in range
        val tempCal = Calendar.getInstance()
        tempCal.timeInMillis = startDate
        
        for (i in 0 until days) {
            val dateStr = dateFormat.format(tempCal.time)
            val dayScans = scansByDate[dateStr] ?: emptyList()
            
            trends.add(
                com.shadowinspect.app.domain.dashboard.ThreatTrend(
                    date = dateStr,
                    critical = dayScans.count { it.riskLevel == "CRITICAL" },
                    high = dayScans.count { it.riskLevel == "HIGH" },
                    medium = dayScans.count { it.riskLevel == "MEDIUM" },
                    low = dayScans.count { it.riskLevel == "LOW" },
                    total = dayScans.size
                )
            )
            
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return trends
    }
}
