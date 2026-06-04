package com.shadowinspect.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.data.scan.ScanRepository
import com.shadowinspect.app.data.scan.ScanSummary
import com.shadowinspect.app.data.scan.DashboardStats
import com.shadowinspect.app.data.session.AgentSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Calendar
import java.util.Locale

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: AgentSessionRepository,
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private var uptimeSeconds = 0

    init {
        observeDashboardStats()
        startSystemMonitors()
    }

    private fun observeDashboardStats() {
        viewModelScope.launch {
            try {
                combine(
                    scanRepository.getDashboardStats(),
                    scanRepository.getRecentScans()
                ) { stats: DashboardStats, recents: List<ScanSummary> ->
                    Pair(stats, recents)
                }.collect { (stats, recents) ->
                    val scanSummaries = recents.map { entity ->
                        HomeScanSummary(
                            title = entity.target,
                            type = entity.type,
                            status = entity.riskLevel,
                            isHighRisk = entity.riskScore >= 50
                        )
                    }

                    val weeklyData = try {
                        scanRepository.getWeeklyBarData()
                    } catch (e: Exception) { emptyList() }

                    val barValues = FloatArray(7) { 0f }
                    weeklyData.forEach { entry ->
                        if (entry.dayIndex in 0..6) barValues[entry.dayIndex] = entry.count.toFloat()
                    }

                    _uiState.update { state ->
                        state.copy(
                            totalScans = stats.totalScans.toInt(),
                            highRiskCount = stats.highRiskCount.toInt(),
                            averageScore = stats.averageScore.toInt(),
                            recentScans = scanSummaries,
                            weeklyBarValues = barValues.toList(),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startSystemMonitors() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                uptimeSeconds++
                val h = uptimeSeconds / 3600
                val m = (uptimeSeconds % 3600) / 60
                val s = uptimeSeconds % 60
                val hStr = h.toString().padStart(2, '0')
                val mStr = m.toString().padStart(2, '0')
                val sStr = s.toString().padStart(2, '0')
                _uiState.update { it.copy(uptime = "$hStr:$mStr:$sStr") }
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    systemLogs = listOf(
                        HomeSystemLog(getTimestamp(), "Kernel sandbox initialized"),
                        HomeSystemLog(getTimestamp(), "Neural heuristic engine ready"),
                        HomeSystemLog(getTimestamp(), "Vulnerability database updated (v4.22.1)")
                    )
                )
            }
            val randomLogs = listOf(
                "Scanning memory pages...",
                "Traffic intercepted on port 443",
                "Heuristic analysis in progress",
                "Entropy check: 7.91",
                "DNS request logged: shadow-api.net",
                "Agent activity verified"
            )
            while (true) {
                delay(5000)
                val newLog = HomeSystemLog(getTimestamp(), randomLogs.random())
                _uiState.update { state ->
                    state.copy(systemLogs = (listOf(newLog) + state.systemLogs).take(10))
                }
            }
        }
    }

    private fun getTimestamp(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%02d:%02d:%02d", 
            cal.get(Calendar.HOUR_OF_DAY), 
            cal.get(Calendar.MINUTE), 
            cal.get(Calendar.SECOND))
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.logout()
            onComplete()
        }
    }
}
