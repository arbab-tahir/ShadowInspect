package com.shadowinspect.app.data.db

import androidx.room.*
import com.shadowinspect.app.domain.education.UserProgress

@Dao
interface UserProgressDao {

    @Query("SELECT * FROM user_progress ORDER BY id DESC LIMIT 1")
    suspend fun getProgress(): UserProgress?

    @Query("SELECT * FROM user_progress ORDER BY id DESC LIMIT 1")
    fun getProgressFlow(): kotlinx.coroutines.flow.Flow<UserProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: UserProgress)
}
