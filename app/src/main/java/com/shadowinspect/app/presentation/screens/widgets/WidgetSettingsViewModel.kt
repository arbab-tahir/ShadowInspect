package com.shadowinspect.app.presentation.screens.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowinspect.app.domain.repository.WidgetRepository
import com.shadowinspect.app.domain.widgets.WidgetConfig
import com.shadowinspect.app.domain.widgets.WidgetPosition
import com.shadowinspect.app.domain.widgets.WidgetTemplate
import com.shadowinspect.app.domain.widgets.WidgetSize
import com.shadowinspect.app.domain.widgets.WidgetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    private val widgetRepository: WidgetRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WidgetSettingsState())
    val uiState: StateFlow<WidgetSettingsState> = _uiState.asStateFlow()
    
    init {
        loadWidgets()
    }
    
    private fun loadWidgets() {
        viewModelScope.launch {
            widgetRepository.getAllWidgets().collect { widgets ->
                _uiState.value = _uiState.value.copy(
                    currentWidgets = widgets
                )
            }
        }
    }
    
    fun getAvailableWidgets(): List<WidgetTemplate> {
        return widgetRepository.getAvailableWidgets()
    }
    
    fun addWidget(template: WidgetTemplate) {
        viewModelScope.launch {
            val maxRow = widgetRepository.getMaxRow() ?: 0
            
            val newWidget = WidgetConfig(
                widgetType = template.type,
                title = template.defaultTitle,
                size = template.defaultSize,
                position = WidgetPosition(
                    row = maxRow + 1,
                    col = 0,
                    rowSpan = 1,
                    colSpan = 1
                ),
                isVisible = true,
                refreshInterval = 5
            )
            
            widgetRepository.saveWidget(newWidget)
        }
    }
    
    fun removeWidget(widgetId: Long) {
        viewModelScope.launch {
            val widget = _uiState.value.currentWidgets.find { it.id == widgetId }
            widget?.let {
                widgetRepository.deleteWidget(it)
            }
        }
    }
    
    fun toggleWidgetVisibility(widgetId: Long) {
        viewModelScope.launch {
            val widget = _uiState.value.currentWidgets.find { it.id == widgetId }
            widget?.let {
                widgetRepository.setWidgetVisibility(widgetId, !it.isVisible)
            }
        }
    }
    
    fun resetToDefault() {
        viewModelScope.launch {
            widgetRepository.initializeDefaultLayout()
        }
    }
    
    fun applyPreset(presetName: String) {
        viewModelScope.launch {
            widgetRepository.deleteAllWidgets()
            
            when (presetName) {
                "security" -> applySecurityPreset()
                "analytics" -> applyAnalyticsPreset()
                "minimal" -> applyMinimalPreset()
            }
        }
    }
    
    private suspend fun applySecurityPreset() {
        val widgets = listOf(
            WidgetConfig(
                widgetType = WidgetType.SECURITY_SCORE,
                title = "Security Score",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(0, 0, 1, 1),
                isVisible = true
            ),
            WidgetConfig(
                widgetType = WidgetType.RISK_GAUGE,
                title = "Risk Level",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(0, 1, 1, 1),
                isVisible = true
            ),
            WidgetConfig(
                widgetType = WidgetType.RECOMMENDATIONS,
                title = "Recommendations",
                size = WidgetSize.LARGE,
                position = WidgetPosition(1, 0, 2, 2),
                isVisible = true
            ),
            WidgetConfig(
                widgetType = WidgetType.THREAT_TRENDS,
                title = "Threat Trends",
                size = WidgetSize.WIDE,
                position = WidgetPosition(3, 0, 1, 3),
                isVisible = true
            )
        )
        
        widgets.forEach { widgetRepository.saveWidget(it) }
    }
    
    private suspend fun applyAnalyticsPreset() {
        val widgets = listOf(
            WidgetConfig(
                widgetType = WidgetType.THREAT_TRENDS,
                title = "Threat Trends",
                size = WidgetSize.LARGE,
                position = WidgetPosition(0, 0, 2, 2),
                isVisible = true
            ),
            WidgetConfig(
                widgetType = WidgetType.PREDICTIONS,
                title = "Predictions",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(0, 2, 1, 1),
                isVisible = true
            ),
            WidgetConfig(
                widgetType = WidgetType.WEEKLY_FORECAST,
                title = "Weekly Forecast",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(1, 2, 1, 1),
                isVisible = true
            ),
            WidgetConfig(
                widgetType = WidgetType.TECHNIQUE_PIE,
                title = "Top Techniques",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(2, 0, 1, 1),
                isVisible = true
            )
        )
        
        widgets.forEach { widgetRepository.saveWidget(it) }
    }
    
    private suspend fun applyMinimalPreset() {
        val widgets = listOf(
            WidgetConfig(
                widgetType = WidgetType.SECURITY_SCORE,
                title = "Security Score",
                size = WidgetSize.LARGE,
                position = WidgetPosition(0, 0, 2, 2),
                isVisible = true
            ),
            WidgetConfig(
                widgetType = WidgetType.RECENT_ACTIVITY,
                title = "Recent",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(2, 0, 2, 2),
                isVisible = true
            )
        )
        
        widgets.forEach { widgetRepository.saveWidget(it) }
    }
    
    data class WidgetSettingsState(
        val currentWidgets: List<WidgetConfig> = emptyList(),
        val isLoading: Boolean = false
    )
}
