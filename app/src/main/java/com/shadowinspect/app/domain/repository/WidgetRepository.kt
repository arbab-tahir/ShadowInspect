package com.shadowinspect.app.domain.repository

import android.content.Context
import com.google.gson.Gson
import com.shadowinspect.app.data.db.WidgetConfigDao
import com.shadowinspect.app.domain.widgets.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetDao: WidgetConfigDao,
    private val gson: Gson
) {
    
    /**
     * Get all visible widgets
     */
    fun getAllWidgets(): Flow<List<WidgetConfig>> = widgetDao.getAllWidgets()
    
    /**
     * Save widget configuration
     */
    suspend fun saveWidget(widget: WidgetConfig): Long {
        return widgetDao.insertWidget(widget)
    }
    
    /**
     * Update widget
     */
    suspend fun updateWidget(widget: WidgetConfig) {
        widgetDao.updateWidget(widget)
    }
    
    /**
     * Delete widget
     */
    suspend fun deleteWidget(widget: WidgetConfig) {
        widgetDao.deleteWidget(widget)
    }
    
    /**
     * Delete widget by ID (actually removes from DB or sets isVisible=false)
     * To prevent them from coming back, we can just delete or hide.
     * We'll just delete them to keep the DB clean.
     */
    suspend fun deleteWidget(widgetId: Long) {
        // Find and delete
        val widgets = widgetDao.getAllWidgets() // Flow, need one-shot, but DAO doesn't have it easily.
        // Actually, widgetDao.setWidgetVisibility(widgetId, false) is fine since we use SharedPreferences now.
        widgetDao.setWidgetVisibility(widgetId, false)
    }
    
    /**
     * Update widget position
     */
    suspend fun updateWidgetPosition(widgetId: Long, position: WidgetPosition) {
        // Since we use @Embedded, we need to fetch the widget and update it
        // Or we can add a specific update query to DAO.
        // Let's use the one I added to DAO: updateWidgetPosition(widgetId, row, col)
        widgetDao.updateWidgetPosition(widgetId, position.row, position.col)
    }
    
    /**
     * Toggle widget visibility
     */
    suspend fun setWidgetVisibility(widgetId: Long, isVisible: Boolean) {
        widgetDao.setWidgetVisibility(widgetId, isVisible)
    }
    
    /**
     * Get available widget templates
     */
    fun getAvailableWidgets(): List<WidgetTemplate> {
        return listOf(
            WidgetTemplate(
                type = WidgetType.RISK_GAUGE,
                defaultTitle = "Risk Gauge",
                defaultSize = WidgetSize.MEDIUM,
                description = "Visual representation of your current security risk",
                icon = "📊",
                category = "Core",
                isPremium = false
            ),
            WidgetTemplate(
                type = WidgetType.SECURITY_SCORE,
                defaultTitle = "Security Score",
                defaultSize = WidgetSize.SMALL,
                description = "Your overall security score with breakdown",
                icon = "🔢",
                category = "Core",
                isPremium = false
            ),
            WidgetTemplate(
                type = WidgetType.THREAT_TRENDS,
                defaultTitle = "Threat Trends",
                defaultSize = WidgetSize.LARGE,
                description = "Track threats over time with line charts",
                icon = "📈",
                category = "Analytics",
                isPremium = false
            ),
            WidgetTemplate(
                type = WidgetType.TECHNIQUE_PIE,
                defaultTitle = "Top Techniques",
                defaultSize = WidgetSize.MEDIUM,
                description = "See which MITRE techniques are most common",
                icon = "🥧",
                category = "Analytics",
                isPremium = false
            ),
            WidgetTemplate(
                type = WidgetType.RECENT_ACTIVITY,
                defaultTitle = "Recent Activity",
                defaultSize = WidgetSize.MEDIUM,
                description = "Your latest scans and findings",
                icon = "📋",
                category = "Activity",
                isPremium = false
            ),
            WidgetTemplate(
                type = WidgetType.QUICK_SCAN,
                defaultTitle = "Quick Scan",
                defaultSize = WidgetSize.MEDIUM,
                description = "Quickly scan URLs, APKs, or phone numbers",
                icon = "🔍",
                category = "Actions",
                isPremium = false
            ),
            WidgetTemplate(
                type = WidgetType.SECURITY_TIP,
                defaultTitle = "Security Tip",
                defaultSize = WidgetSize.SMALL,
                description = "Daily security tips and best practices",
                icon = "💡",
                category = "Education",
                isPremium = false
            ),
            WidgetTemplate(
                type = WidgetType.PREDICTIONS,
                defaultTitle = "Threat Predictions",
                defaultSize = WidgetSize.MEDIUM,
                description = "AI-powered threat predictions",
                icon = "🔮",
                category = "Analytics",
                isPremium = true
            ),
            WidgetTemplate(
                type = WidgetType.WEEKLY_FORECAST,
                defaultTitle = "Weekly Forecast",
                defaultSize = WidgetSize.LARGE,
                description = "7-day threat forecast",
                icon = "📅",
                category = "Analytics",
                isPremium = true
            ),
            WidgetTemplate(
                type = WidgetType.RECOMMENDATIONS,
                defaultTitle = "Recommendations",
                defaultSize = WidgetSize.MEDIUM,
                description = "Personalized security recommendations",
                icon = "✅",
                category = "Actions",
                isPremium = false
            )
        )
    }
    
    /**
     * Get max row to append new widgets
     */
    suspend fun getMaxRow(): Int? = widgetDao.getMaxRow()

    /**
     * Delete all widgets
     */
    suspend fun deleteAllWidgets() = widgetDao.deleteAllWidgets()

    /**
     * Initialize default dashboard layout
     */
    suspend fun initializeDefaultLayout() {
        widgetDao.deleteAllWidgets()
        
        val defaultWidgets = listOf(
            WidgetConfig(
                widgetType = WidgetType.SECURITY_SCORE,
                title = "Security Score",
                size = WidgetSize.SMALL,
                position = WidgetPosition(0, 0, 1, 1),
                isVisible = true,
                refreshInterval = 5
            ),
            WidgetConfig(
                widgetType = WidgetType.QUICK_SCAN,
                title = "Quick Scan",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(0, 1, 2, 1),
                isVisible = true,
                refreshInterval = 0 // No refresh needed
            ),
            WidgetConfig(
                widgetType = WidgetType.RISK_GAUGE,
                title = "Current Risk",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(1, 0, 2, 1),
                isVisible = true,
                refreshInterval = 5
            ),
            WidgetConfig(
                widgetType = WidgetType.THREAT_TRENDS,
                title = "Threat Trends",
                size = WidgetSize.LARGE,
                position = WidgetPosition(1, 1, 2, 2),
                isVisible = true,
                refreshInterval = 15
            ),
            WidgetConfig(
                widgetType = WidgetType.RECENT_ACTIVITY,
                title = "Recent Activity",
                size = WidgetSize.MEDIUM,
                position = WidgetPosition(3, 0, 2, 1),
                isVisible = true,
                refreshInterval = 10
            ),
            WidgetConfig(
                widgetType = WidgetType.SECURITY_TIP,
                title = "Security Tip",
                size = WidgetSize.SMALL,
                position = WidgetPosition(3, 1, 1, 1),
                isVisible = true,
                refreshInterval = 60
            )
        )
        
        defaultWidgets.forEach { widget ->
            widgetDao.insertWidget(widget)
        }
    }
}
