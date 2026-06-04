package com.shadowinspect.app.presentation.screens.dashboard

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.dashboard.ActivityItem
import com.shadowinspect.app.domain.dashboard.DashboardExportManager
import com.shadowinspect.app.domain.dashboard.TacticCount
import com.shadowinspect.app.domain.dashboard.TechniqueCount 
import com.shadowinspect.app.domain.dashboard.ThreatTrend
import com.shadowinspect.app.domain.repository.DashboardRepository
import com.shadowinspect.app.domain.repository.WidgetRepository
import com.shadowinspect.app.domain.widgets.*
import com.shadowinspect.app.domain.score.*
import com.shadowinspect.app.domain.trends.*
import com.shadowinspect.app.data.db.ScanDao
import com.shadowinspect.app.data.db.MitreDetectionDao
import com.shadowinspect.app.utils.TestDataGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val exportManager: DashboardExportManager,
    private val scoreEngine: SecurityScoreEngine,
    private val trendAnalyzer: TrendAnalyzer,
    private val forecastGenerator: WeeklyForecastGenerator,
    private val widgetRepository: WidgetRepository,
    private val scanDao: ScanDao,
    private val mitreDao: MitreDetectionDao,
    private val testDataGenerator: TestDataGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _widgetState = MutableStateFlow(WidgetState())
    val widgetState: StateFlow<WidgetState> = _widgetState.asStateFlow()

    private var lastDashboardState: com.shadowinspect.app.domain.dashboard.DashboardState? = null

    init {
        loadWidgets()
        observeDatabaseChanges()
    }

    // Flag to prevent re-initializing after user deliberately clears all widgets
    private var hasInitializedWidgets = false

    private fun loadWidgets() {
        viewModelScope.launch {
            widgetRepository.getAllWidgets().collect { widgets ->
                if (widgets.isEmpty() && !hasInitializedWidgets) {
                    // First launch — auto-initialize default layout
                    hasInitializedWidgets = true
                    widgetRepository.initializeDefaultLayout()
                } else {
                    // Normal update — respect whatever the user has set
                    hasInitializedWidgets = true
                    _widgetState.value = _widgetState.value.copy(
                        widgets = widgets
                    )
                }
            }
        }
    }

    /**
     * Observes live changes in the database and triggers a dashboard refresh 
     * whenever new scans are added or removed.
     */
    private fun observeDatabaseChanges() {
        viewModelScope.launch {
            scanDao.getTotalScansFlow().collect { _ ->
                // Wait for the widgets to be loaded
                if (hasInitializedWidgets) {
                    loadDashboardData()
                } else {
                    // Small delay to ensure widgets initialize before data load hits
                    kotlinx.coroutines.delay(500)
                    loadDashboardData()
                }
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Fetch instantaneous snapshot perfectly, with fallback
            val dbState = try {
                repository.getDashboardStateSnapshot()
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Error loading DB State", e)
                com.shadowinspect.app.domain.dashboard.DashboardState(
                    0, 0, 0, 0, 0, 
                    com.shadowinspect.app.domain.dashboard.RiskDistribution(0,0,0,0,0), 
                    emptyList(), emptyList(), emptyList(), emptyList()
                )
            }
            
            // Use a mock "demo" state if DB is completely empty (totalScans == 0)
            val isDemo = dbState.totalScans == 0
            val dashboardState = if (isDemo) getDemoDashboardState() else dbState

            lastDashboardState = dashboardState

            // Calculate sophisticated score and recommendations
            val score = try {
                if (isDemo) getDemoSecurityScore() else scoreEngine.calculateScore()
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Error calculating score", e)
                // Fallback: calculate basic score directly from DB without the engine
                val avgRisk = try { scanDao.getAverageRiskScore().toInt() } catch (_: Exception) { 0 }
                // Security Score is INVERSE of risk (low risk = high security)
                val fallbackScore = (100 - avgRisk).coerceIn(0, 100)
                getDemoSecurityScore().copy(overall = fallbackScore)
            }
            
            val summary = try {
                if (isDemo) getDemoWeeklySummary() else scoreEngine.getWeeklySummary()
            } catch(e: Exception) {
                Log.e("DASHBOARD", "Error loading summary", e)
                null
            }
            
            val trendData = try {
                if (isDemo) getDemoTrendAnalysis() else trendAnalyzer.analyzeTrends(days = 30)
            } catch(e: Exception) {
                Log.e("DASHBOARD", "Error analyzing trends", e)
                getDemoTrendAnalysis() // fallback to keep UI from crashing
            }
            
            val forecast = try {
                if (isDemo) getDemoForecast() else forecastGenerator.generateWeeklyForecast()
            } catch(e: Exception) {
                Log.e("DASHBOARD", "Error generating forecast", e)
                null
            }
            
            val predictedFutureRisk = try {
                repository.predictFutureRisk(dashboardState.threatTrends)
            } catch(e: Exception) {
                0
            }

            val avgRawRisk = try { scanDao.getAverageRiskScore().toInt() } catch (_: Exception) { 0 }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                securityScore = score.overall,
                averageRiskScore = avgRawRisk,
                categoryScores = score.categories,
                predictedScore = predictedFutureRisk,
                totalScans = dashboardState.totalScans.takeIf { it > 0 } ?: 15,
                highRiskCount = dashboardState.highRiskCount.takeIf { !isDemo } ?: 2,
                criticalCount = dashboardState.riskDistribution.critical.takeIf { !isDemo } ?: 1,
                totalTechniques = dashboardState.techniqueDistribution.sumOf { it.count }.takeIf { !isDemo } ?: 16,
                topTechniques = dashboardState.techniqueDistribution.take(10).takeIf { it.isNotEmpty() } ?: getDemoDashboardState().techniqueDistribution,
                tacticDistribution = dashboardState.tacticDistribution.takeIf { it.isNotEmpty() } ?: getDemoDashboardState().tacticDistribution,
                threatTrends = dashboardState.threatTrends.takeIf { it.isNotEmpty() } ?: getDemoDashboardState().threatTrends,
                recentActivity = dashboardState.recentActivity.takeIf { it.isNotEmpty() } ?: getDemoDashboardState().recentActivity,
                scoreFactors = score.factors,
                recommendations = score.recommendations,
                scoreHistory = score.history,
                weeklySummary = summary,
                threatPatterns = if (isDemo) getDemoTrendAnalysis().patterns else trendData.patterns,
                predictions = if (isDemo) getDemoTrendAnalysis().predictions else trendData.predictions,
                weeklyForecast = forecast,
                trendAnalysis = trendData,
                isDemoData = isDemo,
                error = null
            )
        }
    }

    fun exportDashboard() {
        val state = lastDashboardState ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val file = exportManager.exportDashboardSummary(state)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportFile = file
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun resetExportState() {
        _uiState.value = _uiState.value.copy(exportFile = null)
    }

    fun refreshData() {
        loadDashboardData()
    }

    // Add this debug function to see what data is actually flowing
    fun debugDashboardData() {
        viewModelScope.launch {
            Log.d("DASHBOARD_DEBUG", "=== DASHBOARD DATA DEBUG ===")
            
            // Check database directly
            var scans = scanDao.getAllScans()
            Log.d("DASHBOARD_DEBUG", "Total scans in DB: ${scans.size}")
            
            if (scans.isEmpty()) {
                Log.d("DASHBOARD_DEBUG", "Database is empty! Generating test data automatically...")
                testDataGenerator.generateTestData()
                scans = scanDao.getAllScans()
                Log.d("DASHBOARD_DEBUG", "Generated test data. New scan count: ${scans.size}")
            }
            
            scans.forEach { scan ->
                Log.d("DASHBOARD_DEBUG", "Scan: ${scan.scanType} - ${scan.target} - Risk: ${scan.riskScore} (${scan.riskLevel})")
            }
            
            // Check MITRE detections
            val detections = mitreDao.getAllDetections()
            Log.d("DASHBOARD_DEBUG", "Total MITRE detections: ${detections.size}")
            
            // Check current state
            Log.d("DASHBOARD_DEBUG", "Current UI State:")
            Log.d("DASHBOARD_DEBUG", "securityScore: ${_uiState.value.securityScore}")
            Log.d("DASHBOARD_DEBUG", "totalScans: ${_uiState.value.totalScans}")
            Log.d("DASHBOARD_DEBUG", "threatTrends size: ${_uiState.value.threatTrends.size}")
            Log.d("DASHBOARD_DEBUG", "topTechniques size: ${_uiState.value.topTechniques.size}")
            
            // Force refresh
            loadDashboardData()
        }
    }

    fun startDragging(widgetId: Long) {
        _widgetState.value = _widgetState.value.copy(
            isEditing = true,
            draggingWidgetId = widgetId
        )
    }

    fun updateWidgetPosition(widgetId: Long, offsetX: Int, offsetY: Int) {
        viewModelScope.launch {
            val widget = _widgetState.value.widgets.find { it.id == widgetId }
            widget?.let {
                val newPosition = it.position.copy(
                    row = (it.position.row + (offsetY / 160)).coerceAtLeast(0),
                    col = (it.position.col + (offsetX / 160)).coerceAtLeast(0)
                )
                widgetRepository.updateWidgetPosition(widgetId, newPosition)
            }
        }
    }

    fun removeWidget(widgetId: Long) {
        viewModelScope.launch {
            // Actually delete from DB so it doesn't reappear
            widgetRepository.deleteWidget(widgetId)
        }
    }

    // --- Demo Data Helpers ---
    private fun getDemoDashboardState(): com.shadowinspect.app.domain.dashboard.DashboardState {
        return com.shadowinspect.app.domain.dashboard.DashboardState(
            securityScore = 85,
            totalScans = 15,
            highRiskCount = 2,
            criticalCount = 1,
            mitigatedCount = 5,
            riskDistribution = com.shadowinspect.app.domain.dashboard.RiskDistribution(low = 10, medium = 3, high = 1, critical = 1, safe = 0),
            techniqueDistribution = listOf(
                TechniqueCount("T1059", "Command and Scripting Interpreter", 8),
                TechniqueCount("T1437", "Application Layer Protocol", 5),
                TechniqueCount("T1614", "System Location Tracking", 3)
            ),
            tacticDistribution = listOf(
                TacticCount("Discovery", 10),
                TacticCount("Execution", 5),
                TacticCount("Collection", 1)
            ),
            threatTrends = listOf(
                ThreatTrend("2024-03-01", 1, 0, 0, 0, 1),
                ThreatTrend("2024-03-02", 2, 1, 0, 0, 3)
            ),
            recentActivity = listOf(
                ActivityItem(1L, "URL", "http://suspicious-demo.com", 85, "High", System.currentTimeMillis(), 3),
                ActivityItem(2L, "APK", "com.demo.fakeapp", 92, "Critical", System.currentTimeMillis() - 86400000, 5)
            )
        )
    }

    private fun getDemoSecurityScore(): com.shadowinspect.app.domain.score.SecurityScore {
        return com.shadowinspect.app.domain.score.SecurityScore(
            overall = 85,
            categories = com.shadowinspect.app.domain.score.ScoreCategories(
                appSecurity = com.shadowinspect.app.domain.score.CategoryScore("App Security", 88, 0.4, emptyList(), 1, System.currentTimeMillis()),
                urlSafety = com.shadowinspect.app.domain.score.CategoryScore("URL Safety", 75, 0.2, emptyList(), 2, System.currentTimeMillis()),
                phoneSecurity = com.shadowinspect.app.domain.score.CategoryScore("Phone Security", 95, 0.1, emptyList(), 0, System.currentTimeMillis()),
                systemHealth = com.shadowinspect.app.domain.score.CategoryScore("System Health", 80, 0.2, emptyList(), 0, System.currentTimeMillis()),
                privacyScore = com.shadowinspect.app.domain.score.CategoryScore("Privacy", 90, 0.05, emptyList(), 0, System.currentTimeMillis()),
                networkSecurity = com.shadowinspect.app.domain.score.CategoryScore("Network", 85, 0.05, emptyList(), 0, System.currentTimeMillis())
            ),
            trend = com.shadowinspect.app.domain.score.ScoreTrend(
                direction = com.shadowinspect.app.domain.score.TrendDirection.UP,
                changePercentage = 5.0f,
                periodDays = 7,
                significantEvents = emptyList()
            ),
            factors = listOf(
                com.shadowinspect.app.domain.score.ScoreFactor("Scan Frequency", 90, "Behavior", "POSITIVE", "Keep scanning regularly", null),
                com.shadowinspect.app.domain.score.ScoreFactor("Device Risk", 80, "Threats", "CRITICAL", "2 high-risk items found", "Resolve critical issues")
            ),
            recommendations = listOf(
                com.shadowinspect.app.domain.score.ScoreRecommendation("Rec1", "Review critical threats", "Action needed", 5, "Threats", "IMMEDIATE", "LOW", null)
            ),
            history = listOf(
                com.shadowinspect.app.domain.score.HistoricalScore(
                    overallScore = 80,
                    appScore = 85,
                    urlScore = 75,
                    phoneScore = 90,
                    systemScore = 70,
                    timestamp = System.currentTimeMillis() - 86400000*2
                ),
                com.shadowinspect.app.domain.score.HistoricalScore(
                    overallScore = 85,
                    appScore = 88,
                    urlScore = 75,
                    phoneScore = 95,
                    systemScore = 80,
                    timestamp = System.currentTimeMillis() - 86400000*1
                )
            ),
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun getDemoWeeklySummary(): com.shadowinspect.app.domain.score.WeeklyScoreSummary {
        return com.shadowinspect.app.domain.score.WeeklyScoreSummary(
            weekStart = System.currentTimeMillis() - 86400000*7,
            weekEnd = System.currentTimeMillis(),
            averageScore = 82,
            bestScore = 88,
            worstScore = 75,
            improvements = 2,
            newThreats = 1,
            topRecommendation = "Run a full device scan"
        )
    }

    private fun getDemoTrendAnalysis(): com.shadowinspect.app.domain.trends.TrendAnalysis {
        return com.shadowinspect.app.domain.trends.TrendAnalysis(
            period = com.shadowinspect.app.domain.trends.AnalysisPeriod(System.currentTimeMillis() - 86400000*7, System.currentTimeMillis(), 7, 15),
            patterns = listOf(
                com.shadowinspect.app.domain.trends.ThreatPattern(
                    id = "p1",
                    patternType = com.shadowinspect.app.domain.trends.PatternType.ESCALATING,
                    description = "Increasing frequency of unauthorized access attempts from unknown IPs.",
                    severity = "HIGH",
                    confidence = 0.85f,
                    frequency = 12,
                    firstDetected = System.currentTimeMillis() - 86400000,
                    lastDetected = System.currentTimeMillis() - 3600000,
                    relatedTechniques = listOf("T1078", "T1110")
                ),
                com.shadowinspect.app.domain.trends.ThreatPattern(
                    id = "p2",
                    patternType = com.shadowinspect.app.domain.trends.PatternType.REPEATING,
                    description = "Scheduled beaconing detected to a known malicious command-and-control server.",
                    severity = "CRITICAL",
                    confidence = 0.92f,
                    frequency = 45,
                    firstDetected = System.currentTimeMillis() - 172800000,
                    lastDetected = System.currentTimeMillis() - 7200000,
                    relatedTechniques = listOf("T1071", "T1571")
                ),
                com.shadowinspect.app.domain.trends.ThreatPattern(
                    id = "p3",
                    patternType = com.shadowinspect.app.domain.trends.PatternType.EMERGING,
                    description = "New suspicious process behavior detected in local system context.",
                    severity = "MEDIUM",
                    confidence = 0.70f,
                    frequency = 3,
                    firstDetected = System.currentTimeMillis() - 43200000,
                    lastDetected = System.currentTimeMillis() - 86400000,
                    relatedTechniques = listOf("T1543")
                ),
                com.shadowinspect.app.domain.trends.ThreatPattern(
                    id = "p4",
                    patternType = com.shadowinspect.app.domain.trends.PatternType.CORRELATED,
                    description = "Correlated phishing URL clicks across multiple user accounts.",
                    severity = "HIGH",
                    confidence = 0.88f,
                    frequency = 8,
                    firstDetected = System.currentTimeMillis() - 259200000,
                    lastDetected = System.currentTimeMillis() - 172800000,
                    relatedTechniques = listOf("T1566")
                )
            ),
            predictions = listOf(com.shadowinspect.app.domain.trends.ThreatPrediction(
                id = "1",
                threatType = "T1059",
                probability = 0.6f,
                timeframe = com.shadowinspect.app.domain.trends.Timeframe.NEXT_WEEK,
                expectedSeverity = "Medium",
                contributingFactors = listOf("Missing OS Updates"),
                recommendedActions = listOf("Update software"),
                confidenceInterval = com.shadowinspect.app.domain.trends.ConfidenceInterval(0.4f, 0.8f, 0.7f)
            )),
            anomalies = emptyList(),
            statistics = com.shadowinspect.app.domain.trends.TrendStatistics(
                averageRiskScore = 85f,
                medianRiskScore = 85f,
                standardDeviation = 2.1f,
                riskVelocity = 0.5f,
                threatDiversity = 3,
                peakThreatDay = "Monday",
                lowThreatDay = "Sunday",
                dayOfWeekPatterns = emptyMap(),
                hourOfDayPatterns = emptyMap()
            ),
            recommendations = emptyList()
        )
    }

    private fun getDemoForecast(): com.shadowinspect.app.domain.trends.WeeklyForecast {
        return com.shadowinspect.app.domain.trends.WeeklyForecast(
            weekStarting = System.currentTimeMillis(),
            predictedThreats = emptyList(),
            overallRiskLevel = "Low",
            accuracy = null
        )
    }

    data class WidgetState(
        val widgets: List<WidgetConfig> = emptyList(),
        val isEditing: Boolean = false,
        val draggingWidgetId: Long? = null
    )

    data class DashboardUiState(
        val isLoading: Boolean = false,
        val isExporting: Boolean = false,
        val securityScore: Int = 0,
        val averageRiskScore: Int = 0,          // Raw average scan risk (0-100); used by Risk Gauge
        val categoryScores: ScoreCategories? = null, // Detailed per-category breakdown
        val predictedScore: Int = 0,
        val totalScans: Int = 0,
        val highRiskCount: Int = 0,
        val criticalCount: Int = 0,
        val totalTechniques: Int = 0,
        val topTechniques: List<TechniqueCount> = emptyList(),
        val tacticDistribution: List<TacticCount> = emptyList(),
        val threatTrends: List<ThreatTrend> = emptyList(),
        val recentActivity: List<ActivityItem> = emptyList(),
        val scoreFactors: List<ScoreFactor> = emptyList(),
        val recommendations: List<ScoreRecommendation> = emptyList(),
        val scoreHistory: List<HistoricalScore> = emptyList(),
        val weeklySummary: WeeklyScoreSummary? = null,
        val threatPatterns: List<ThreatPattern> = emptyList(),
        val predictions: List<ThreatPrediction> = emptyList(),
        val weeklyForecast: WeeklyForecast? = null,
        val trendAnalysis: TrendAnalysis? = null,
        val exportFile: java.io.File? = null,
        val isDemoData: Boolean = false,
        val error: String? = null
    )
}
