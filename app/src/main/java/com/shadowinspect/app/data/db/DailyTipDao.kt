package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.education.DailyTip

@Dao
interface DailyTipDao {

    @Query("SELECT * FROM daily_tips WHERE dateAdded >= :dateStart ORDER BY id DESC LIMIT 1")
    suspend fun getTipForDate(dateStart: Long): DailyTip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: DailyTip)

    @Query("SELECT * FROM daily_tips ORDER BY dateAdded DESC LIMIT 1")
    suspend fun getLatestTip(): DailyTip?
}
