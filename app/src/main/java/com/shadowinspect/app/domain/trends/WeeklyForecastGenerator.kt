package com.shadowinspect.app.domain.trends

import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.ScanEntity
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class WeeklyForecastGenerator @Inject constructor(
    private val scanDao: ScanDao,
    private val trendAnalyzer: TrendAnalyzer
) {
    
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    
    /**
     * Generate forecast for upcoming week
     */
    suspend fun generateWeeklyForecast(): WeeklyForecast = withContext(Dispatchers.IO) {
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Analyze last 90 days for patterns
        val analysis = trendAnalyzer.analyzeTrends(days = 90, includePredictions = false)
        
        val predictions = mutableListOf<PredictedThreatForDay>()
        
        // Get day of week patterns
        val dayPatterns = analysis.statistics.dayOfWeekPatterns
        
        // Predict for each day of upcoming week
        val calendar = Calendar.getInstance()
        for (i in 0..6) {
            calendar.timeInMillis = weekStart + i * 24 * 60 * 60 * 1000L
            val dayName = dayFormat.format(calendar.time)
            
            val baseThreats = dayPatterns[dayName] ?: 5
            val trend = analysis.statistics.riskVelocity * (i + 1)
            
            val predictedThreats = (baseThreats * (1 + analysis.statistics.riskVelocity / 100)).toInt()
            val predictedRisk = (analysis.statistics.averageRiskScore + trend).toInt()
            
            predictions.add(
                PredictedThreatForDay(
                    day = dayName,
                    predictedThreats = predictedThreats,
                    predictedRisk = predictedRisk.coerceIn(0, 100),
                    actualThreats = null,
                    actualRisk = null
                )
            )
        }
        
        val overallRisk = if (predictions.isNotEmpty()) predictions.map { it.predictedRisk }.average().toInt() else 0
        val riskLevel = when {
            overallRisk > 70 -> "CRITICAL"
            overallRisk > 50 -> "HIGH"
            overallRisk > 30 -> "MEDIUM"
            else -> "LOW"
        }
        
        WeeklyForecast(
            weekStarting = weekStart,
            predictedThreats = predictions,
            overallRiskLevel = riskLevel
        )
    }
    
    /**
     * Update forecast accuracy (call after week passes)
     */
    suspend fun updateForecastAccuracy(
        forecast: WeeklyForecast,
        actualScans: List<ScanEntity>
    ) {
        val actualByDay = actualScans.groupBy {
            dayFormat.format(Date(it.timestamp))
        }
        
        var totalError = 0f
        var count = 0
        
        forecast.predictedThreats.forEach { prediction ->
            val actual = actualByDay[prediction.day]?.size ?: 0
            val error = if (prediction.predictedThreats > 0) {
                abs(prediction.predictedThreats - actual) / prediction.predictedThreats.toFloat()
            } else {
                actual.toFloat()
            }
            totalError += error
            count++
            
            // Update prediction with actual data
            prediction.actualThreats = actual
            prediction.actualRisk = actualByDay[prediction.day]?.map { it.riskScore }?.average()?.toInt()
        }
        
        // Update forecast accuracy
        forecast.accuracy = if (count > 0) (1 - (totalError / count)).coerceIn(0f, 1f) else null
    }
}
