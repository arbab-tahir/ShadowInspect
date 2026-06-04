package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.widgets.WidgetConfig
import com.shadowinspect.app.domain.widgets.WidgetType
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetConfigDao {
    
    @Query("SELECT * FROM widget_configs WHERE isVisible = 1 ORDER BY row ASC, col ASC")
    fun getAllWidgets(): Flow<List<WidgetConfig>>
    
    @Query("SELECT * FROM widget_configs WHERE widgetType = :type")
    suspend fun getWidgetsByType(type: WidgetType): List<WidgetConfig>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetConfig): Long
    
    @Update
    suspend fun updateWidget(widget: WidgetConfig)
    
    @Delete
    suspend fun deleteWidget(widget: WidgetConfig)
    
    @Query("DELETE FROM widget_configs")
    suspend fun deleteAllWidgets()
    
    @Query("UPDATE widget_configs SET isVisible = :isVisible WHERE id = :widgetId")
    suspend fun setWidgetVisibility(widgetId: Long, isVisible: Boolean)
    
    @Query("UPDATE widget_configs SET row = :row, col = :col WHERE id = :widgetId")
    suspend fun updateWidgetPosition(widgetId: Long, row: Int, col: Int)
    
    @Query("SELECT COUNT(*) FROM widget_configs WHERE isVisible = 1")
    suspend fun getVisibleWidgetCount(): Int
    
    @Query("SELECT MAX(row) FROM widget_configs")
    suspend fun getMaxRow(): Int?
}
