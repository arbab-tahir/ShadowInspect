package com.shadowinspect.app.domain.widgets

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Available widget types
 */
enum class WidgetType {
    RISK_GAUGE,
    SECURITY_SCORE,
    THREAT_TRENDS,
    TECHNIQUE_PIE,
    TACTIC_BARS,
    RECENT_ACTIVITY,
    QUICK_SCAN,
    SECURITY_TIP,
    PREDICTIONS,
    WEEKLY_FORECAST,
    TOP_THREATS,
    SYSTEM_HEALTH,
    PRIVACY_SCORE,
    RECOMMENDATIONS
}

/**
 * Widget size options
 */
enum class WidgetSize {
    SMALL,      // 1x1 grid
    MEDIUM,     // 2x1 or 1x2
    LARGE,      // 2x2
    WIDE,       // 3x1
    TALL        // 1x3
}

/**
 * User's widget configuration
 */
@Entity(tableName = "widget_configs")
data class WidgetConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val widgetType: WidgetType,
    val title: String,
    val size: WidgetSize,
    @Embedded val position: WidgetPosition,
    val isVisible: Boolean = true,
    val refreshInterval: Int = 5, // minutes
    val customSettings: String? = null, // JSON for widget-specific settings
    val colorTheme: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Widget position in grid
 */
data class WidgetPosition(
    val row: Int,
    val col: Int,
    val rowSpan: Int = 1,
    val colSpan: Int = 1
) {
    fun toMap(): Map<String, Int> = mapOf(
        "row" to row,
        "col" to col,
        "rowSpan" to rowSpan,
        "colSpan" to colSpan
    )
    
    companion object {
        fun fromMap(map: Map<String, Int>): WidgetPosition {
            return WidgetPosition(
                row = map["row"] ?: 0,
                col = map["col"] ?: 0,
                rowSpan = map["rowSpan"] ?: 1,
                colSpan = map["colSpan"] ?: 1
            )
        }
    }
}

/**
 * Widget template for adding new widgets
 */
data class WidgetTemplate(
    val type: WidgetType,
    val defaultTitle: String,
    val defaultSize: WidgetSize,
    val description: String,
    val icon: String,
    val category: String,
    val isPremium: Boolean = false
)

/**
 * Dashboard layout preset
 */
data class DashboardPreset(
    val name: String,
    val description: String,
    val widgets: List<WidgetConfig>,
    val thumbnail: String? = null
)

/**
 * Widget statistics for analytics
 */
data class WidgetStats(
    val widgetType: WidgetType,
    val views: Int,
    val interactions: Int,
    val avgTimeSpent: Long,
    val lastUsed: Long
)
